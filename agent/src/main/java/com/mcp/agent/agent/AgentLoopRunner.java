package com.mcp.agent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentLoopRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopRunner.class);

    private final AgentOrchestrator orchestrator;

    public AgentLoopRunner(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${agent.poll-interval-ms:60000}")
    public void runLoop() {
        log.info("Agent loop triggered");
        orchestrator.run();
    }
}
