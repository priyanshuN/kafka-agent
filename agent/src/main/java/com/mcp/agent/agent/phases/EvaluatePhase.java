package com.mcp.agent.agent.phases;

import com.mcp.agent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class EvaluatePhase {

    private static final Logger log = LoggerFactory.getLogger(EvaluatePhase.class);

    private final ChatClient chatClient;
    private final AgentProperties properties;

    public EvaluatePhase(ChatClient chatClient, AgentProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    public String execute(String collectPayload) {
        log.info("[EVALUATE] Sending lag data to LLM for classification");

        String prompt = """
                You are a Kafka lag classifier. Analyze the consumer group metrics below and return a JSON object.

                INPUT METRICS:
                %s

                THRESHOLDS: WARN = %d, CRITICAL = %d

                STRICT RULES (follow exactly):
                1. If totalLag = 0 for a group → DO NOT include it in alerts. Zero lag means healthy.
                2. If totalLag > 0 AND totalLag < %d → severity = "WARN", recommendedAction = "SEND_ALERT"
                3. If totalLag >= %d → severity = "CRITICAL", recommendedAction = "SEND_ALERT"
                4. If ALL groups have totalLag = 0 → return exactly: {"alerts": []}

                OUTPUT FORMAT (return ONLY this JSON, no markdown, no explanation, no extra text):
                {"alerts": [{"groupId": "<id>", "severity": "WARN or CRITICAL", "totalLag": <number>, "recommendedAction": "SEND_ALERT", "reasoning": "<one sentence>"}]}
                """.formatted(collectPayload, properties.getWarnThreshold(), properties.getCriticalThreshold(),
                        properties.getCriticalThreshold(), properties.getCriticalThreshold());

        String result = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("[EVALUATE] LLM result: {}", result);
        return result;
    }
}
