package com.mcp.servers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsumerGroupInfo(
        @JsonProperty("groupId") String groupId,
        @JsonProperty("state") String state,
        @JsonProperty("memberCount") int memberCount,
        @JsonProperty("partitionAssignor") String partitionAssignor
) {}
