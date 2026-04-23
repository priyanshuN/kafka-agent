package com.mcp.servers.config;

import com.mcp.servers.tools.KafkaConsumerGroupTools;
import com.mcp.servers.tools.KafkaRemediationTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider kafkaToolCallbackProvider(
            KafkaConsumerGroupTools consumerGroupTools,
            KafkaRemediationTools remediationTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(consumerGroupTools, remediationTools)
                .build();
    }
}
