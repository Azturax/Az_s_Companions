# Companion AI (text dialogue)

Optional **text** chat replies for owned companions. **Default: disabled** (offline scripted dialogue only). No VoiceMod / TTS integration.

Use `/azscompanions ask <message>` near your companion, and `/azscompanions ai status` to inspect the active provider.

## Providers

| `provider` | Meaning |
|------------|---------|
| `disabled` | No network AI (default) |
| `local` | OpenAI-compatible HTTP → local server (Ollama, LM Studio, llama.cpp) |
| `openai_compatible` | Same HTTP client → remote API (OpenAI, OpenRouter, Groq, Together, Azure-compatible proxies) |
| `mcp` | Chat routed through an MCP server tool (`companion_chat` by default) |

`local` and `openai_compatible` share one client; only base URL / API key differ. MCP can wrap a local or remote model behind the server.

## Non-local LLMs

Yes — any OpenAI-compatible Chat Completions endpoint:

- OpenAI: `https://api.openai.com/v1`
- OpenRouter / Groq / Together / self-hosted gateways: their `/v1` base
- Prefer env `AZS_LLM_API_KEY` over putting secrets in config

## Config

Dedicated AI config files (not inside the main server config). Default `provider` is **`disabled`**.

| Loader | Path |
|--------|------|
| **Fabric** | `config/azscompanions-ai.json` (created on first launch) |
| **NeoForge** | `config/azscompanions-ai.toml` (separate from `azscompanions-server.toml`) |

| Key | Notes |
|-----|--------|
| `provider` | `disabled` / `local` / `openai_compatible` / `mcp` |
| `baseUrl` | Default `http://127.0.0.1:11434/v1` |
| `model` | e.g. `llama3.2`, `gpt-4o-mini` |
| `apiKey` / `apiKeyEnv` | Prefer env (`AZS_LLM_API_KEY`) |
| `systemPrompt` | `{name}` `{form}` `{language}` |
| `inputLanguage` | Player language (`en`, `de`, `ja`, …) |
| `timeoutSeconds` / `maxTokens` | Limits |
| `enableChatMessages` | Owner chat lines for replies (all forms) |
| `mcp.transport` | `http` or `stdio` |
| `mcp.url` / `mcp.command` / `mcp.args` | Endpoint or subprocess |
| `mcp.toolName` | Default `companion_chat` |
| `mcp.toolAllowlist` | Empty = only `toolName` |

### Default Fabric (`config/azscompanions-ai.json`)

```json
{
  "_comment": "Text dialogue AI. provider: disabled | local | openai_compatible | mcp. Prefer env API keys.",
  "provider": "disabled",
  "baseUrl": "http://127.0.0.1:11434/v1",
  "model": "llama3.2",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "systemPrompt": "You are {name}, a wholesome adult Minecraft companion (form: {form}). Stay in character, keep replies short (1-3 sentences), never be sexual or cruel. The player speaks in {language}. Reply in that language unless they ask otherwise.",
  "inputLanguage": "en",
  "timeoutSeconds": 30,
  "maxTokens": 256,
  "enableChatMessages": true,
  "mcp": {
    "transport": "http",
    "url": "http://127.0.0.1:3001/mcp",
    "command": "",
    "args": [],
    "toolName": "companion_chat",
    "protocolVersion": "2025-03-26",
    "toolAllowlist": ""
  }
}
```

### Default NeoForge (`config/azscompanions-ai.toml`)

```toml
# Az's Companions — companion AI (text dialogue only).
# Default provider=disabled. Prefer env AZS_LLM_API_KEY over apiKey.

provider = "disabled"
baseUrl = "http://127.0.0.1:11434/v1"
model = "llama3.2"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
timeoutSeconds = 30
maxTokens = 256
enableChatMessages = true

[mcp]
	transport = "http"
	url = "http://127.0.0.1:3001/mcp"
	command = ""
	args = []
	toolName = "companion_chat"
	protocolVersion = "2025-03-26"
	toolAllowlist = ""
```


## Examples

### Local Ollama

```json
{
  "provider": "local",
  "baseUrl": "http://127.0.0.1:11434/v1",
  "model": "llama3.2",
  "inputLanguage": "en"
}
```

### Remote OpenAI-compatible

```bash
set AZS_LLM_API_KEY=sk-...
```

```json
{
  "provider": "openai_compatible",
  "baseUrl": "https://openrouter.ai/api/v1",
  "model": "openai/gpt-4o-mini",
  "inputLanguage": "de"
}
```

### MCP HTTP

```json
{
  "provider": "mcp",
  "inputLanguage": "en",
  "mcp": {
    "transport": "http",
    "url": "http://127.0.0.1:3001/mcp",
    "toolName": "companion_chat"
  }
}
```

Expected MCP tool args: `message`, `companion_name`, `form`, `player_name`, `language`, `system_prompt` → text content reply.

## Dialogue for every mob form

Scripted greet/say/success and AI replies use form-agnostic `speakLine` / owner chat — **player, animal, and hostile** forms all get text dialogue. CCI `companion_say` / greet / wave use the same path.

Optional NeoForge client sound events for dialogue categories remain separate (Minecraft sounds, not VoiceMod).

## Deferred

- Custom TTS / VoiceMod (not shipped)
- Multi-turn chat memory UI
- Full MCP SDK (resources/prompts) — minimal `initialize` + `tools/call` only
- Fabric dialogue sound-packet parity with NeoForge
