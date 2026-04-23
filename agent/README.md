# Kafka Lag Agent

The autonomous agent server that connects to the [MCP Server](../servers/README.md) and monitors Kafka consumer group lag every 60 seconds. Uses an LLM (Groq by default, Ollama as alternative) to investigate and remediate lag automatically.

## How It Works

The agent runs on a fixed schedule and operates in one of two modes:

### Agentic Mode (default)
The LLM receives all 10 MCP tools and drives the entire flow itself via Spring AI's tool calling loop:

```
LLM → getLagSummary()
LLM → describeGroup(groupId)     ← if lag is critical
LLM → sendAlert() / resetOffsetToLatest() / markTopicPaused()
LLM → returns plain text summary
```

No hardcoded sequence — the LLM decides which tools to call and in what order based on what it finds.

### Phased Mode
Hardcoded four-phase pipeline:

```
COLLECT → EVALUATE → REMEDIATE → REPORT
```

Each phase saves a checkpoint to H2. If the agent crashes mid-run, it resumes from the last saved phase on restart.

| Phase | What happens |
|---|---|
| COLLECT | Calls `getLagSummary()` directly, no LLM |
| EVALUATE | Sends lag JSON to LLM for WARN/CRITICAL classification |
| REMEDIATE | Parses LLM output, calls tools directly |
| REPORT | Logs structured summary |

## Prerequisites

- MCP Server running on `http://localhost:8080`
- Java 25
- **Groq** (default): set `GROQ_API_KEY` env var
- **Ollama** (alternative): running on `http://localhost:11434` with `qwen2.5:14b` pulled; remove `spring.profiles.active: groq` from `application.yml`

## Running

```bash
./mvnw spring-boot:run
# Runs on http://localhost:8081
# H2 console: http://localhost:8081/h2-console
# Health: http://localhost:8081/actuator/health
```

## Configuration

| Property | Default | Description |
|---|---|---|
| `agent.mode` | `agentic` | `agentic` or `phased` |
| `agent.poll-interval-ms` | `60000` | Loop interval in ms |
| `agent.warn-threshold` | `1000` | Total lag for WARN |
| `agent.critical-threshold` | `10000` | Total lag for CRITICAL |
| `agent.remediation.auto-reset-enabled` | `false` | Allow automatic offset reset |
| `agent.remediation.max-actions-per-run` | `3` | Max actions per cycle (phased mode) |
| `spring.ai.ollama.chat.options.model` | `qwen2.5:14b` | Ollama model (when not using `groq` profile) |
| `spring.ai.openai.chat.options.model` | `llama-3.3-70b-versatile` | Groq model (configured in `application-groq.yml`) |
| `GROQ_API_KEY` | — | Groq API key (required when `groq` profile is active) |
| `spring.ai.mcp.client.sse.connections.kafka.url` | `http://localhost:8080/sse` | MCP server SSE endpoint |

## Remediation Safety Rules

| Action | When allowed |
|---|---|
| `SEND_ALERT` | Always |
| `RESET_OFFSET_LATEST` | CRITICAL + no active members + `auto-reset-enabled=true` |
| `MARK_PAUSED` | CRITICAL + group state DEAD |
| `RESET_OFFSET_EARLIEST` | Never automatic — too destructive |

## H2 Checkpoint Table

```sql
SELECT * FROM AGENT_CHECKPOINT ORDER BY CREATED_AT DESC;
```

Shows phase progression per run. Useful for debugging crash recovery.

## Key Classes

| Class | Purpose |
|---|---|
| `AgentLoopRunner` | `@Scheduled` entry point |
| `AgentOrchestrator` | Routes to agentic or phased flow |
| `AgenticAgent` | Single LLM call with all tools wired |
| `CollectPhase` | Direct MCP tool call, no LLM |
| `EvaluatePhase` | LLM classification |
| `RemediatePhase` | Direct tool execution based on LLM output |
| `McpToolInvoker` | Invokes MCP tools by name directly |
| `RemediationDecisionTree` | Safety gate for write actions |
| `CheckpointService` | Saves/loads phase state from H2 |
