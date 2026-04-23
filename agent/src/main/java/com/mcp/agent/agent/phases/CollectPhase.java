package com.mcp.agent.agent.phases;

import com.mcp.agent.agent.McpToolInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CollectPhase {

    private static final Logger log = LoggerFactory.getLogger(CollectPhase.class);

    private final McpToolInvoker toolInvoker;

    public CollectPhase(McpToolInvoker toolInvoker) {
        this.toolInvoker = toolInvoker;
    }

    public String execute() {
        log.info("[COLLECT] Fetching lag metrics directly from MCP server");
        String result = toolInvoker.invoke("getLagSummary");
        log.debug("[COLLECT] Raw result: {}", result);
        return result;
    }
}
