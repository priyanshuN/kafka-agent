package com.mcp.servers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RemediationResult(
        @JsonProperty("action") String action,
        @JsonProperty("groupId") String groupId,
        @JsonProperty("topic") String topic,
        @JsonProperty("success") boolean success,
        @JsonProperty("message") String message
) {}
