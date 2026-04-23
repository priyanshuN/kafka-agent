package com.mcp.agent.checkpoint;

import com.mcp.agent.model.AgentPhase;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Getter
@Entity
@Table(name = "agent_checkpoint")
public class CheckpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentPhase phase;

    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean active;

    public CheckpointEntity() {}

    public CheckpointEntity(String runId, AgentPhase phase, String payloadJson) {
        this.runId = runId;
        this.phase = phase;
        this.payloadJson = payloadJson;
        this.createdAt = Instant.now();
        this.active = true;
    }

}
