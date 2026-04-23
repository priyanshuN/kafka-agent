package com.mcp.agent.agent;

import com.mcp.agent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AgenticAgent {

    private static final Logger log = LoggerFactory.getLogger(AgenticAgent.class);

    private final ChatClient chatClient;
    private final AgentProperties properties;

    public AgenticAgent(ChatClient chatClient, AgentProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    public String run() {
        log.info("[AGENTIC] Starting agentic run — LLM has full tool access");

        String prompt = """
                You are a Kafka consumer lag monitoring agent. Investigate and remediate as follows:

                1. Call getLagSummary() to get lag metrics for all consumer groups.
                2. For each group where totalLag > 0, classify severity:
                   - totalLag >= %d (CRITICAL threshold) → CRITICAL
                   - totalLag >= %d (WARN threshold) → WARN
                   - totalLag = 0 → skip, do nothing
                3. For CRITICAL groups: call describeGroup(groupId) to check member count and state.
                4. Take action based on findings:
                   - WARN → call sendAlert(groupId, severity, message)
                   - CRITICAL with active members → call sendAlert(groupId, severity, message)
                   - CRITICAL with no active members → call resetOffsetToLatest(groupId, topic)
                   - CRITICAL with group state DEAD → call markTopicPaused(topic, reason)
                5. Return a plain text summary of what you found and what actions you took.
                """.formatted(properties.getCriticalThreshold(), properties.getWarnThreshold());

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("[AGENTIC] Run complete. Summary: {}", result);
        return result;
    }
}
