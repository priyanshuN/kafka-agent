package com.mcp.agent.agent.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ReportPhase {

    private static final Logger log = LoggerFactory.getLogger(ReportPhase.class);

    public void execute(String remediatePayload) {
        log.info("[REPORT] ========== Agent Run Complete ==========");
        log.info("[REPORT] Timestamp: {}", Instant.now());
        log.info("[REPORT] Actions taken: {}", remediatePayload);
        log.info("[REPORT] ==========================================");
    }
}
