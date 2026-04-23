package com.mcp.agent.remediation;

import com.mcp.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

@Component
public class RemediationDecisionTree {

    public boolean isAllowed(String severity, String action, AgentProperties properties) {
        return switch (action.toUpperCase()) {
            case "SEND_ALERT" -> true;
            case "RESET_OFFSET_LATEST" ->
                    "CRITICAL".equals(severity) && properties.getRemediation().isAutoResetEnabled();
            case "MARK_PAUSED" ->
                    "CRITICAL".equals(severity);
            case "RESET_OFFSET_EARLIEST" ->
                    false; // never automatic — too destructive
            default -> false;
        };
    }
}
