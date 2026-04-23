package com.mcp.servers.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class ToolsDebugController {

    private final ToolCallbackProvider toolCallbackProvider;

    public ToolsDebugController(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
    }

    @GetMapping("/tools")
    public List<Map<String, String>> listTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(t -> Map.of(
                        "name", t.getToolDefinition().name(),
                        "description", t.getToolDefinition().description()
                ))
                .toList();
    }
}
