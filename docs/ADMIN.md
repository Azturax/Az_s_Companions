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

**Save & apply** — writes the AI file and applies settings to the live server LLM runtime (no restart required for admin-editable fields).

Chat after a successful save:

> Companion AI settings saved and applied on the server.

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

Also editable: `model`, `apiKeyEnv`, masked **`apiKey`** (status only over the wire; blank keeps current; Clear clears config key), `inputLanguage`, **Use server LLM** (`serverLlmOnly`), **Idle chat** (`idleChat`), `mcpUrl`. Non-blank `apiKey` in the file wins over `apiKeyEnv` / `AZS_LLM_API_KEY`. Env remains fine for hosts that prefer not to store the key in the config file.

#### Use server LLM

Toggle label **Use server LLM: ON/OFF** writes `serverLlmOnly` (default **ON**).

| Mode | Effect |
|------|--------|
| **ON** (recommended for multiplayer) | This host’s AI config (`provider` / `baseUrl` / `model` / MCP / keys) is authoritative for every companion. Joining clients do not run their own LLM. |
| **OFF** | Only relevant on an integrated singleplayer/LAN host that is not already treated as shared (dedicated servers always use the server endpoint). |

#### Idle chat

Toggle **Idle chat: ON/OFF** writes `idleChat` (default **ON** for new configs). Companions near the owner occasionally speak (~90–240s). Uses the server LLM when configured; otherwise sparse scripted lines. Does not revive AI Mode or name-listen.

API keys always live on the **server** process (config file or env). The admin GUI never sends the stored key to clients — only a status (`config` / `env` / not set). Clients never need their own key when Use server LLM is ON.

#### Join-time “use server LLM?” prompt

On join, players may see a yes/no confirm when Companion AI is available:

- **Dedicated / configured host:** server syncs that AI is enabled (`provider` ≠ `disabled`).
- **Singleplayer / integrated:** if AI is still disabled, the client briefly probes local LiteLLM (`127.0.0.1:4000`), Ollama (`11434`), and LM Studio (`1234`).

**Yes** enables Use server LLM for hosts/admins (and may apply a local profile when AI was off). **No** remembers dismissal for that server address for the client session — no spam, no auto-connect.

**Ask-only (0.3.12+):** companions reply via **`/ask`** / **`/az ask`** only. Admin toggles for `chatListenMode`, `enableAiActions`, and `nameListen` are removed; legacy config keys are ignored.

See [COMPANION_AI.md](COMPANION_AI.md) for full AI config reference.
