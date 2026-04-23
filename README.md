# Kafka Consumer Lag Monitoring Agent

An autonomous Kafka consumer lag monitoring and auto-remediation system built with **Spring Boot 4**, **Spring AI 2.0**, and **Model Context Protocol (MCP)**.

## Architecture

```
Agent Server (:8081)
  │
  │  [loop every 60s]
  │  LLM autonomously calls tools → investigates → remediates
  │
  │  ChatClient (Spring AI) + qwen2.5:14b (Ollama)
  │
  └── MCP Client ──SSE──▶ MCP Server (:8080)
                                │
                                ├── KafkaConsumerGroupTools (read)
                                │     getLagSummary()
                                │     listConsumerGroups()
                                │     describeGroup(groupId)
                                │     getPartitionLag(groupId)
                                │     checkLagThreshold(groupId, threshold)
                                │
                                └── KafkaRemediationTools (write)
                                      sendAlert(groupId, severity, message)
                                      resetOffsetToLatest(groupId, topic)
                                      resetOffsetToEarliest(groupId, topic)
                                      markTopicPaused(topic, reason)
                                      markTopicResumed(topic)
                                │
                                └── Kafka (localhost:9092)
```

## Two Agent Modes

### Agentic Mode (`agent.mode: agentic`)
The LLM has full access to all 10 MCP tools and drives the entire investigation autonomously. It decides which tools to call, in what order, and what actions to take — using Spring AI's built-in tool calling loop.

```
LLM → calls getLagSummary()
LLM → sees high lag → calls describeGroup() for more context
LLM → decides action → calls sendAlert() or resetOffsetToLatest()
LLM → returns summary
```

### Phased Mode (`agent.mode: phased`)
Hardcoded four-phase pipeline with checkpoint recovery:
1. **COLLECT** — fetches lag metrics directly via MCP tools
2. **EVALUATE** — sends data to LLM for classification (WARN/CRITICAL)
3. **REMEDIATE** — executes actions based on LLM output
4. **REPORT** — logs structured summary

Checkpoints are saved to H2 after each phase — if the agent crashes mid-run, it resumes from the last saved phase on restart.

## Prerequisites

- Java 25 (via IntelliJ SDK manager)
- Kafka running on `localhost:9092`
- [Ollama](https://ollama.com) running on `localhost:11434`
- Model pulled: `ollama pull qwen2.5:14b`

## Getting Started

### 1. Start MCP Server
```bash
cd servers/
./mvnw spring-boot:run
# Runs on http://localhost:8080
# SSE endpoint: http://localhost:8080/sse
# Debug tools: http://localhost:8080/debug/tools
```

### 2. Start Agent Server
```bash
cd agent/
./mvnw spring-boot:run
# Runs on http://localhost:8081
```

### 3. Create artificial lag for testing
```bash
# Create topic
kafka-topics --create --topic test-lag-topic --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

# Produce messages
for i in {1..5000}; do echo "message-$i"; done | kafka-console-producer --topic test-lag-topic --bootstrap-server localhost:9092

# Register consumer group (Ctrl+C immediately after)
kafka-console-consumer --topic test-lag-topic --group test-lag-group --bootstrap-server localhost:9092

# Reset offsets to create lag
kafka-consumer-groups --bootstrap-server localhost:9092 --group test-lag-group --reset-offsets --topic test-lag-topic --to-earliest --execute
```

Watch agent logs — next cycle detects lag and fires alerts automatically.

## Configuration

### Agent Server (`agent/src/main/resources/application.yml`)

| Property | Default | Description |
|---|---|---|
| `agent.mode` | `agentic` | `agentic` or `phased` |
| `agent.poll-interval-ms` | `60000` | How often agent runs (ms) |
| `agent.warn-threshold` | `1000` | Total lag to trigger WARN |
| `agent.critical-threshold` | `10000` | Total lag to trigger CRITICAL |
| `agent.remediation.auto-reset-enabled` | `false` | Allow automatic offset reset |
| `agent.remediation.max-actions-per-run` | `3` | Max remediation actions per cycle |

### MCP Server (`servers/src/main/resources/application.yml`)

| Property | Default | Description |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `server.port` | `8080` | MCP server port |

## Tech Stack

- **Java 25** + **Spring Boot 4.0.5**
- **Spring AI 2.0.0-M4** — MCP server/client, ChatClient, tool calling
- **Model Context Protocol (MCP)** over SSE transport
- **Ollama** — local LLM inference (`qwen2.5:14b`)
- **Apache Kafka 4.x** — consumer group monitoring via Admin API
- **H2** — embedded database for checkpoints and paused topic state

## License

MIT
