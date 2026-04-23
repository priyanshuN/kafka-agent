package com.mcp.agent.agent.phases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.agent.agent.McpToolInvoker;
import com.mcp.agent.config.AgentProperties;
import com.mcp.agent.remediation.RemediationDecisionTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RemediatePhase {

    private static final Logger log = LoggerFactory.getLogger(RemediatePhase.class);

    private final McpToolInvoker toolInvoker;
    private final RemediationDecisionTree decisionTree;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    public RemediatePhase(McpToolInvoker toolInvoker, RemediationDecisionTree decisionTree,
                          AgentProperties properties, ObjectMapper objectMapper) {
        this.toolInvoker = toolInvoker;
        this.decisionTree = decisionTree;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String execute(String evaluatePayload) {
        log.info("[REMEDIATE] Processing alerts");
        List<String> results = new ArrayList<>();
        int actionsCount = 0;
        int maxActions = properties.getRemediation().getMaxActionsPerRun();

        try {
            JsonNode root = objectMapper.readTree(repairJson(evaluatePayload));
            JsonNode alerts = root.path("alerts");

            if (alerts.isEmpty()) {
                log.info("[REMEDIATE] No alerts to process");
                return "{\"actionsPerformed\": []}";
            }

            for (JsonNode alert : alerts) {
                if (actionsCount >= maxActions) {
                    log.warn("[REMEDIATE] Max actions per run ({}) reached", maxActions);
                    break;
                }

                String groupId = alert.path("groupId").asText();
                String severity = alert.path("severity").asText();
                String action = alert.path("recommendedAction").asText();
                String reasoning = alert.path("reasoning").asText();

                if (!decisionTree.isAllowed(severity, action, properties)) {
                    log.warn("[REMEDIATE] Action {} blocked for group={}, falling back to SEND_ALERT", action, groupId);
                    action = "SEND_ALERT";
                }

                String result = executeAction(groupId, severity, action, reasoning, alert);
                results.add(result);
                actionsCount++;
            }

        } catch (Exception e) {
            log.error("[REMEDIATE] Failed to parse evaluate payload", e);
        }

        return "{\"actionsPerformed\": " + results + "}";
    }

    private String repairJson(String json) {
        if (json == null) return "{\"alerts\": []}";
        String trimmed = json.strip();
        // extract first { ... } block in case LLM added extra text
        int start = trimmed.indexOf('{');
        if (start == -1) return "{\"alerts\": []}";
        trimmed = trimmed.substring(start);
        // count braces to detect truncation and close if needed
        int open = 0;
        for (char c : trimmed.toCharArray()) {
            if (c == '{') open++;
            else if (c == '}') open--;
        }
        return trimmed + "}".repeat(Math.max(0, open));
    }

    private String executeAction(String groupId, String severity, String action,
                                  String reasoning, JsonNode alert) {
        log.info("[REMEDIATE] Executing {} for group={} severity={}", action, groupId, severity);

        return switch (action.toUpperCase()) {
            case "SEND_ALERT" -> toolInvoker.invoke("sendAlert", Map.of(
                    "groupId", groupId,
                    "severity", severity,
                    "message", reasoning
            ));
            case "RESET_OFFSET_LATEST" -> {
                // get affected topics from the alert partitions if present, else use groupId only
                String topic = alert.path("topic").asText(groupId);
                yield toolInvoker.invoke("resetOffsetToLatest", Map.of(
                        "groupId", groupId,
                        "topic", topic
                ));
            }
            case "MARK_PAUSED" -> {
                String topic = alert.path("topic").asText(groupId);
                yield toolInvoker.invoke("markTopicPaused", Map.of(
                        "topic", topic,
                        "reason", reasoning
                ));
            }
            default -> {
                log.warn("[REMEDIATE] Unknown action {}, sending alert instead", action);
                yield toolInvoker.invoke("sendAlert", Map.of(
                        "groupId", groupId,
                        "severity", severity,
                        "message", "Unknown action requested: " + action + ". " + reasoning
                ));
            }
        };
    }
}
