package com.mcp.agent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

@Component
public class McpToolInvoker {

    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;

    public McpToolInvoker(ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.objectMapper = objectMapper;
    }

    public String invoke(String toolName) {
        return invoke(toolName, Map.of());
    }

    public String invoke(String toolName, Map<String, Object> args) {
        try {
            String argsJson = objectMapper.writeValueAsString(args);
            return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                    .filter(t -> t.getToolDefinition().name().equals(toolName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("MCP tool not found: " + toolName))
                    .call(argsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize args for tool: " + toolName, e);
        }
    }
}
