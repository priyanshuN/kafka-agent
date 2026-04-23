package com.mcp.servers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LagAlert(
        @JsonProperty("groupId") String groupId,
        @JsonProperty("severity") String severity,
        @JsonProperty("threshold") long threshold,
        @JsonProperty("actualLag") long actualLag,
        @JsonProperty("exceeded") boolean exceeded
) {}
