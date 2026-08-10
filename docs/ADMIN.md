# Az Admin panel

In-game admin UI for server owners / ops / whitelist. Primary command: **`/az admin`** (alias root **`/azscompanions admin`**). Same panel via **`/az ai config`**.

## Access

Allowed when **all** of the following hold:

1. `enableAzAdminCommand=true` (default **true**)
2. **And** one of:
   - Singleplayer / integrated LAN **host owner**
   - Permission level **ops** (2+)
   - Player UUID or name on the whitelist

Denied players get a clear chat message.

### Config keys

| Loader | File | Keys |
|--------|------|------|
| **NeoForge** | `config/azscompanions-server.toml` → `[admin]` | `enableAzAdminCommand`, `adminWhitelist`, `azAdminUsers` |
| **Fabric** | `config/azscompanions-server.json` → `"admin"` | same keys |

`adminWhitelist` and `azAdminUsers` are aliases (UUID and/or player name, case-insensitive names).

## Panel

Two tabs:

### Overview

- Toggle teamfight (your session)
- Print AI status / companion list by player
- Dismiss your owned companions
- Chunk-loading note (`companionChunkLoading`)
- Nearest companion: clear persona, show/hide armor, reset behavior spacing

All actions are **validated server-side** (same access gate).

### AI Config

Edit key LLM settings and **Save** to the dedicated AI file:

| Loader | File |
|--------|------|
| Fabric | `config/azscompanions-ai.json` |
| NeoForge | `config/azscompanions-ai.toml` |

**No hot-reload** — the live LLM client is unchanged until restart.

Chat after a successful save:

> Companion AI settings saved. Restart the server/game for them to apply.

#### Provider profiles

Cycle **Profile** to fill `provider` + `baseUrl` (and a model placeholder). **All fields stay editable** after any preset (including Custom…). Tweaking `provider` / `baseUrl` / `mcpUrl` away from a preset switches the label to **Custom...**.

| Profile | provider | baseUrl (default) |
|---------|----------|-------------------|
| Disabled | `disabled` | (kept / placeholder) |
| Local (LM Studio) | `local` | `http://127.0.0.1:1234/v1` |
| Local (Ollama) | `local` | `http://127.0.0.1:11434/v1` |
| OpenRouter | `openai_compatible` | `https://openrouter.ai/api/v1` |
| OpenAI | `openai_compatible` | `https://api.openai.com/v1` |
| Groq | `openai_compatible` | `https://api.groq.com/openai/v1` |
| LiteLLM | `openai_compatible` | `http://127.0.0.1:4000/v1` (also seeds mcp `…:4000/mcp/`) |
| MCP (HTTP) | `mcp` | mcp url `http://127.0.0.1:3001/mcp` |
| Custom... | (yours) | (yours) |

Also editable: `model`, `apiKeyEnv`, `inputLanguage`, `serverLlmOnly`, `mcpUrl`. Prefer env API keys over putting secrets in the file.

**Ask-only (0.3.12+):** companions reply via **`/ask`** / **`/az ask`** only. Admin toggles for `chatListenMode`, `enableAiActions`, and `nameListen` are removed; legacy config keys are ignored.

See [COMPANION_AI.md](COMPANION_AI.md) for full AI config reference.
