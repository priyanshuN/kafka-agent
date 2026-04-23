package com.mcp.servers.tools;

import com.mcp.servers.model.RemediationResult;
import com.mcp.servers.repository.PausedTopicEntity;
import com.mcp.servers.repository.PausedTopicRepository;
import com.mcp.servers.service.KafkaAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class KafkaRemediationTools {

    private static final Logger log = LoggerFactory.getLogger(KafkaRemediationTools.class);

    private final KafkaAdminService adminService;
    private final PausedTopicRepository pausedTopicRepository;

    public KafkaRemediationTools(KafkaAdminService adminService, PausedTopicRepository pausedTopicRepository) {
        this.adminService = adminService;
        this.pausedTopicRepository = pausedTopicRepository;
    }

    @Tool(description = "Resets committed offsets for a consumer group to the latest (log end) offset on all partitions of a topic. WARNING: all unprocessed messages are skipped. Only use for CRITICAL lag when the group has no active members.")
    public RemediationResult resetOffsetToLatest(String groupId, String topic) {
        try {
            adminService.resetOffsetToLatest(groupId, topic);
            log.warn("REMEDIATION: Reset offsets to LATEST for group={} topic={}", groupId, topic);
            return new RemediationResult("RESET_OFFSET_LATEST", groupId, topic, true,
                    "Offsets reset to latest successfully. All unprocessed messages skipped.");
        } catch (Exception e) {
            return new RemediationResult("RESET_OFFSET_LATEST", groupId, topic, false,
                    "Failed: " + e.getMessage());
        }
    }

    @Tool(description = "Resets committed offsets for a consumer group to the earliest available offset on all partitions of a topic. Use to replay all messages from the beginning.")
    public RemediationResult resetOffsetToEarliest(String groupId, String topic) {
        try {
            adminService.resetOffsetToEarliest(groupId, topic);
            log.warn("REMEDIATION: Reset offsets to EARLIEST for group={} topic={}", groupId, topic);
            return new RemediationResult("RESET_OFFSET_EARLIEST", groupId, topic, true,
                    "Offsets reset to earliest successfully. All messages will be replayed.");
        } catch (Exception e) {
            return new RemediationResult("RESET_OFFSET_EARLIEST", groupId, topic, false,
                    "Failed: " + e.getMessage());
        }
    }

    @Tool(description = "Records a topic as paused in the monitoring system with a reason. Does not stop active consumers — it is a monitoring-level flag for tracking intent to pause.")
    public RemediationResult markTopicPaused(String topic, String reason) {
        try {
            if (pausedTopicRepository.existsByTopic(topic)) {
                return new RemediationResult("MARK_PAUSED", null, topic, true,
                        "Topic already marked as paused.");
            }
            pausedTopicRepository.save(new PausedTopicEntity(topic, reason));
            log.warn("REMEDIATION: Marked topic as paused topic={} reason={}", topic, reason);
            return new RemediationResult("MARK_PAUSED", null, topic, true,
                    "Topic marked as paused. Reason: " + reason);
        } catch (Exception e) {
            return new RemediationResult("MARK_PAUSED", null, topic, false,
                    "Failed: " + e.getMessage());
        }
    }

    @Tool(description = "Clears the paused mark for a topic, allowing normal monitoring to resume.")
    public RemediationResult markTopicResumed(String topic) {
        try {
            pausedTopicRepository.findByTopic(topic)
                    .ifPresent(pausedTopicRepository::delete);
            log.info("REMEDIATION: Marked topic as resumed topic={}", topic);
            return new RemediationResult("MARK_RESUMED", null, topic, true,
                    "Topic paused flag cleared.");
        } catch (Exception e) {
            return new RemediationResult("MARK_RESUMED", null, topic, false,
                    "Failed: " + e.getMessage());
        }
    }

    @Tool(description = "Logs a structured alert for a consumer group. Use when lag exceeds threshold but no automated action should be taken. Severity should be INFO, WARN, or CRITICAL.")
    public RemediationResult sendAlert(String groupId, String severity, String message) {
        String logMessage = "[KAFKA-ALERT] severity={} groupId={} message={}";
        switch (severity.toUpperCase()) {
            case "CRITICAL" -> log.error(logMessage, severity, groupId, message);
            case "WARN"     -> log.warn(logMessage, severity, groupId, message);
            default         -> log.info(logMessage, severity, groupId, message);
        }
        return new RemediationResult("SEND_ALERT", groupId, null, true,
                "Alert logged: [" + severity + "] " + message);
    }
}
