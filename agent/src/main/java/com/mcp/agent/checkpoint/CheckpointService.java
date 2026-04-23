package com.mcp.agent.checkpoint;

import com.mcp.agent.model.AgentPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);

    private final CheckpointRepository repository;
    private String currentRunId;

    public CheckpointService(CheckpointRepository repository) {
        this.repository = repository;
    }

    public void startNewRun() {
        currentRunId = UUID.randomUUID().toString();
        log.info("Starting new agent run: {}", currentRunId);
    }

    @Transactional
    public void save(AgentPhase phase, String payloadJson) {
        repository.deactivateAll();
        repository.save(new CheckpointEntity(currentRunId, phase, payloadJson));
        log.debug("Checkpoint saved: runId={} phase={}", currentRunId, phase);
    }

    @Transactional(readOnly = true)
    public Optional<CheckpointEntity> loadLatest() {
        return repository.findTopByActiveTrueOrderByCreatedAtDesc();
    }

}
