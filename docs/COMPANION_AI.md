# Companion AI — config guide

Optional **text** dialogue for owned companions (chat lines / `/ask` / `/az ask`). **Default: disabled** — offline scripted dialogue only.

**Multiplayer: configure AI once on the server.** Dedicated servers and LAN hosts load `azscompanions-ai.json` / `.toml` in their own process; every companion uses that **shared server LLM** endpoint (provider/baseUrl/model/MCP). Joining players do **not** need LM Studio or API keys — ask, name-mention, idle, and CCI AI all run **server-side**. Companions remain **independent minds**: each has its own context, personality, and chat memory — they are **not** one shared brain.

| Supported | Not supported |
|-----------|----------------|
| Text replies via local or remote OpenAI-compatible APIs | VoiceMod / TTS / speech synthesis |
| Optional structured **AI actions** (mine/craft/move/…) when enabled | Multi-turn chat memory UI (in-game) |
| MCP tool bridge (`companion_chat`) | Full MCP SDK (resources/prompts) |
| Shared server LLM + **separate minds** per companion (`serverLlmOnly` + `perCompanionMemory`) | Per-client LLM endpoints |

Source of truth: `CompanionAiSettings` + Fabric `CompanionAiConfigIO` / NeoForge `AiConfig`.

---

## Config file paths

Dedicated AI files (not inside the main server config):

| Loader | Path | Format |
|--------|------|--------|
| **Fabric** | `config/azscompanions-ai.json` | JSON (created on first **server** start) |
| **NeoForge** | `config/azscompanions-ai.toml` | TOML (separate from `azscompanions-server.toml`) |

Restart the game / server (or reload NeoForge config if your tooling supports it) after edits. Fabric applies the JSON when the Minecraft **server** starts (dedicated or integrated singleplayer/LAN). Clients that only join a remote server never need a working local provider.

### In-game edit (admins)

Ops / host / whitelist can open **`/az admin`** (or **`/az ai config`**) → **AI Config** tab:

1. Pick a **Profile** (LM Studio, Ollama, OpenRouter, OpenAI, Groq, LiteLLM, MCP, Disabled) or **Custom...** — presets fill defaults; **all fields stay editable**
2. Edit `provider` / `baseUrl` / `model` / `apiKeyEnv` / **`apiKey`** (masked) / `mcpUrl` as needed (tweaks that leave a preset → **Custom...**)
3. Toggle **Use server LLM** (`serverLlmOnly`, default ON) so companions use this host’s endpoint — see below
4. Status shows whether a key is set (`config` / `env` / not set) without revealing the secret. Leave the apiKey box blank to keep the current key; **Clear** removes the stored config key. A typed key writes `apiKey` (file wins over env). Keys always stay on the server.
5. **Save & apply** writes `azscompanions-ai.json` / `.toml` on the **server** and applies to the live LLM runtime (no restart required for these admin fields)

#### Use server LLM (admin toggle)

Maps to config `serverLlmOnly` (default **true**).

- **ON:** Shared **server** LLM endpoint — all companions use the host’s provider/baseUrl/model/MCP. Joining clients do not need LM Studio, Ollama, or API keys. Dedicated servers always behave as shared regardless of this flag.
- **OFF:** Only meaningful on solo integrated singleplayer (or a host not forced into shared mode). Does **not** merge companion minds; `perCompanionMemory` still keeps separate histories.
- **API keys:** Always resolved on the server (`apiKey` in the AI file, or `apiKeyEnv` / `AZS_LLM_API_KEY`). The GUI shows status only over the wire; clients never receive the plaintext key.

#### Join-time consent prompt

After joining a world, if Companion AI is available the client may open a yes/no screen (**Use the server LLM?**):

| Detection | When |
|-----------|------|
| **Server sync** | Host `provider` ≠ `disabled` — S2C offer on login (dedicated + integrated). |
| **Local probe** | Integrated/SP only when AI is still disabled: TCP probe LiteLLM `:4000`, Ollama `:11434`, LM Studio `:1234` (and loopback configured `baseUrl`). |

- **Yes** — accept for this server key (client session). Hosts/admins turn **Use server LLM** on and may apply a matching local profile when AI was disabled; chat tip mentions `/ask`.
- **No** — dismiss for this server key until you reconnect under a new session key; AI is not enabled; nothing auto-connects without consent.

**Ask-only:** use **`/ask`** or **`/az ask`**. Auto chat listen, name-mention listen, and LLM world actions are removed (0.3.12+).

Details + whitelist: [ADMIN.md](ADMIN.md).

---

## Providers

| `provider` | Meaning |
|------------|---------|
| `disabled` | No network AI (**default**, offline-safe) |
| `local` | OpenAI-compatible HTTP → local server (Ollama, LM Studio, llama.cpp) |
| `openai_compatible` | Same HTTP client → **remote** API (OpenAI, OpenRouter, Groq, Together, LiteLLM, Azure-compatible proxies) |
| `mcp` | Chat routed through an MCP server tool (default name `companion_chat`) |

Aliases accepted in config (map to the canonical value):

| You can write | Resolves to |
|---------------|-------------|
| `ollama`, `lmstudio`, `llama_cpp` | `local` |
| `openai`, `openrouter`, `groq`, `together`, `azure_openai`, `litellm` | `openai_compatible` |

`local` and `openai_compatible` share one client (`POST …/v1/chat/completions`). Only `baseUrl` / API key typically differ.

### Admin UI profiles

In `/az admin` → AI Config, a profile cycle fills `provider` + `baseUrl` (and a model placeholder). **Every field stays editable** after any preset; diverging from preset defaults shows **Custom...**. Presets: Disabled, Local (LM Studio `…:1234/v1`), Local (Ollama `…:11434/v1`), OpenRouter, OpenAI, Groq, LiteLLM (`…:4000/v1`), MCP (HTTP). See [ADMIN.md](ADMIN.md).

Companions reply via **`/ask` / `/az ask` only** — no auto chat react, name-mention listen, or LLM world tools.

---

## Security: API keys

**Prefer an environment variable** over putting secrets in the config file.

1. Leave `apiKey` empty (`""`).
2. Set `apiKeyEnv` to `AZS_LLM_API_KEY` (default).
3. Export the key in the shell / launcher that starts Minecraft:

```bash
# Windows (cmd)
set AZS_LLM_API_KEY=sk-or-v1-...

# Windows (PowerShell)
$env:AZS_LLM_API_KEY = "sk-or-v1-..."

# Linux / macOS
export AZS_LLM_API_KEY=sk-or-v1-...
```

Resolution order (`resolveApiKey`): non-blank `apiKey` in config **wins**, else `System.getenv(apiKeyEnv)`.

Do not commit real keys. Config files often live under the instance folder and get copied/shared.

---

## Remote LLM setups (openai_compatible)

Use `provider = "openai_compatible"` (or an alias like `openrouter`) with a public `/v1` base URL and a model id from that provider.

**API key:** optional when the proxy has no auth (e.g. local LiteLLM without `master_key`). Set `AZS_LLM_API_KEY` (or `apiKey`) for OpenAI / OpenRouter / Groq and for LiteLLM when a master/virtual key is enabled. `/az ai status` shows `(no API key)` vs `(key set)` — informational only; `/ask` is not blocked solely for a blank key.

Common bases:

| Provider | `baseUrl` | Example `model` |
|----------|-----------|-----------------|
| OpenAI | `https://api.openai.com/v1` | `gpt-4o-mini` |
| OpenRouter | `https://openrouter.ai/api/v1` | `openai/gpt-4o-mini` |
| Groq | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` |
| Together | `https://api.together.xyz/v1` | (provider model id) |
| **LiteLLM** (local/remote proxy) | `http://127.0.0.1:4000/v1` | model id as configured in LiteLLM |
| Azure / custom proxy | your gateway’s `…/v1` | as exposed by the proxy |

Auth: when a key is resolved, HTTP clients send `Authorization: Bearer <key>`. If the value already starts with `Bearer `, it is not double-prefixed. LiteLLM’s master key (and virtual keys) require this header on **chat** and on **MCP** (`POST /mcp/`). Without a key, no Authorization header is sent (open proxies still work; secured ones return HTTP 401).

### Fabric — LiteLLM proxy (JSON)

```json
{
  "provider": "openai_compatible",
  "baseUrl": "http://127.0.0.1:4000/v1",
  "model": "gpt-4o-mini",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "inputLanguage": "en",
  "timeoutSeconds": 30,
  "maxTokens": 256,
  "enableChatMessages": true,
  "chatListenMode": "off"
}
```

Alias: `provider` may also be `"litellm"` (maps to `openai_compatible`). In `/az admin` → AI Config, pick profile **LiteLLM** to fill these defaults (and seed `mcp.url` to `http://127.0.0.1:4000/mcp/` for MCP use).

### NeoForge — LiteLLM proxy (TOML)

```toml
provider = "openai_compatible"
baseUrl = "http://127.0.0.1:4000/v1"
model = "gpt-4o-mini"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
timeoutSeconds = 30
maxTokens = 256
enableChatMessages = true
chatListenMode = "off"
```

If LiteLLM has a master/virtual key, set it in the **server** JVM env (singleplayer integrated server = same game process):

```bash
# Windows PowerShell
$env:AZS_LLM_API_KEY = "sk-..."

# Verify proxy (same Bearer as the mod uses)
curl -s -H "Authorization: Bearer $env:AZS_LLM_API_KEY" http://127.0.0.1:4000/v1/model/info
```

If the proxy runs **without** auth, leave `apiKey` empty and do not set the env var — status may show `(no API key)` while `/ask` still works.

### Fabric — OpenRouter (JSON)

```json
{
  "provider": "openai_compatible",
  "baseUrl": "https://openrouter.ai/api/v1",
  "model": "openai/gpt-4o-mini",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "inputLanguage": "en",
  "timeoutSeconds": 30,
  "maxTokens": 256,
  "enableChatMessages": true,
  "chatListenMode": "off"
}
```

### NeoForge — OpenRouter (TOML)

```toml
provider = "openai_compatible"
baseUrl = "https://openrouter.ai/api/v1"
model = "openai/gpt-4o-mini"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
timeoutSeconds = 30
maxTokens = 256
enableChatMessages = true
chatListenMode = "off"
```

### Fabric — OpenAI (JSON)

```json
{
  "provider": "openai_compatible",
  "baseUrl": "https://api.openai.com/v1",
  "model": "gpt-4o-mini",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "inputLanguage": "en"
}
```

### NeoForge — OpenAI (TOML)

```toml
provider = "openai_compatible"
baseUrl = "https://api.openai.com/v1"
model = "gpt-4o-mini"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
```

### Fabric — Groq (JSON)

```json
{
  "provider": "openai_compatible",
  "baseUrl": "https://api.groq.com/openai/v1",
  "model": "llama-3.3-70b-versatile",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "inputLanguage": "en"
}
```

### NeoForge — Groq (TOML)

```toml
provider = "openai_compatible"
baseUrl = "https://api.groq.com/openai/v1"
model = "llama-3.3-70b-versatile"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
```

`/az ai status` (alias `/azscompanions ai status`) should show `AI: openai_compatible @ https://… model=… (key set)` when the env var is visible to the JVM.

---

## Local LLM setups

### LM Studio + Gemma (`provider=local`)

1. In LM Studio, start the local server (OpenAI-compatible).
2. Load a Gemma (or other) model and note the served model id.
3. Default LM Studio endpoint: `http://127.0.0.1:1234/v1`.

**Fabric**

```json
{
  "provider": "local",
  "baseUrl": "http://127.0.0.1:1234/v1",
  "model": "google/gemma-2-9b",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "inputLanguage": "en",
  "timeoutSeconds": 60,
  "maxTokens": 256,
  "enableChatMessages": true,
  "chatListenMode": "off"
}
```

**NeoForge**

```toml
provider = "local"
baseUrl = "http://127.0.0.1:1234/v1"
model = "google/gemma-2-9b"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
timeoutSeconds = 60
maxTokens = 256
enableChatMessages = true
chatListenMode = "off"
```

Replace `model` with the exact id LM Studio shows for the loaded model. API key is usually optional for local servers.

### Ollama (`provider=local`)

Default base in code: `http://127.0.0.1:11434/v1`.

```bash
ollama pull llama3.2
```

**Fabric**

```json
{
  "provider": "local",
  "baseUrl": "http://127.0.0.1:11434/v1",
  "model": "llama3.2",
  "apiKey": "",
  "inputLanguage": "en"
}
```

**NeoForge**

```toml
provider = "local"
baseUrl = "http://127.0.0.1:11434/v1"
model = "llama3.2"
apiKey = ""
inputLanguage = "en"
```

---

## MCP custom `companion_chat` server

Set `provider` to `mcp`. The client calls `initialize` then `tools/call` on your server (HTTP Streamable-style or stdio).

**Auth:** every MCP HTTP request (including `POST /mcp/`) sends `Authorization: Bearer <key>` from the same `apiKey` / `AZS_LLM_API_KEY` resolution used for chat. LiteLLM’s MCP gateway (`http://127.0.0.1:4000/mcp/`) needs the same master/virtual key as `/v1/chat/completions` — without Bearer you get `401` / “Malformed API Key… Ensure Key has `Bearer ` prefix.”

Expected tool arguments: `message`, `companion_name`, `form`, `player_name`, `language`, `system_prompt` → text content reply.

### MCP via LiteLLM — Fabric

```json
{
  "provider": "mcp",
  "apiKey": "",
  "apiKeyEnv": "AZS_LLM_API_KEY",
  "inputLanguage": "en",
  "enableChatMessages": true,
  "mcp": {
    "transport": "http",
    "url": "http://127.0.0.1:4000/mcp/",
    "command": "",
    "args": [],
    "toolName": "companion_chat",
    "protocolVersion": "2025-03-26",
    "toolAllowlist": ""
  }
}
```

### MCP via LiteLLM — NeoForge

```toml
provider = "mcp"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
enableChatMessages = true

[mcp]
	transport = "http"
	url = "http://127.0.0.1:4000/mcp/"
	command = ""
	args = []
	toolName = "companion_chat"
	protocolVersion = "2025-03-26"
	toolAllowlist = ""
```

### MCP HTTP — Fabric

```json
{
  "provider": "mcp",
  "inputLanguage": "en",
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

### MCP HTTP — NeoForge

```toml
provider = "mcp"
inputLanguage = "en"
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

### MCP stdio (optional)

```json
{
  "provider": "mcp",
  "mcp": {
    "transport": "stdio",
    "command": "node",
    "args": ["path/to/your-mcp-server.js"],
    "toolName": "companion_chat"
  }
}
```

```toml
provider = "mcp"

[mcp]
	transport = "stdio"
	command = "node"
	args = ["path/to/your-mcp-server.js"]
	toolName = "companion_chat"
```

Empty `toolAllowlist` = only `toolName` is allowed. Comma-separated names expand the allowlist.

---

## Disabled (default)

No network calls; scripted dialogue only.

**Fabric**

```json
{
  "provider": "disabled"
}
```

**NeoForge**

```toml
provider = "disabled"
```

---

## Full config key reference

Types and defaults match `CompanionAiSettings` / loaders. Fabric nests MCP under `"mcp": { … }`; NeoForge uses a `[mcp]` table (same field names).

### Core LLM

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `provider` | string | `disabled` | `disabled` \| `local` \| `openai_compatible` \| `mcp` (see aliases above) |
| `baseUrl` | string | `http://127.0.0.1:11434/v1` | OpenAI-compatible base (`…/v1`). Used by `local` and `openai_compatible`. Client also accepts a host without `/v1` and appends it. |
| `model` | string | `llama3.2` | Model id sent to the API |
| `apiKey` | string | `""` | Inline key (discouraged). Wins over env if non-blank |
| `apiKeyEnv` | string | `AZS_LLM_API_KEY` | Env var name when `apiKey` is empty |
| `systemPrompt` | string | (wholesome companion template) | Placeholders: `{name}`, `{form}`, `{language}`, `{attitude}` |
| `inputLanguage` | string | `en` | Preferred player / reply language (`en`, `de`, `ja`, …) |
| `timeoutSeconds` | int | `30` | HTTP/MCP timeout; clamped **5–120**. Also drives the soft Thinking HUD progress bar |
| `maxTokens` | int | `256` | Completion cap; clamped **32–2048** |
| `maxInputChars` | int | `2000` | Max characters of **one** player chat/ask message kept for the LLM. **Full multi-sentence text is preserved** (no first-sentence trim). Clamped **64–8000** |
| `queueMaxDepth` | int | `4` | While AI is busy, queue up to this many extra requests instead of dropping them (`0` = reject while busy) |
| `enableChatMessages` | bool | `true` | Show LLM/MCP replies as owner chat lines (all forms) |
| `serverLlmOnly` | bool | `true` | **Use server LLM** in `/az admin` → AI Config. Server-authoritative LLM **endpoint**: all companions use this host’s provider/baseUrl/model/MCP/keys. Joining clients’ AI configs are ignored for LLM calls (default **true**; always effectively on for dedicated servers). Singleplayer uses the same local file via the integrated server. Does **not** merge companion minds. |
| `perCompanionMemory` | bool | `true` | Separate rolling chat/history buffers keyed by companion **entity UUID**. Idle / name-mention / ask for companion A never inject companion B’s transcript. Children have their own buffers (may know parent name in the system prompt only). |
| `memoryMaxMessages` | int | `16` | Max prior user+assistant messages kept per companion when `perCompanionMemory` is on; clamped **2–64** |

### Chat listen / auto-react

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `chatListenMode` | string | `off` | `off` — no auto-react · `player` (alias `owner`) — only owner chat · `global` (aliases `all`, `everyone`) — any nearby chat may trigger nearest companion (owner online) |
| `nameListen` | bool | `true` | **Primary chat path.** Saying a companion's display name in normal chat (`Kon, how are you?`, `Bit come here please`) triggers that companion — **no slash command required**. Works even when `chatListenMode` is `off` |
| `chatReaction` | string | — | **Fabric JSON only (legacy alias):** same as `chatListenMode` if `chatListenMode` is absent |
| `chatReactRange` | double | `48` | Max blocks from speaker/owner; clamped **8–128** |
| `chatReactCooldownSeconds` | int | `20` | Per-companion cooldown; clamped **5–600** |
| `censorChat` | bool | `true` | Star-out common profanity in AI prompts and `speakLine` output (alias `filterProfanity`) |
| `censorExtraWords` | string[] | `[]` | Extra whole words to censor |

Slash commands and companion-looking chat lines (`<Name> …`) are ignored for auto-react and name mention.

### Name mention — talk in chat (primary; no slash needed)

With AI enabled and `nameListen=true` (default), type normal chat using their name. **You do not need** `/ask`, `/az ask`, or `/Name ask`.

Examples (`chatListenMode` can stay `off`):

```text
Kon, how are you?
Bit come here please. Then mine some stone.
hey Kon please follow me
Bit dance!
```

Multi-sentence messages after the name are sent to the LLM in full (up to `maxInputChars`). Rapid follow-up chats queue (`queueMaxDepth`) instead of being dropped while Thinking…

Optional extras (still work): `/ask …`, `/az ask …`, chat form `Kon ask hello`.

When `nameListen` is on and AI is enabled:

| Speaker | Mode | Behavior |
|---------|------|----------|
| **Owner** | Owner address | Obey/help tone; **text dialogue only** (no LLM world tools); **no** auto-react cooldown |
| **Other player** | Stranger | Friendly, helpful social chat only (text). No LLM world tools for anyone. |

Stranger `come_here` / `run_at_player` approaches the **speaker** briefly without permanent FOLLOW, and looks at them. Speak lines notify **speaker and owner**. Public chat is **not** canceled for name mentions.

### Thinking HUD

While an AI request is in flight, the owner (and stranger speaker when relevant) sees a **top-right** overlay: spinning gear + companion name + `Thinking...` + a soft progress bar (elapsed / `timeoutSeconds`, capped below 100%). The bar clears when the request completes or fails.

### Idle ambient chat

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `idleChat` | bool | `true` | Occasional ambient LLM lines when owner is online and nearby (scripted fallback if LLM fails/off). Toggle **Idle chat** in `/az admin` → AI Config. Skips sleep/combat, busy LLM worker, and ~45s after any speak line. |
| `idleChatSecondsMin` | int | `90` | Min seconds between ambient lines; clamped **30–3600** |
| `idleChatSecondsMax` | int | `240` | Max seconds (random in `[min,max]`); clamped **30–3600** |

### Call player when away

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `callPlayerWhenAway` | bool | `false` | Call the owner by name when they stay too far for too long |
| `callPlayerAfterSeconds` | int | `90` | Seconds beyond distance before a call; clamped **30–3600** |
| `callPlayerDistance` | double | `48` | Owner farther than this (blocks) counts as away; clamped **8–128** |
| `callPlayerCooldownSeconds` | int | `60` | Cooldown between call lines; clamped **5–600** |

### Ask requires server AI config

**`/ask` and `/az ask` only work when the server has AI configured** (`provider` ≠ `disabled` in the server’s `azscompanions-ai.toml` / `.json`). The host’s LLM is authoritative (`serverLlmOnly` default true) — joining clients do **not** run their own model. If AI is unavailable, ask replies with a clear “not available on this server” message (no client-side fallback).

### AI Mode removed

**AI Mode** (“Let the LLM play the game”) is **removed** — no menu button, no `AiPlayMode` UI, no goal pausing for LLM world tools, no CCI `aiMode=`. Companions keep normal follow/wander/combat goals. LLM is **text chat only** via `/ask` when the server provider is enabled.

Use material gather tasks instead (`/az gather <item> <count> [nearest|look]` or CCI `companion_task`).

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `enableAiActions` | bool | `false` | Unused for world puppeting (AI Mode removed); kept for admin/CCI session hints only |
| `aiActionReach` | int | `5` | Legacy reach clamp **2–16** |
| `aiActionCooldownTicks` | int | `10` | Legacy cooldown clamp **0–100** |

### Child Bit autonomy

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `childAutonomy` | string | `balanced` | `cling` (aliases `close`, `attached`) · `balanced` · `curious` (aliases `explore`, `independent`) |
| `childLeashRadius` | double | `0` | Soft max blocks from parent; **0** = autonomy default (`cling` 6 / `balanced` 10 / `curious` 16). Clamped **0–48** |

### MCP block

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `mcp.transport` | string | `http` | `http` \| `stdio` (aliases `process`, `subprocess`) |
| `mcp.url` | string | `http://127.0.0.1:3001/mcp` | Streamable HTTP endpoint |
| `mcp.command` | string | `""` | Executable for stdio (required when `transport=stdio`) |
| `mcp.args` | string[] | `[]` | Args for stdio command |
| `mcp.toolName` | string | `companion_chat` | Tool invoked for chat |
| `mcp.protocolVersion` | string | `2025-03-26` | MCP protocol version header / init |
| `mcp.toolAllowlist` | string | `""` | Comma-separated allowed tools; empty = only `toolName` |

---

## Default files (all keys)

### Fabric `config/azscompanions-ai.json`

```json
{
  "_comment": "Text dialogue AI. provider: disabled|local|openai_compatible|mcp. chatListenMode: off|player|global. Prefer env API keys.",
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
  "serverLlmOnly": true,
  "perCompanionMemory": true,
  "memoryMaxMessages": 16,
  "censorChat": true,
  "censorExtraWords": [],
  "chatListenMode": "off",
  "nameListen": true,
  "chatReactRange": 48.0,
  "chatReactCooldownSeconds": 20,
  "idleChat": true,
  "idleChatSecondsMin": 90,
  "idleChatSecondsMax": 240,
  "callPlayerWhenAway": false,
  "callPlayerAfterSeconds": 90,
  "callPlayerDistance": 48.0,
  "callPlayerCooldownSeconds": 60,
  "enableAiActions": false,
  "aiActionReach": 5,
  "aiActionCooldownTicks": 10,
  "childAutonomy": "balanced",
  "childLeashRadius": 0.0,
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

### NeoForge `config/azscompanions-ai.toml`

```toml
# Az's Companions — companion AI (text dialogue only).
# Default provider=disabled. Prefer env AZS_LLM_API_KEY over apiKey.
# See docs/COMPANION_AI.md

provider = "disabled"
baseUrl = "http://127.0.0.1:11434/v1"
model = "llama3.2"
apiKey = ""
apiKeyEnv = "AZS_LLM_API_KEY"
inputLanguage = "en"
timeoutSeconds = 30
maxTokens = 256
enableChatMessages = true
serverLlmOnly = true
perCompanionMemory = true
memoryMaxMessages = 16
censorChat = true
censorExtraWords = []
chatListenMode = "off"
nameListen = true
chatReactRange = 48.0
chatReactCooldownSeconds = 20
idleChat = true
idleChatSecondsMin = 90
idleChatSecondsMax = 240
callPlayerWhenAway = false
callPlayerAfterSeconds = 90
callPlayerDistance = 48.0
callPlayerCooldownSeconds = 60
enableAiActions = false
aiActionReach = 5
aiActionCooldownTicks = 10
childAutonomy = "balanced"
childLeashRadius = 0.0

[mcp]
	transport = "http"
	url = "http://127.0.0.1:3001/mcp"
	command = ""
	args = []
	toolName = "companion_chat"
	protocolVersion = "2025-03-26"
	toolAllowlist = ""
```

(`systemPrompt` is also present in NeoForge with the same default string; omitted above for brevity — edit in-game config or the generated TOML.)

---

## Commands (ask + `/az`)

Mod root is **`/az`**. Legacy **`/azscompanions`** redirects to the same tree.

| Command | Effect |
|---------|--------|
| `/ask <message>` | Ask your **nearest** owned companion |
| `/az ask <message>` | Same as `/ask` |
| `/az ask <Name> <message>` | Ask your owned companion whose display name matches `<Name>` (sanitized, case-insensitive) |
| `Kon ask <message>` | Chat (no slash): same as named `/az ask`, **only if you own** that companion; message is not broadcast when accepted |
| `/az ai status` | Provider / key status |
| `/az persona [nearest\|Name]` | Show Who / What / How / speech / relationship / quirks for nearest or named owned companion |
| `/az persona set who\|what\|how\|speech\|relationship\|quirks <text…>` | Set a persona field on nearest owned companion (marks initialized) |
| `/az persona <Name> set who\|what\|how\|speech\|relationship\|quirks <text…>` | Same, named target |
| `/az persona clear` | Clear persona text (keeps `personaInitialized` — no re-onboarding) |
| `/az persona edit` | Re-open full Persona setup GUI (scrollable; all fields) anytime |
| `/az teamfight on\|off\|status` | Ops team-fight toggle (alias under `/azscompanions`) |

**Not used:** global dynamic `/kon` Brigadier roots (they clash between players and other mods). Name targeting is always **resolved against the commanding player’s owned companions**.

---

## Per-companion persona (Who / What / How + optional extras)

Each companion has an independent mind. Persona fields persist in entity NBT (survives charm store/summon, dimension change) and are injected into that companion’s LLM system prompt.

| Property | Meaning | NBT key |
|----------|---------|---------|
| `whoAmI` | Who am I — identity, role, backstory | `WhoAmI` |
| `whatAmIDoing` | What am I doing — goals, duties, current focus | `WhatAmIDoing` |
| `howWillIBe` | How will I be — personality, tone, temperament | `HowWillIBe` |
| `speechStyle` | Optional speech flavor | `SpeechStyle` |
| `relationshipToOwner` | Optional bond to owner | `RelationshipToOwner` |
| `quirks` | Optional mannerisms | `Quirks` |
| `personaInitialized` | First-create onboarding done (or CCI/command set) | `PersonaInitialized` |

Empty fields → generic friendly companion prompt. Max length 2048 per text field.

### First-create onboarding (once only)

Triggers when a **new** primary companion is created and `personaInitialized` is false:

- Charm first bind / recruit
- Creator **Done** (if still unset)
- CCI `companion_summon` without persona keys

The Persona setup GUI shows **all** fields in a scrollable panel:

1. Who am I
2. What am I doing
3. How will I be
4. Speech style (optional)
5. Relationship to owner (optional)
6. Quirks (optional)

Mouse wheel / scrollbar when content exceeds the panel. Save, Skip, or close marks `personaInitialized`. Does **not** re-open on charm recall/store, CCI recall of an existing companion, or dimension change. Revisit anytime with `/az persona edit` (same full scrollable form) or `/az persona set …`.

Owner-only: speak lines + Persona setup GUI go to the owner.

### CCI

```text
companion_persona whoAmI=A knight;whatAmIDoing=Guard the gate;howWillIBe=Stoic and warm;speechStyle=Formal;relationshipToOwner=Sworn protector;quirks=Hums when idle
companion_modify who=Scout;what=Mining;how=Cheerful;quirks=Collects shiny rocks
companion_summon form=wolf;name=Bit;whoAmI=A pup;whatAmIDoing=Following;howWillIBe=Playful
```

Setting any persona key via CCI marks `personaInitialized` and **skips** first-create onboarding.

Aliases: `who`/`whoAmI`, `what`/`whatAmIDoing`, `how`/`howWillIBe`, plus `speech`/`speechStyle`, `relationship`/`relationshipToOwner`, `quirks`/`quirk`.

---

## Multiplayer notes

**Configure AI once on the server** — not on each player's client.

- **Shared server LLM (`serverLlmOnly` / admin **Use server LLM**, default true):** The dedicated server or LAN host loads `config/azscompanions-ai.json` / `.toml` (and env `AZS_LLM_API_KEY` / optional file `apiKey`). Every companion on that world uses that **same provider/baseUrl/model/MCP endpoint**. Players joining remotely do **not** need LM Studio, Ollama, or API keys.
- **Separate minds (`perCompanionMemory`, default true):** Shared endpoint ≠ shared brain. Each companion has its own rolling chat history keyed by **entity UUID**, plus a system prompt built from **that** companion’s name, form, attitude, and child/parent flags — never another companion’s recent chat. Idle / name-mention / ask for A cannot inject B’s transcript. `memoryMaxMessages` (default 16) caps the buffer. Child Bits get their **own** history (parent name may appear in the prompt only).
- **Singleplayer / offline:** The integrated server reads the same config file in your instance — local setup still works as before.
- **Where LLM runs:** `/ask`, name-mention, chat listen, idle/call-away, and CCI AI subjects execute on the **server** process only (`CompanionAiRuntime`). Clients never call the LLM for companions.
- **Ownership:** `/ask`, `/az ask`, and chat `Name ask …` only target companions **owned by the commanding player**. Two players can both have a companion named Kon — each command hits **their own** Kon only.
- **Replies:** Companion dialogue is sent to the **owner’s client** (`speakLine`), not public server chat. Stranger name mentions also notify the **speaker**.
- **Chat listen / name mention:**
  - `player` — only the owner’s chat near their companion.
  - `global` — nearby chat may prompt a companion whose owner is online; the **reply still goes to the owner**. Non-owners get **stranger** prompts + social-safe actions only (no mine/build/inventory).
  - `nameListen` (default true) — `Bit, come here` listens by name even when `chatListenMode=off`; prefers speaker’s owned companion, else nearest with online owner; strangers socialize safely; does **not** cancel public chat.
  - `censorChat` / `censorExtraWords` — filter prompts and speak lines.
  - Per-companion **and** per-owner cooldowns (`chatReactCooldownSeconds`) rate-limit listen spam on dedicated servers.
- **LLM scope:** Text chat only (`/ask`, name listen, idle/call-away). World control uses Behavior tasks (gather/deposit/mine/…), not LLM tools. AI Mode menu/NBT/CCI toggle is removed.
- **FTB suite (optional):** Same FTB team / claim walk-vs-interact / AI claim tools / rank gates — see [COMPAT.md](COMPAT.md) (`ftbTeamsCompat`, `ftbChunksAllowPresence`, `ftbChunksBlockInteraction`, `ftbChunksAiClaim`, `ftbRanksCompat`, `trustSameTeamAsOwner`).
- **Children / Bits:** Same owner UUID as the parent/streamer; ask and AI actions use the same ownership checks. Each Bit still has an **independent** AI memory buffer.
- **CCI:** Streamer client packet binds summons and AI to that player on dedicated servers (see [CCI.md](CCI.md)); LLM still uses the **server** AI config.
- **Busy lock:** One in-flight LLM request at a time per server process (`CompanionAiRuntime`).
- **Status:** `/az ai status` on a running server may show `[server LLM shared]` and `[separate minds]` when those modes apply.
---

## How to test

1. Configure a provider (local, remote, or MCP) and restart / reload.
2. Summon your companion (charm) and stand nearby.
3. Check status:

```text
/az ai status
```

Examples of status text:

- `AI: disabled (scripted dialogue only)`
- `AI: local OpenAI-compatible @ http://127.0.0.1:1234/v1 model=… lang=en chatListen=off [server LLM shared]`
- `AI: openai_compatible @ https://openrouter.ai/api/v1 model=… (key set) chatListen=player [server LLM shared]`
- `AI: mcp http url=http://127.0.0.1:3001/mcp tool=companion_chat …`

If status shows `(no API key)`, the JVM does not see `AZS_LLM_API_KEY` (or `apiKey`). That is fine for open local proxies; for secured remote APIs / LiteLLM with a master key, set the env on the **server** process and restart.

4. Ask explicitly:

```text
/ask Hello, how are you?
/az ask Kon Hello, how are you?
```

Or in chat (owned companion named Kon):

```text
Kon ask Hello, how are you?
```

You should see a thinking indicator, then a short in-character chat line when `enableChatMessages` is true.

5. Optional auto-listen — set `chatListenMode`:

| Mode | Behavior |
|------|----------|
| `off` | Explicit ask (`/ask`, `/az ask`, chat `Name ask`) + name mentions when `nameListen` + idle/call if enabled |
| `player` | Owner's chat near the companion triggers a reply |
| `global` | Any player's chat may trigger the nearest companion (owner online); reply to owner; strangers get helpful dialogue and limited social actions only |

Name mention (independent of mode when `nameListen=true`):

```text
Bit, come here
```

Then say something in chat (not a `/` command and not `Name ask …`) within `chatReactRange`.

6. Optional: set `idleChat` / `callPlayerWhenAway` to exercise ambient and call-away prompts.

---

## Dialogue for every mob form

Scripted greet/say/success and AI replies use form-agnostic owner chat — **player, animal, and hostile** forms all get text dialogue. CCI `companion_say` / greet / wave use the same path (see [CCI_STREAMING_GUIDE.md](CCI_STREAMING_GUIDE.md)).

Optional NeoForge client sound events for dialogue categories remain separate (vanilla Minecraft sounds — **not** VoiceMod).

---

## Deferred / out of scope

- Custom TTS / VoiceMod (not shipped)
- Multi-turn chat memory **UI** (server-side per-companion buffers via `perCompanionMemory` are shipped)
- Full MCP SDK (resources/prompts) — minimal `initialize` + `tools/call` only
- Fabric dialogue sound-packet parity with NeoForge
