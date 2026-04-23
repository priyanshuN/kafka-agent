package com.mcp.servers.repository;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Getter
@Entity
@Table(name = "paused_topics")
public class PausedTopicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String topic;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant pausedAt;

    public PausedTopicEntity() {}

    public PausedTopicEntity(String topic, String reason) {
        this.topic = topic;
        this.reason = reason;
        this.pausedAt = Instant.now();
    }
}
