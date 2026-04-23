package com.mcp.agent.agent.prompt;

public final class PhasePromptTemplates {

    private PhasePromptTemplates() {}

    public static final String BASE_SYSTEM_PROMPT = """
            You are an autonomous Kafka consumer lag monitoring agent.
            You have access to tools that connect to a live Kafka cluster.
            Always use tools to fetch real data — never assume or fabricate metric values.
            Return responses as valid JSON only, with no markdown, no code fences, no explanation.
            """;

    public static final String COLLECT_USER = """
            Step 1: Call getLagSummary() to get lag metrics for all consumer groups.
            Step 2: Filter out groups with totalLag equal to 0.
            Step 3: Return a JSON object in exactly this format:
            {
              "groups": [
                {
                  "groupId": "<group-id>",
                  "totalLag": <number>,
                  "maxPartitionLag": <number>,
                  "partitionCount": <number>
                }
              ],
              "collectedAt": "<ISO timestamp>"
            }
            If all groups have zero lag, return { "groups": [], "collectedAt": "<ISO timestamp>" }
            """;

    public static String evaluateUser(String collectPayload, long warnThreshold, long criticalThreshold) {
        return """
                Here are the current Kafka consumer lag metrics:
                %s

                Thresholds: WARN = %d, CRITICAL = %d

                For each group in the metrics:
                - If totalLag is 0: skip it
                - If totalLag is between WARN and CRITICAL: severity = WARN, action = SEND_ALERT
                - If totalLag is >= CRITICAL: severity = CRITICAL, action = SEND_ALERT
                  (only use RESET_OFFSET_LATEST if the group has no active members and you confirm this with describeGroup)

                Return a JSON object in exactly this format:
                {
                  "alerts": [
                    {
                      "groupId": "<group-id>",
                      "severity": "WARN|CRITICAL",
                      "totalLag": <number>,
                      "recommendedAction": "SEND_ALERT|RESET_OFFSET_LATEST|MARK_PAUSED",
                      "reasoning": "<one sentence>"
                    }
                  ]
                }
                If there are no alerts, return { "alerts": [] }
                """.formatted(collectPayload, warnThreshold, criticalThreshold);
    }

    public static String remediateUser(String alert) {
        return """
                Execute the following remediation for this Kafka consumer group alert:
                %s

                Use the appropriate tool to carry out the recommendedAction.
                For SEND_ALERT: call sendAlert(groupId, severity, message) with a descriptive message.
                For RESET_OFFSET_LATEST: call resetOffsetToLatest(groupId, topic) for each affected topic.
                For MARK_PAUSED: call markTopicPaused(topic, reason) for each affected topic.

                Return a JSON object:
                {
                  "groupId": "<group-id>",
                  "actionTaken": "<action>",
                  "success": true|false,
                  "detail": "<result message>"
                }
                """.formatted(alert);
    }
}
