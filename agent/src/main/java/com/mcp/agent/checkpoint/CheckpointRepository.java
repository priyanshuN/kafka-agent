package com.mcp.agent.checkpoint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CheckpointRepository extends JpaRepository<CheckpointEntity, Long> {

    Optional<CheckpointEntity> findTopByActiveTrueOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE CheckpointEntity c SET c.active = false WHERE c.active = true")
    void deactivateAll();
}
