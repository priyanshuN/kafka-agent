package com.mcp.agent.agent;

import com.mcp.agent.agent.phases.*;
import com.mcp.agent.checkpoint.CheckpointEntity;
import com.mcp.agent.checkpoint.CheckpointService;
import com.mcp.agent.config.AgentProperties;
import com.mcp.agent.model.AgentPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final CheckpointService checkpointService;
    private final CollectPhase collectPhase;
    private final EvaluatePhase evaluatePhase;
    private final RemediatePhase remediatePhase;
    private final ReportPhase reportPhase;
    private final AgenticAgent agenticAgent;
    private final AgentProperties properties;

    public AgentOrchestrator(CheckpointService checkpointService,
                             CollectPhase collectPhase,
                             EvaluatePhase evaluatePhase,
                             RemediatePhase remediatePhase,
                             ReportPhase reportPhase,
                             AgenticAgent agenticAgent,
                             AgentProperties properties) {
        this.checkpointService = checkpointService;
        this.collectPhase = collectPhase;
        this.evaluatePhase = evaluatePhase;
        this.remediatePhase = remediatePhase;
        this.reportPhase = reportPhase;
        this.agenticAgent = agenticAgent;
        this.properties = properties;
    }

    public void run() {
        if ("agentic".equalsIgnoreCase(properties.getMode())) {
            runAgentic();
        } else {
            runPhased();
        }
    }

    private void runAgentic() {
        log.info("[ORCHESTRATOR] Mode = AGENTIC");
        checkpointService.startNewRun();
        try {
            String summary = agenticAgent.run();
            checkpointService.save(AgentPhase.DONE, summary);
        } catch (Exception e) {
            log.error("[ORCHESTRATOR] Agentic run failed", e);
        }
    }

    private void runPhased() {
        log.info("[ORCHESTRATOR] Mode = PHASED");
        Optional<CheckpointEntity> latest = checkpointService.loadLatest();

        AgentPhase resumeFrom = latest
                .filter(c -> c.getPhase() != AgentPhase.DONE)
                .map(CheckpointEntity::getPhase)
                .orElse(AgentPhase.COLLECT);

        String resumePayload = latest
                .filter(c -> c.getPhase() != AgentPhase.DONE)
                .map(CheckpointEntity::getPayloadJson)
                .orElse(null);

        if (resumeFrom != AgentPhase.COLLECT) {
            log.info("[ORCHESTRATOR] Resuming from checkpoint phase={}", resumeFrom);
        }

        checkpointService.startNewRun();

        try {
            String collectPayload;
            if (resumeFrom == AgentPhase.COLLECT || resumePayload == null) {
                collectPayload = collectPhase.execute();
                checkpointService.save(AgentPhase.EVALUATE, collectPayload);
            } else if (resumeFrom == AgentPhase.EVALUATE) {
                collectPayload = resumePayload;
            } else {
                collectPayload = resumePayload;
            }

            String evaluatePayload;
            if (resumeFrom == AgentPhase.COLLECT || resumeFrom == AgentPhase.EVALUATE) {
                evaluatePayload = evaluatePhase.execute(collectPayload);
                checkpointService.save(AgentPhase.REMEDIATE, evaluatePayload);
            } else {
                evaluatePayload = resumePayload;
            }

            String remediatePayload = remediatePhase.execute(evaluatePayload);
            checkpointService.save(AgentPhase.REPORT, remediatePayload);

            reportPhase.execute(remediatePayload);
            checkpointService.save(AgentPhase.DONE, "");

        } catch (Exception e) {
            log.error("[ORCHESTRATOR] Phased run failed — checkpoint saved, will resume next cycle", e);
        }
    }
}
