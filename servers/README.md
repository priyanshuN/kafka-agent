# Kafka MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that exposes Apache Kafka consumer group monitoring and remediation capabilities as tools. Built with Spring Boot 4 and Spring AI.

## Overview

This server connects to a local (or remote) Kafka cluster and provides an LLM agent with structured access to consumer group lag metrics and remediation actions. It is designed to be used alongside the `kafka-lag-agent` which drives an autonomous monitoring loop.

## Tools

### Monitoring (Read-Only)

| Tool | Description |
|------|-------------|
| `listConsumerGroups` | Lists all consumer groups registered in the cluster with their state and member count |
| `describeGroup` | Returns detailed state of a specific consumer group |
| `getPartitionLag` | Returns per-partition lag for all partitions of a consumer group |
| `getLagSummary` | Returns lag summary for all consumer groups sorted by total lag |
| `checkLagThreshold` | Checks if a consumer group's lag exceeds a given threshold and returns a severity-classified alert |

### Remediation (Write)

| Tool | Description |
|------|-------------|
| `resetOffsetToLatest` | Resets committed offsets to the log-end offset, skipping all pending messages |
| `resetOffsetToEarliest` | Resets committed offsets to the earliest available offset for full replay |
| `markTopicPaused` | Records a topic as paused in the monitoring system with a reason |
| `markTopicResumed` | Clears the paused flag for a topic |
| `sendAlert` | Logs a structured alert at INFO / WARN / CRITICAL severity |

## Requirements

- Java 25
- Apache Kafka running on `localhost:9092`
- Maven (or use the included `./mvnw` wrapper)

## Configuration

All configuration lives in `src/main/resources/application.yml`.

| Property | Default | Description |
|----------|---------|-------------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `server.port` | `8080` | HTTP port for the MCP SSE endpoint |
| `spring.datasource.url` | `jdbc:h2:file:./data/mcp-server` | H2 database for paused topic state |

## Running

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw package -DskipTests
java -jar target/servers-0.0.1-SNAPSHOT.jar
```

The MCP SSE endpoint will be available at:

```
http://localhost:8080/sse
```

## MCP Client Configuration

To connect an MCP client to this server, point it at the SSE endpoint:

```json
{
  "mcpServers": {
    "kafka-lag-monitor": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

For Spring AI clients, add this to `application.yml`:

```yaml
spring:
  ai:
    mcp:
      client:
        sse:
          connections:
            kafka-lag-monitor:
              url: http://localhost:8080/sse
```

## Development

### Debug Endpoint

While the server is running, list all registered tools:

```
GET http://localhost:8080/debug/tools
```

### H2 Console

Inspect the paused topics database at:

```
http://localhost:8080/h2-console
```

JDBC URL: `jdbc:h2:file:./data/mcp-server`

### Health Check

```
GET http://localhost:8080/actuator/health
```

## Project Structure

```
src/main/java/com/mcp/servers/
├── config/
│   ├── KafkaAdminConfig.java       # Raw Kafka Admin client bean
│   ├── McpToolsConfig.java         # Registers tools with MCP server
│   ├── SecurityConfig.java         # Permits all requests (local dev)
│   └── ToolsDebugController.java   # Debug endpoint for tool listing
├── model/
│   ├── ConsumerGroupInfo.java
│   ├── LagAlert.java
│   ├── LagSummary.java
│   ├── PartitionLagInfo.java
│   └── RemediationResult.java
├── repository/
│   ├── PausedTopicEntity.java
│   └── PausedTopicRepository.java
├── service/
│   └── KafkaAdminService.java      # Kafka Admin API wrappers
└── tools/
    ├── KafkaConsumerGroupTools.java # Read-only MCP tools
    └── KafkaRemediationTools.java  # Remediation MCP tools
```

## Related

- [kafka-lag-agent](../agent/) — The autonomous agent that connects to this server and drives the monitoring loop
- [Model Context Protocol](https://modelcontextprotocol.io) — MCP specification
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) — Spring AI MCP server documentation
