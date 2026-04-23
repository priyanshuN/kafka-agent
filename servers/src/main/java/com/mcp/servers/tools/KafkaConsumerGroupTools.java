package com.mcp.servers.tools;

import com.mcp.servers.model.ConsumerGroupInfo;
import com.mcp.servers.model.LagAlert;
import com.mcp.servers.model.LagSummary;
import com.mcp.servers.service.KafkaAdminService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaConsumerGroupTools {

    private final KafkaAdminService adminService;

    public KafkaConsumerGroupTools(KafkaAdminService adminService) {
        this.adminService = adminService;
    }

    @Tool(description = "Returns all Kafka consumer groups registered in the cluster with their state and member count.")
    public List<ConsumerGroupInfo> listConsumerGroups() {
        return adminService.listConsumerGroups();
    }

    @Tool(description = "Returns detailed state of a specific consumer group: state, member count, and partition assignor. Provide the exact consumer group ID.")
    public ConsumerGroupInfo describeGroup(String groupId) {
        return adminService.describeGroup(groupId);
    }

    @Tool(description = "Returns per-partition lag (endOffset minus committedOffset) for all partitions of a consumer group. Non-zero lag means the consumer is behind. Provide the consumer group ID.")
    public LagSummary getPartitionLag(String groupId) {
        return adminService.getPartitionLag(groupId);
    }

    @Tool(description = "Returns lag summary for ALL active consumer groups sorted by total lag descending. Use this as the first call to get a full picture of cluster health.")
    public List<LagSummary> getLagSummary() {
        return adminService.getLagSummaryForAll();
    }

    @Tool(description = "Checks if a consumer group's total lag exceeds the given threshold. Returns a LagAlert with severity INFO/WARN/CRITICAL and whether the threshold was exceeded.")
    public LagAlert checkLagThreshold(String groupId, long threshold) {
        LagSummary summary = adminService.getPartitionLag(groupId);
        boolean exceeded = summary.totalLag() > threshold;
        String severity;
        if (!exceeded) {
            severity = "INFO";
        } else if (summary.totalLag() < threshold * 10) {
            severity = "WARN";
        } else {
            severity = "CRITICAL";
        }
        return new LagAlert(groupId, severity, threshold, summary.totalLag(), exceeded);
    }
}
