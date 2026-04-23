package com.mcp.servers.config;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class McpTransportConfig {

    @Bean
    public HttpServletSseServerTransportProvider mcpSseTransport(JsonMapper mcpServerJsonMapper) {
        return HttpServletSseServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(mcpServerJsonMapper))
                .sseEndpoint("/sse")
                .messageEndpoint("/mcp/message")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpSseServlet(
            HttpServletSseServerTransportProvider mcpSseTransport) {
        ServletRegistrationBean<HttpServletSseServerTransportProvider> registration =
                new ServletRegistrationBean<>(mcpSseTransport);
        registration.addUrlMappings("/sse", "/mcp/message");
        registration.setName("mcpSseServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
