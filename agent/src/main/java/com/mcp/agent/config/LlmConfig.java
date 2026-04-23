package com.mcp.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.agent.agent.prompt.PhasePromptTemplates;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider toolCallbackProvider,
                                 AgentProperties properties) {
        ChatClient.Builder b = builder.defaultSystem(PhasePromptTemplates.BASE_SYSTEM_PROMPT);
        if ("agentic".equalsIgnoreCase(properties.getMode())) {
            b.defaultToolCallbacks(toolCallbackProvider.getToolCallbacks());
        }
        return b.build();
    }
}
