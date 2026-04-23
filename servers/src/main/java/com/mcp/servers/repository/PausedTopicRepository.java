package com.mcp.servers.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PausedTopicRepository extends JpaRepository<PausedTopicEntity, Long> {
    Optional<PausedTopicEntity> findByTopic(String topic);
    boolean existsByTopic(String topic);
}
