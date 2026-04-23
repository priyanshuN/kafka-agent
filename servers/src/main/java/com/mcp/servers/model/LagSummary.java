package com.mcp.servers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LagSummary(
        @JsonProperty("groupId") String groupId,
        @JsonProperty("totalLag") long totalLag,
        @JsonProperty("maxPartitionLag") long maxPartitionLag,
        @JsonProperty("partitionCount") int partitionCount,
        @JsonProperty("partitions") List<PartitionLagInfo> partitions
) {}
