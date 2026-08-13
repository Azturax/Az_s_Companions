# Az's Companions — store page copy (1.0.1)

Paste-ready descriptions for Modrinth and CurseForge. Characters are **adult**, **wholesome**, and **non-sexual**. Do not advertise removed features (Jindujun / Flying Nimbus / whistle, Glowing Orb, flight ki aura).

**Version:** 1.0.1 · Minecraft **1.21.1** · NeoForge + Fabric  
**License:** MIT  
**Repo:** https://github.com/Azturax/Az_s_Companions

---

## Modrinth (Markdown)

```markdown
# Az's Companions

A wholesome adult companion for Minecraft **1.21.1** — follow you through the world, fight beside you, sleep at night, and (optionally) chat with you via a local or remote LLM.

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.

---

## What you get

Summon a companion with the **Companion Charm**, then command them with the in-game menu (default keybind **K**) or charm + Shift + right-click:

- **Follow / Stay / Sit / Wander** — Sit uses a visible sit pose on humanoid forms; animal forms keep their native sit
- **Customize** — forms (player + mobs), coat/breed variants, proportions, skins, and **activity outfits** (Sleeping / Bathing / Adventuring) for player form
- **Inventory** — store and manage their gear
- **Combat** — fixed netherite-sword melee; optional team fights for streamers / ops
- **Worldplay** — flower gifts, night sleep (Kon Bed), ride-along when you mount, swim follow, dimension teleport with you (vanilla + modded), logout park / login restore
- **Mob flavor** — cat form scares creepers; wolf form scares skeletons; wander mode can playfully interact with nearby mobs
- **Child Bits** — companions can have children that stick with the family
- **Optional AI** — `/ask` dialogue, idle chatter, and event reactions when you configure an LLM (off by default)

Owned companions **do not naturally despawn**. Desert pyramids can drop a Companion Charm (~5%; disable with `world.enableLoot`).

---

## Editions (pick one jar)

| Jar | Use when |
|-----|----------|
| **Standalone** (NeoForge or Fabric) | Default — no streamer bridge |
| **CCI** (NeoForge or Fabric) | Twitch / Streamlabs / StreamElements via [CCI](https://modrinth.com/mod/content-creator-integration) + iChunUtil |

Install **one** Az's Companions jar per loader — never standalone + CCI together.

**Requirements:** Minecraft **1.21.1**, Java **21**, NeoForge **21.1.x** *or* Fabric Loader ≥0.16 + Fabric API.  
**CCI editions only:** CCI **1.13.0** + iChunUtil **1.0.3** for the same loader.

---

## Soft compatibility

Works without these; nicer with them:

- FTB Teams / Chunks / Ranks (trust & claim-aware behavior)
- Map icons (Xaero's, JourneyMap)
- Dynamic lights soft-detect
- Simple Voice Chat detect (no full TTS bridge)

---

## Multiplayer & hosts

- Companions are **per-owner**. Commands and `/ask` only target **your** companions.
- On disconnect, owned companions **park** and restore near you on login.
- **Chunk tickets** (default on) keep summoned companions / Bits loaded nearby so follow and sleep keep working when you walk away — not FTB claims.
- Dedicated / LAN hosts configure AI once on the **server** (keys stay on the host). See **AI settings** below.

Special UUID perks exist for a few players (wolf companion grant / toggleable flying dog + survival flight). High-level only — no public UUID list on the store page.

---

## AI settings (optional)

**Default: disabled.** Without a provider, companions use short offline / scripted lines only. Enabling AI never forces a cloud API — you choose local or remote.

### How chat works

- Talk with **`/ask <message>`** or **`/az ask <message>`** (optionally name a companion).
- Replies appear as in-character chat for the **owner** (Thinking HUD while waiting).
- Each companion keeps its **own** persona and memory — shared host endpoint ≠ one shared brain.
- First-create **persona** onboarding (Who / What / How + optional speech, relationship, quirks); edit anytime with `/az persona edit`.
- **Idle chat** (toggle in admin): occasional ambient lines when nearby (~75–180s by default).
- **Reactive chat** / **Item-find chat** (admin toggles): event reactions; item-find is the builtin “I found something” line. Hosts can add more via `customChatEvents` in the AI config file.
- **Reactive chatter:** short reactions to nearby events (explosions, darkness → ask for light, crafting gear, notable finds ~once per **14 days** real-time per owner). Uses the LLM when configured; otherwise sparse scripted fallbacks.
- LLM is **text dialogue only** — no “AI Mode” that puppets the world. Gathering / tasks use normal companion behavior commands.

### Join consent & privacy

- On join you may see an optional **yes/no** prompt. It **asks**; it does **not** auto-enable or lock you into a server LLM.
- Your choice is remembered **once** per server (client file) — no nag every join.
- **API keys never leave the host.** Clients only see key status (`config` / `env` / not set), never the secret.
- Prefer env var **`AZS_LLM_API_KEY`** over pasting keys into config files.

### Providers

Configure in **`/az admin`** → **AI Config** (or `/az ai config`) — ops / host / whitelist:

| Profile examples | Typical use |
|------------------|-------------|
| **Disabled** | Default, offline-safe |
| **LM Studio / Ollama** | Local OpenAI-compatible HTTP |
| **LiteLLM / OpenAI / OpenRouter / Groq** | Remote or proxied `/v1/chat/completions` |
| **MCP** | Route chat through an MCP `companion_chat` tool |

Files: Fabric `config/azscompanions-ai.json` · NeoForge `config/azscompanions-ai.toml`.  
**Save & apply** writes the file and hot-applies admin fields (no restart required for those).

### Use server LLM

- **OFF (default):** Personal / host-local setup. Great for singleplayer or your own LAN host pointing at LM Studio, Ollama, LiteLLM, or a remote API.
- **ON (opt-in):** Shared **host** endpoint for everyone on that world. Joiners do not need their own keys or local LLM.
- On **dedicated** servers, `/ask` always runs in the **server process** (no per-joiner client LLM). Turn **Use server LLM** on when you want that shared endpoint intentional.

### Knobs players & admins care about

| Setting | What it does |
|---------|----------------|
| `provider` | `disabled` / `local` / `openai_compatible` / `mcp` |
| `baseUrl` + `model` | Endpoint and model id |
| `apiKey` / `apiKeyEnv` | Secrets on the host only |
| **Use server LLM** (`serverLlmOnly`) | Opt-in shared host LLM |
| **Idle chat** (`idleChat`) | Ambient lines nearby |
| **Reactive chat** (`reactiveChat`) | Event reactions (TNT, dark, crafts, …) |
| **Item-find chat** (`itemFindChat`) | Builtin “I found something” |
| `customChatEvents` | Host-defined extra events (AI config file) |
| `idleChatSecondsMin` / `Max` | Ambient interval (default ~75–180s) |
| `maxParallelRequests` | Concurrent LLM calls (default 2; `/ask` jumps the queue) |
| `timeoutSeconds` / `connectTimeoutSeconds` | Generation vs fail-fast connect |
| `maxTokens` | Reply length cap (ambient capped shorter) |
| `perCompanionMemory` / `memoryMaxMessages` | Separate minds + history depth |
| `inputLanguage` | Preferred reply language |
| `enableChatMessages` | Show replies as owner chat lines |

Status: `/az ai status`. Full reference: [Companion AI docs](https://github.com/Azturax/Az_s_Companions/blob/main/docs/COMPANION_AI.md).

### Safe setup (recommended)

1. Leave provider **Disabled** until you want AI.
2. Start a local server (LM Studio / Ollama / LiteLLM) **or** pick a remote OpenAI-compatible API.
3. Open `/az admin` → AI Config → choose a profile → set model → save.
4. Put the key in **`AZS_LLM_API_KEY`** (or masked `apiKey` on the host only).
5. Try `/ask hello` near your companion.
6. For multiplayer sharing, turn **Use server LLM** ON only when you intend a host-wide endpoint.
7. Turn **Idle chat** OFF if you only want on-demand `/ask`.

---

## Commands (quick)

- `/az` — primary root (alias `/azscompanions`)
- `/ask` · `/az ask` — talk to your companion
- `/az admin` · `/az ai config` — admin / AI panel
- `/az persona …` — view / edit persona
- `/az teamfight on|off|status` — ops team-fight toggle

---

## Known limitations

- NeoForge **26.2** / newer Minecraft lines: not shipped in 1.0.1
- VoiceMod TTS / full SVC entity speech: detect-only soft-compat
- Old saves with removed items/forms simply no longer spawn those features

---

## Links

- [GitHub](https://github.com/Azturax/Az_s_Companions)
- [Companion AI guide](https://github.com/Azturax/Az_s_Companions/blob/main/docs/COMPANION_AI.md)
- [CCI / streaming](https://github.com/Azturax/Az_s_Companions/blob/main/docs/CCI.md)
- [Admin panel](https://github.com/Azturax/Az_s_Companions/blob/main/docs/ADMIN.md)
```

---

## CurseForge (HTML-friendly)

CurseForge’s description editor accepts a subset of HTML. Paste the block below. If the editor strips tags, use the Modrinth Markdown above and convert headings/lists in the CF UI.

**CF tips**

- Prefer `<h2>` / `<h3>`, `<ul><li>`, `<p>`, `<strong>`, `<code>`, `<a href="…">`, `<hr/>`, simple `<table>` if allowed.
- Avoid Markdown tables if the preview breaks — the HTML version uses lists instead where needed.
- Upload **one** edition jar per file (standalone vs CCI); mention both loaders in the description.
- Short summary / subtitle field (if present): *Wholesome adult companions for 1.21.1 — follow, fight, customize, optional LLM chat.*

```html
<h2>Az's Companions</h2>
<p>A wholesome adult companion for Minecraft <strong>1.21.1</strong> — follow you through the world, fight beside you, sleep at night, and (optionally) chat with you via a local or remote LLM.</p>
<p>Characters are explicitly <strong>adult</strong>, <strong>wholesome</strong>, and <strong>non-sexual</strong>.</p>

<hr/>

<h2>What you get</h2>
<p>Summon a companion with the <strong>Companion Charm</strong>, then command them with the in-game menu (default keybind <strong>K</strong>) or charm + Shift + right-click:</p>
<ul>
  <li><strong>Follow / Stay / Sit / Wander</strong> — Sit uses a visible sit pose on humanoid forms; animal forms keep their native sit</li>
  <li><strong>Customize</strong> — forms (player + mobs), coat/breed variants, proportions, skins, and <strong>activity outfits</strong> (Sleeping / Bathing / Adventuring) for player form</li>
  <li><strong>Inventory</strong> — store and manage their gear</li>
  <li><strong>Combat</strong> — fixed netherite-sword melee; optional team fights for streamers / ops</li>
  <li><strong>Worldplay</strong> — flower gifts, night sleep (Kon Bed), ride-along when you mount, swim follow, dimension teleport with you (vanilla + modded), logout park / login restore</li>
  <li><strong>Mob flavor</strong> — cat form scares creepers; wolf form scares skeletons; wander mode can playfully interact with nearby mobs</li>
  <li><strong>Child Bits</strong> — companions can have children that stick with the family</li>
  <li><strong>Optional AI</strong> — <code>/ask</code> dialogue, idle chatter, and event reactions when you configure an LLM (off by default)</li>
</ul>
<p>Owned companions <strong>do not naturally despawn</strong>. Desert pyramids can drop a Companion Charm (~5%; disable with <code>world.enableLoot</code>).</p>

<hr/>

<h2>Editions (pick one jar)</h2>
<ul>
  <li><strong>Standalone</strong> (NeoForge or Fabric) — default, no streamer bridge</li>
  <li><strong>CCI</strong> (NeoForge or Fabric) — Twitch / Streamlabs / StreamElements via CCI + iChunUtil</li>
</ul>
<p>Install <strong>one</strong> Az's Companions jar per loader — never standalone + CCI together.</p>
<p><strong>Requirements:</strong> Minecraft <strong>1.21.1</strong>, Java <strong>21</strong>, NeoForge <strong>21.1.x</strong> <em>or</em> Fabric Loader ≥0.16 + Fabric API.<br/>
<strong>CCI editions only:</strong> CCI <strong>1.13.0</strong> + iChunUtil <strong>1.0.3</strong> for the same loader.</p>

<hr/>

<h2>Soft compatibility</h2>
<p>Works without these; nicer with them:</p>
<ul>
  <li>FTB Teams / Chunks / Ranks (trust &amp; claim-aware behavior)</li>
  <li>Map icons (Xaero's, JourneyMap)</li>
  <li>Dynamic lights soft-detect</li>
  <li>Simple Voice Chat detect (no full TTS bridge)</li>
</ul>

<hr/>

<h2>Multiplayer &amp; hosts</h2>
<ul>
  <li>Companions are <strong>per-owner</strong>. Commands and <code>/ask</code> only target <strong>your</strong> companions.</li>
  <li>On disconnect, owned companions <strong>park</strong> and restore near you on login.</li>
  <li><strong>Chunk tickets</strong> (default on) keep summoned companions / Bits loaded nearby so follow and sleep keep working when you walk away — not FTB claims.</li>
  <li>Dedicated / LAN hosts configure AI once on the <strong>server</strong> (keys stay on the host). See AI settings below.</li>
</ul>
<p>Special UUID perks exist for a few players (wolf companion grant / toggleable flying dog + survival flight). High-level only — no public UUID list here.</p>

<hr/>

<h2>AI settings (optional)</h2>
<p><strong>Default: disabled.</strong> Without a provider, companions use short offline / scripted lines only. Enabling AI never forces a cloud API — you choose local or remote.</p>

<h3>How chat works</h3>
<ul>
  <li>Talk with <code>/ask &lt;message&gt;</code> or <code>/az ask &lt;message&gt;</code> (optionally name a companion).</li>
  <li>Replies appear as in-character chat for the <strong>owner</strong> (Thinking HUD while waiting).</li>
  <li>Each companion keeps its <strong>own</strong> persona and memory — shared host endpoint ≠ one shared brain.</li>
  <li>First-create <strong>persona</strong> onboarding (Who / What / How + optional speech, relationship, quirks); edit anytime with <code>/az persona edit</code>.</li>
  <li><strong>Idle chat</strong> (toggle in admin): occasional ambient lines when nearby (~75–180s by default).</li>
  <li><strong>Reactive chat</strong> / <strong>Item-find chat</strong> (admin toggles): event reactions; item-find is the builtin “I found something” line. Hosts can add more via <code>customChatEvents</code> in the AI config file.</li>
  <li><strong>Reactive chatter:</strong> short reactions to nearby events (explosions, darkness → ask for light, crafting gear, notable finds ~once per <strong>14 days</strong> real-time per owner). Uses the LLM when configured; otherwise sparse scripted fallbacks.</li>
  <li>LLM is <strong>text dialogue only</strong> — no “AI Mode” that puppets the world. Gathering / tasks use normal companion behavior commands.</li>
</ul>

<h3>Join consent &amp; privacy</h3>
<ul>
  <li>On join you may see an optional <strong>yes/no</strong> prompt. It <strong>asks</strong>; it does <strong>not</strong> auto-enable or lock you into a server LLM.</li>
  <li>Your choice is remembered <strong>once</strong> per server (client file) — no nag every join.</li>
  <li><strong>API keys never leave the host.</strong> Clients only see key status (<code>config</code> / <code>env</code> / not set), never the secret.</li>
  <li>Prefer env var <code>AZS_LLM_API_KEY</code> over pasting keys into config files.</li>
</ul>

<h3>Providers</h3>
<p>Configure in <code>/az admin</code> → <strong>AI Config</strong> (or <code>/az ai config</code>) — ops / host / whitelist:</p>
<ul>
  <li><strong>Disabled</strong> — default, offline-safe</li>
  <li><strong>LM Studio / Ollama</strong> — local OpenAI-compatible HTTP</li>
  <li><strong>LiteLLM / OpenAI / OpenRouter / Groq</strong> — remote or proxied <code>/v1/chat/completions</code></li>
  <li><strong>MCP</strong> — route chat through an MCP <code>companion_chat</code> tool</li>
</ul>
<p>Files: Fabric <code>config/azscompanions-ai.json</code> · NeoForge <code>config/azscompanions-ai.toml</code>.<br/>
<strong>Save &amp; apply</strong> writes the file and hot-applies admin fields (no restart required for those).</p>

<h3>Use server LLM</h3>
<ul>
  <li><strong>OFF (default):</strong> Personal / host-local setup. Great for singleplayer or your own LAN host pointing at LM Studio, Ollama, LiteLLM, or a remote API.</li>
  <li><strong>ON (opt-in):</strong> Shared <strong>host</strong> endpoint for everyone on that world. Joiners do not need their own keys or local LLM.</li>
  <li>On <strong>dedicated</strong> servers, <code>/ask</code> always runs in the <strong>server process</strong> (no per-joiner client LLM). Turn <strong>Use server LLM</strong> on when you want that shared endpoint intentional.</li>
</ul>

<h3>Knobs players &amp; admins care about</h3>
<ul>
  <li><code>provider</code> — <code>disabled</code> / <code>local</code> / <code>openai_compatible</code> / <code>mcp</code></li>
  <li><code>baseUrl</code> + <code>model</code> — endpoint and model id</li>
  <li><code>apiKey</code> / <code>apiKeyEnv</code> — secrets on the host only</li>
  <li><strong>Use server LLM</strong> (<code>serverLlmOnly</code>) — opt-in shared host LLM</li>
  <li><strong>Idle chat</strong> (<code>idleChat</code>) — ambient lines nearby</li>
  <li><strong>Reactive chat</strong> (<code>reactiveChat</code>) — event reactions</li>
  <li><strong>Item-find chat</strong> (<code>itemFindChat</code>) — builtin “I found something”</li>
  <li><code>customChatEvents</code> — host-defined extra events (AI config file)</li>
  <li><code>idleChatSecondsMin</code> / <code>Max</code> — ambient interval (default ~75–180s)</li>
  <li><code>maxParallelRequests</code> — concurrent LLM calls (default 2; <code>/ask</code> jumps the queue)</li>
  <li><code>timeoutSeconds</code> / <code>connectTimeoutSeconds</code> — generation vs fail-fast connect</li>
  <li><code>maxTokens</code> — reply length cap (ambient capped shorter)</li>
  <li><code>perCompanionMemory</code> / <code>memoryMaxMessages</code> — separate minds + history depth</li>
  <li><code>inputLanguage</code> — preferred reply language</li>
  <li><code>enableChatMessages</code> — show replies as owner chat lines</li>
</ul>
<p>Status: <code>/az ai status</code>. Full reference on GitHub: docs/COMPANION_AI.md</p>

<h3>Safe setup (recommended)</h3>
<ol>
  <li>Leave provider <strong>Disabled</strong> until you want AI.</li>
  <li>Start a local server (LM Studio / Ollama / LiteLLM) <em>or</em> pick a remote OpenAI-compatible API.</li>
  <li>Open <code>/az admin</code> → AI Config → choose a profile → set model → save.</li>
  <li>Put the key in <code>AZS_LLM_API_KEY</code> (or masked <code>apiKey</code> on the host only).</li>
  <li>Try <code>/ask hello</code> near your companion.</li>
  <li>For multiplayer sharing, turn <strong>Use server LLM</strong> ON only when you intend a host-wide endpoint.</li>
  <li>Turn <strong>Idle chat</strong> OFF if you only want on-demand <code>/ask</code>.</li>
</ol>

<hr/>

<h2>Commands (quick)</h2>
<ul>
  <li><code>/az</code> — primary root (alias <code>/azscompanions</code>)</li>
  <li><code>/ask</code> · <code>/az ask</code> — talk to your companion</li>
  <li><code>/az admin</code> · <code>/az ai config</code> — admin / AI panel</li>
  <li><code>/az persona …</code> — view / edit persona</li>
  <li><code>/az teamfight on|off|status</code> — ops team-fight toggle</li>
</ul>

<hr/>

<h2>Known limitations</h2>
<ul>
  <li>NeoForge <strong>26.2</strong> / newer Minecraft lines: not shipped in 1.0.1</li>
  <li>VoiceMod TTS / full SVC entity speech: detect-only soft-compat</li>
  <li>Old saves with removed items/forms simply no longer spawn those features</li>
</ul>

<hr/>

<h2>Links</h2>
<ul>
  <li><a href="https://github.com/Azturax/Az_s_Companions">GitHub</a></li>
  <li><a href="https://github.com/Azturax/Az_s_Companions/blob/main/docs/COMPANION_AI.md">Companion AI guide</a></li>
  <li><a href="https://github.com/Azturax/Az_s_Companions/blob/main/docs/CCI.md">CCI / streaming</a></li>
  <li><a href="https://github.com/Azturax/Az_s_Companions/blob/main/docs/ADMIN.md">Admin panel</a></li>
</ul>
```

---

## Short blurbs (optional store fields)

**Tagline / one-liner**

> Wholesome adult companions for Minecraft 1.21.1 — follow, customize, fight, and optionally chat with a local or cloud LLM.

**Very short summary (≈160 chars)**

> Adult wholesome companions for 1.21.1 (NeoForge + Fabric). Charm summon, forms, combat, gifts, optional `/ask` AI. Standalone or CCI editions.

**Categories / tags (suggestions)**

`adventure` · `mobs` · `utility` · `social` · `management` · `cosmetic` · `technology` (AI) · `1.21.1`

---

## Accuracy notes (for maintainers)

- Sourced from README, CHANGELOG **1.0.1**, `docs/COMPANION_AI.md`, `docs/ADMIN.md`, CCI/COMPAT high-level.
- **Not advertised:** Jindujun / Flying Nimbus / whistle, Glowing Orb, flight ki aura, AI Mode / LLM world puppeting.
- UUID perks mentioned only at high level (no spoiler UUIDs).
- AI path emphasized as **ask + idle/reactive**; join consent is once-per-server; `serverLlmOnly` default OFF.
