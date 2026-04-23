package com.mcp.servers.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartitionLagInfo(
        @JsonProperty("topic") String topic,
        @JsonProperty("partition") int partition,
        @JsonProperty("committedOffset") long committedOffset,
        @JsonProperty("endOffset") long endOffset,
        @JsonProperty("lag") long lag
) {}
