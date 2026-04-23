package com.mcp.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private long pollIntervalMs = 60000;
    private long warnThreshold = 1000;
    private long criticalThreshold = 10000;
    private String mode = "phased";
    private Remediation remediation = new Remediation();

    @Setter
    @Getter
    public static class Remediation {
        private boolean autoResetEnabled = false;
        private int maxActionsPerRun = 3;

    }

}
