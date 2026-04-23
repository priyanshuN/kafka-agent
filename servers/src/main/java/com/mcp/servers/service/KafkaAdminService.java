package com.mcp.servers.service;

import com.mcp.servers.model.ConsumerGroupInfo;
import com.mcp.servers.model.LagSummary;
import com.mcp.servers.model.PartitionLagInfo;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    private final Admin admin;

    public KafkaAdminService(Admin admin) {
        this.admin = admin;
    }

    public List<ConsumerGroupInfo> listConsumerGroups() {
        try {
            Collection<GroupListing> listings = admin.listGroups().all().get();
            List<String> groupIds = listings.stream()
                    .map(GroupListing::groupId)
                    .toList();

            if (groupIds.isEmpty()) return List.of();

            Map<String, ConsumerGroupDescription> descriptions =
                    admin.describeConsumerGroups(groupIds).all().get();

            return descriptions.values().stream()
                    .map(d -> new ConsumerGroupInfo(
                            d.groupId(),
                            d.groupState().toString(),
                            d.members().size(),
                            d.partitionAssignor()
                    ))
                    .toList();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to list consumer groups", e);
            throw new RuntimeException("Failed to list consumer groups: " + e.getMessage(), e);
        }
    }

    public ConsumerGroupInfo describeGroup(String groupId) {
        try {
            Map<String, ConsumerGroupDescription> result =
                    admin.describeConsumerGroups(List.of(groupId)).all().get();
            ConsumerGroupDescription d = result.get(groupId);
            if (d == null) throw new RuntimeException("Group not found: " + groupId);
            return new ConsumerGroupInfo(
                    d.groupId(),
                    d.groupState().toString(),
                    d.members().size(),
                    d.partitionAssignor()
            );
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to describe group {}", groupId, e);
            throw new RuntimeException("Failed to describe group: " + e.getMessage(), e);
        }
    }

    public LagSummary getPartitionLag(String groupId) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committedOffsets =
                    admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();

            if (committedOffsets.isEmpty()) {
                return new LagSummary(groupId, 0L, 0L, 0, List.of());
            }

            Map<TopicPartition, OffsetSpec> latestSpecs = committedOffsets.keySet().stream()
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    admin.listOffsets(latestSpecs).all().get();

            List<PartitionLagInfo> partitions = new ArrayList<>();
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committedOffsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                long committed = entry.getValue().offset();
                long end = endOffsets.getOrDefault(tp,
                        new ListOffsetsResult.ListOffsetsResultInfo(committed, -1L, Optional.empty()))
                        .offset();
                long lag = Math.max(0, end - committed);
                partitions.add(new PartitionLagInfo(tp.topic(), tp.partition(), committed, end, lag));
            }

            long totalLag = partitions.stream().mapToLong(PartitionLagInfo::lag).sum();
            long maxLag = partitions.stream().mapToLong(PartitionLagInfo::lag).max().orElse(0L);

            return new LagSummary(groupId, totalLag, maxLag, partitions.size(), partitions);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to get partition lag for group {}", groupId, e);
            throw new RuntimeException("Failed to get partition lag: " + e.getMessage(), e);
        }
    }

    public List<LagSummary> getLagSummaryForAll() {
        List<ConsumerGroupInfo> groups = listConsumerGroups();
        return groups.stream()
                .map(g -> {
                    try {
                        return getPartitionLag(g.groupId());
                    } catch (Exception e) {
                        log.warn("Could not get lag for group {}: {}", g.groupId(), e.getMessage());
                        return new LagSummary(g.groupId(), -1L, -1L, 0, List.of());
                    }
                })
                .sorted(Comparator.comparingLong(LagSummary::totalLag).reversed())
                .toList();
    }

    public void resetOffsetToLatest(String groupId, String topic) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committed =
                    admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();

            Map<TopicPartition, OffsetSpec> latestSpecs = committed.keySet().stream()
                    .filter(tp -> tp.topic().equals(topic))
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

            if (latestSpecs.isEmpty()) {
                throw new RuntimeException("Group " + groupId + " has no committed offsets for topic " + topic);
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                    admin.listOffsets(latestSpecs).all().get();

            Map<TopicPartition, OffsetAndMetadata> newOffsets = endOffsets.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> new OffsetAndMetadata(e.getValue().offset())));

            admin.alterConsumerGroupOffsets(groupId, newOffsets).all().get();
            log.info("Reset offsets to latest for group={} topic={}", groupId, topic);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to reset offset to latest for group={} topic={}", groupId, topic, e);
            throw new RuntimeException("Failed to reset offset: " + e.getMessage(), e);
        }
    }

    public void resetOffsetToEarliest(String groupId, String topic) {
        try {
            Map<TopicPartition, OffsetAndMetadata> committed =
                    admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();

            Map<TopicPartition, OffsetSpec> earliestSpecs = committed.keySet().stream()
                    .filter(tp -> tp.topic().equals(topic))
                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.earliest()));

            if (earliestSpecs.isEmpty()) {
                throw new RuntimeException("Group " + groupId + " has no committed offsets for topic " + topic);
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> startOffsets =
                    admin.listOffsets(earliestSpecs).all().get();

            Map<TopicPartition, OffsetAndMetadata> newOffsets = startOffsets.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> new OffsetAndMetadata(e.getValue().offset())));

            admin.alterConsumerGroupOffsets(groupId, newOffsets).all().get();
            log.info("Reset offsets to earliest for group={} topic={}", groupId, topic);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to reset offset to earliest for group={} topic={}", groupId, topic, e);
            throw new RuntimeException("Failed to reset offset: " + e.getMessage(), e);
        }
    }
}
