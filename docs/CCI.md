# CCI Documentation — Az's Companions

Complete guide for **iChun Content Creator Integration (CCI)** with Az's Companions **CCI edition** jars.

- **Repo:** [Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Companion AI:** [COMPANION_AI.md](COMPANION_AI.md)
- **Streaming deep-dive (legacy alias):** [CCI_STREAMING_GUIDE.md](CCI_STREAMING_GUIDE.md) — prefer this page as the full manual
- Official CCI docs: [content-creator-integration.readthedocs.io](https://content-creator-integration.readthedocs.io/)

---

## 1. What CCI is / which jar

CCI connects Twitch (or Streamlabs / StreamElements) events to in-game outcomes via **IMC** (`modId` + `subject` + `message`).

From **v1.0.8**, CCI is a **soft/optional** dependency inside the **main** Az's Companions jars — there are **no** separate `*-cci` jars:

| Jar | Use when |
|-----|----------|
| `azscompanions-neoforge-*.jar` | NeoForge 1.21.1 / 1.21.5 — CCI activates when CCI + iChunUtil are installed |
| `azscompanions-fabric-*.jar` | Fabric 1.21.1 / 1.21.5 / 1.20.1 — same |
| `azscompanions-forge-*.jar` | Forge **1.20.1 only** — same |
| NeoForge **26.x** jars | **No CCI** for that line |

Without CCI installed, companions work normally (bridge idle). With CCI present, stream IMC outcomes and Fabric `/azscci` work. NeoForge vs Fabric remain **separate** jars.

Install **one** Az's Companions jar only for your loader.

**IMC mod id:** `azscompanions`

Match **Minecraft version + loader** for CCI and iChunUtil. Full Modrinth version-id table: [MULTI_VERSION.md](MULTI_VERSION.md).

---

## 2. Prerequisites

Pins differ by Minecraft line — **do not** mix Modrinth version ids across MC versions.

### Minecraft 1.21.1 (default product line)

| Requirement | Version |
|-------------|---------|
| Minecraft | **1.21.1** |
| Java | **21** |
| NeoForge | **21.1.x** *or* Fabric Loader ≥0.16 + Fabric API |
| CCI | **1.13.0** (same loader) |
| iChunUtil | **1.0.3** (same loader) |

| Mod | NeoForge | Fabric |
|-----|----------|--------|
| [CCI 1.13.0](https://modrinth.com/mod/content-creator-integration) | [AySbAgcO](https://modrinth.com/mod/content-creator-integration/version/AySbAgcO) | [PERd6IT9](https://modrinth.com/mod/content-creator-integration/version/PERd6IT9) |
| [iChunUtil 1.0.3](https://modrinth.com/mod/ichunutil) | [OvIyyNh4](https://modrinth.com/mod/ichunutil/version/OvIyyNh4) | [gfAOoiwe](https://modrinth.com/mod/ichunutil/version/gfAOoiwe) |

### Minecraft 1.21.5

| Mod | NeoForge | Fabric |
|-----|----------|--------|
| CCI 1.13.0 | [WRDFe2RG](https://modrinth.com/mod/content-creator-integration/version/WRDFe2RG) | [5wvklhb4](https://modrinth.com/mod/content-creator-integration/version/5wvklhb4) |
| iChunUtil **1.0.7** | [Hrl6YCrv](https://modrinth.com/mod/ichunutil/version/Hrl6YCrv) | [BEq7Tobw](https://modrinth.com/mod/ichunutil/version/BEq7Tobw) |

### Minecraft 1.20.1 (Forge or Fabric — no NeoForge)

| Mod | Forge | Fabric |
|-----|-------|--------|
| CCI 1.13.0 | [nNaAlKHI](https://modrinth.com/mod/content-creator-integration/version/nNaAlKHI) | [7tk12xkN](https://modrinth.com/mod/content-creator-integration/version/7tk12xkN) |
| iChunUtil **1.0.3** | [W6d0pCyu](https://modrinth.com/mod/ichunutil/version/W6d0pCyu) | [JjEWQx5u](https://modrinth.com/mod/ichunutil/version/JjEWQx5u) |

---

## 3. Install (step-by-step)

1. Install your target Minecraft (**1.21.1**, **1.21.5**, or **1.20.1**) + matching loader (NeoForge / Fabric / Forge).
2. Drop into `mods/` the **matching-line** CCI + iChunUtil (table above) and **one** Az's Companions **CCI** jar.
3. Launch once (client or integrated server). Confirm logs show CCI bridge / AI status line.
4. Open the **CCI Editor**, connect Twitch (or SE/SL).
5. Create an Event → add **IMC** outcome:
   - `modId` = `azscompanions`
   - `subject` = e.g. `companion_greet`
   - `message` = e.g. `$username` or `form=zombie;team=red`
6. Test with Event Viewer / a channel-point redemption.

**Fabric fallback:** `/azscci <subject> [message]` (CommandOutcome) when IMC is awkward.

**Dedicated server:** CCI IMC is normally client-driven; the bridge packet applies on the server as the **streamer player who sent the packet**. That player owns summoned leaders/children. Another player’s “Kon” is never selected.

---

## 4. How IMC / actions work

```
Stream event → CCI IMCOutcome { modId=azscompanions, subject, message }
  → Az's Companions bridge (client) → packet → server applyOnServer(player, action, message)
```

- **subject** maps to an internal action (aliases allowed).
- **message** is free text **or** `key=value` pairs separated by `;` `,` or newlines.
- Bare tokens (no `=`) become form / attitude / team / name fallbacks — see `CciCompanionParams`.
- Twitch chat lines without `=` can be translated by `TeamFightChatParser` into structured params for spawn/score actions.

Ownership: CCI summons / leaders / children are **owned by the streamer player**. Chat listen (`player` / `global`), ask, and idle AI all target that owner’s companions only (multiplayer-safe — never another account’s entity).

### Multiplayer / dedicated servers

| Rule | Detail |
|------|--------|
| Owner bind | Summon / spawn_leader / spawn_child set `OwnerUuid` to the streamer’s UUID from the CCI action packet. |
| Find target | Nearby companion lookup uses `isOwnedBy(streamer)` only (not trusted guests). |
| Ask / AI chat | Same ownership gate as in-game `/ask` / `/az ask [Name]`. |
| Chat listen | `player` = streamer’s chat; `global` = nearby chat may react; strangers get social-safe actions only |
| Ask | `/ask` / `/az ask` — **requires server AI config** (no client LLM) |
| Name mention | Removed (0.3.12 ask-only — use `/ask`) |
| In-game commands | `/ask`, `/az ask`, `Name ask …` resolve names **per commanding player**. |

See also [COMPANION_AI.md](COMPANION_AI.md#multiplayer-notes).

### CCI-summoned companions + chat / AI

**CCI-summoned companions use streamer chat input; AI applies when configured.**

| AI provider | Behavior |
|-------------|----------|
| `disabled` | Canned `say` / `greet` / `wave` text only |
| `local` / `openai_compatible` / `mcp` | `greet` / `wave` generate via LLM (fallback to canned if busy). `companion_ask` / `ai_chat` / in-game chat listen / idle use the full AI pipeline. `say` stays exact unless `ai=true` |

Same ownership rules as charm-summoned companions — one streamer UUID ties everything together.

---

## 5. Every CCI action (aliases, params, failures)

Unless noted, needs an owned companion within **~96 blocks**.

### Chat / modes (6)

| Subject | Aliases | Message | Effect | Failure |
|---------|---------|---------|--------|---------|
| `companion_say` | `say` | Exact line (default `Hello!`) | Owner chat `<Name> …`. With AI on + `ai=true`, LLM rewrite | No companion |
| `companion_greet` | `greet` | Usually `$username` | Thanks supporter; **AI line when provider enabled** | No companion |
| `companion_wave` | `wave` | Optional name | Hello line; **AI when enabled** | No companion |
| `companion_follow` | `follow` | ignored | Mode FOLLOW | No companion |
| `companion_sit` | `sit` | ignored | Mode SIT | No companion |
| `companion_stay` | `stay` | ignored | Mode STAY (no teleport) | No companion |

### Attitude / teams / summon / gear / modify (11)

| Subject | Aliases | Message examples | Effect |
|---------|---------|------------------|--------|
| `companion_set_attitude` | `set_attitude`, `attitude` | `hostile` / `attitude=passive` | Persist PASSIVE/HOSTILE |
| `companion_set_team` | `set_team`, `team` | `red` / `$username` / `team=blue` | teamId (nametag); rivals fight |
| `companion_summon` | `summon` | `form=zombie;attitude=hostile;team=red;skin=Notch` | Recruit owned companion |
| `companion_summon_passive` | `summon_passive` | `form=chicken;team=blue` | Summon PASSIVE |
| `companion_summon_hostile` | `summon_hostile` | `form=skeleton;team=red` | Summon HOSTILE |
| `companion_set_mainhand` | `set_mainhand`, `mainhand` | `minecraft:diamond_sword` / `clear` | Main hand |
| `companion_set_offhand` | `set_offhand`, `offhand` | `minecraft:shield` | Off hand |
| `companion_set_armor` | `set_armor`, `armor` | `helmet=…;boots=…` | Armor slots |
| `companion_set_hand` / `companion_set_equipment` | `set_equipment`, `set_hand` | `mainhand=…;offhand=…;helmet=…` | Any gear keys |
| `companion_modify` | `modify`, `customize`, `edit`, … | `form=wolf;skin=Notch;name=Fluffy;showArmor=false` | Edit **called** companion |
| `companion_turn_evil` | `turn_evil`, `go_evil`, `berserk` | `seconds=10` (5–15) | Temp HOSTILE then restore |

**Summon/modify keys:** `form`, `skin`/`player`, `name`, `attitude`, `team`, `showArmor`/`show_armor`/`armor_visible`, `followRadius`/`teleportDistance`, `personalSpace`, `wanderRadius`, `chunkLoading` (per-companion ticket opt-out; server `companionChunkLoading` must be on), persona keys below, equipment slots, optional `size`/`scale`.

**Forms:** `player`, `chicken`, `wolf`, `cat`, `cow`, `pig`, `sheep`, `fox`, `rabbit`, `bee`, `zombie`, `skeleton`, `spider`, `enderman`, `husk`, `stray`, …  
Mob **form variants** (Customization `<`/`>`): wolf coats, cat breeds, fox `red`/`snow`, rabbit types, sheep wool — NBT `CompanionFormVariant`.

**Failures:** companion limit; invalid item id; no companion nearby for modify.

### Persona (1) — also via summon / modify

| Subject | Aliases | Message | Effect |
|---------|---------|---------|--------|
| `companion_persona` | `persona`, `set_persona` | `whoAmI=…;whatAmIDoing=…;howWillIBe=…` (+ optional `speech=`, `relationship=`, `quirks=`) | Set persona + mark initialized (skips first-create onboarding) |
|  |  | `op=get` | Print persona summary to chat |
|  |  | `op=clear` / `clear=true` | Clear fields but keep initialized (no onboarding re-prompt) |

**Aliases for fields:** `who`/`identity`/`backstory`, `what`/`purpose`/`goal`, `how`/`personality`/`tone`, `speech`/`style`, `relationship`/`ownerbond`, `quirks`.

Setting persona on `companion_summon` / `companion_modify` with the same keys also marks initialized and skips name/persona onboarding.

### AI (4) — requires CCI + AI config

| Subject | Aliases | Message | Effect |
|---------|---------|---------|--------|
| `companion_ask` | `companion_ai`, `ai_ask`, `ask` | `message=What should we build?` or bare text | LLM **text** reply via server `/ask` path |
| `companion_ai_status` | `ai_status` | ignored | Toast provider status (no companion required) |
| `companion_ai_chat` | `ai_chat`, `stream_chat` | `message=hi;speaker=Alice` or bare chat | Explicit CCI chat feed (not auto-listen) |
| `companion_ai_config` | `ai_config`, `set_ai_config` | (legacy keys ignored) | Status toast only — ask-only; `chatListenMode` / `enableAiActions` retired |

When AI is **disabled**, ask/ai_chat toast an error; greet/wave/say fall back to canned text.

### Play (3)

| Subject | Aliases | Message | Effect |
|---------|---------|---------|--------|
| `companion_play` | `play`, `dance`, `peekaboo`, `play_stop` | `mode=rush\|hide\|seek\|hide_seek\|dance\|peekaboo\|stop;seconds=8` | Start / stop play behavior |
| `companion_rush` | `rush`, `run_at_player` | `seconds=6` | Sprint toward owner |
| `companion_hide_seek` | `hide_seek`, `hide_and_seek` | `role=hider\|seeker;seconds=12` | Hide or seek |

### FTB claim (2) — optional; needs FTB Chunks + `ftbChunksAiClaim`

| Subject | Aliases | Message | Effect |
|---------|---------|---------|--------|
| `companion_task` | `task`, `gather_task` | `item=minecraft:cobblestone;count=2000;deposit=chest` | Assign material gather → nearest/look chest (NeoForge) |
| `unclaim_chunk` | `companion_unclaim_chunk` | same | Release owner's claim |

### Team fight (8)

| Subject | Aliases | Message | Notes |
|---------|---------|---------|-------|
| `teamfight_enable` | `teamfight_on`, … | — | Enable + HUD |
| `teamfight_disable` | `teamfight_off`, … | — | Disable |
| `teamfight_toggle` | … | — | Toggle |
| `teamfight_status` | … | — | ON/OFF toast |
| `teamfight_scoreboard` | `scoreboard`, … | `show` / `hide` / `reset` / `team1=red;team2=blue` | Needs teamfight ON |
| `teamfight_score` | `teamfight_kill`, … | `team=red;points=1` or `killer=Alice` | Score / kill |
| `teamfight_top` | `teamfight_best`, … | — | Top bits/kills chat |
| `companion_spawn_leader` | `spawn_leader`, … | `name=Alice;form=zombie;team=red` | Needs teamfight ON; no amount gate |
| `companion_interaction` | `support_spawn`, `companion_spawn_child`, … | `amount=500;user=Alice;team=red` or `count=2;maxChildren=5` | Amount path needs teamfight ON; spawn count = `count=` **or** `amount ÷ supportAmountPerCompanion` (default 100); capped only by leader `maxChildren` (default 3) |
| `companion_dismiss_child` | `dismiss_child`, `store_child`, … | `count=1` | Store world children onto parent (callable later; no teamfight required) |

**Also:** `/az teamfight on|off|status` (ops; alias `/azscompanions …`). Fabric: `/azscci teamfight_enable`.

**Gear tiers (auto when no armor keys):** amount 100 leather+stick → 250 chain → 500 iron → 750 diamond mix → 1000 netherite. Quality only — **not** a spawn-count ceiling.

**Caps:** default **3** children/leader (`maxChildCompanionsPerLeader`; living + stored). CCI `maxChildren=` / `childCap=` overrides per companion (hard max 64). No interaction-amount ceilings and no global fight-spawn cap. Children excluded from `maxCompanionsPerPlayer`.

**Override examples:** `companion_interaction` → `maxChildren=8;amount=500;user=Alice` · `companion_modify` → `childCap=5` · `companion_spawn_leader` / `companion_summon` → `maxChildren=8`

**Cake:** right-click companion with cake → one child via `spawnChild` (works even if teamfight off).

**Store / call:** Menu **Remove child** or CCI `dismiss_child` parks children on the parent (`StoredChildren`). Charm/empty-hand click on parent calls them FIFO. Menu badge shows **stored/max**.

**Total first-class subjects:** **37** action enum values (chat/modes + attitude/summon/gear/persona + AI + play + FTB claim + teamfight/spawns + dismiss_child).

---

## 6. Child Bits + AI

Children from `companion_interaction` / `companion_spawn_child` / cake:

- **Owned** by the streamer; `LeaderUuid` points at parent leader.
- **Inherit** parent attitude, team, form, skin, armor visibility; default scale **0.5**, name **Bit** (CCI can override).
- **AI tools / world puppeting** removed (ask is text dialogue only).
- **`childAutonomy`:** `cling` | `balanced` | `curious` — leash + idle frequency (children chatter less than parents).
- Idle may talk **to the parent** when cling/balanced.

Leader caps are unchanged (default 3; raise with `maxChildren=` for stream fights).

---

## 7. Defining interactions (CCI-first)

Streamer bots define events via CCI IMC — there is **no** hardcoded cheer/gift parser. Example:

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_interaction",
  "message": "amount=500;user=Alice;form=chicken;team=red"
}
```

With default `supportAmountPerCompanion=100`, that spawns **5** children under Alice’s leader (or until `maxChildren` slots fill). Optional `unit=` is informational only. Explicit `count=` overrides the amount÷price formula.

Prefer structured IMC. Freeform chat without `key=value` is ignored (map your platform events → CCI in your bot).

---

## 8. Config that affects CCI

| File | Keys |
|------|------|
| `azscompanions-server.toml` / Fabric defaults | `maxCompanionsPerPlayer`, `maxChildCompanionsPerLeader` (default 3), `supportAmountPerCompanion` (default 100), combat, chat messages |
| `azscompanions-ai.json` / `.toml` | `provider`, `baseUrl`, `model`, `apiKeyEnv` (`AZS_LLM_API_KEY`), `serverLlmOnly` (default **false** — opt-in shared host endpoint; SP uses the same file as a personal local/remote LLM), `perCompanionMemory` / `memoryMaxMessages` (separate minds), `idleChat`, `callPlayerWhenAway`, `childAutonomy`, `childLeashRadius`, MCP block. Legacy `chatListenMode` / `nameListen` / `enableAiActions` ignored. |

Full key reference + copy-paste setups (LM Studio, Ollama, **remote** OpenAI/OpenRouter/Groq, MCP, disabled): [COMPANION_AI.md](COMPANION_AI.md).

---

## 9. `cci-examples/` walkthrough

Examples ship **inside the CCI jar** under `cci-examples/` (not auto-loaded). Copy into the CCI Editor.

| File | Subject |
|------|---------|
| `imc-companion-say-outcome.json` | `companion_say` |
| `imc-companion-greet-outcome.json` | `companion_greet` |
| `imc-companion-wave-outcome.json` | `companion_wave` |
| `imc-companion-follow/sit/stay-outcome.json` | modes |
| `imc-companion-summon-hostile.json` | hostile summon |
| `imc-companion-modify.json` | modify called companion (+ spacing / persona / chunkLoading) |
| `imc-companion-persona.json` | set persona (skips onboarding) |
| `imc-companion-rush.json` | rush / run at player |
| `imc-companion-hide-seek.json` | hide-and-seek |
| `imc-claim-chunk.json` | FTB claim at feet |
| `imc-ai-config.json` | status toast only (ask-only; legacy session flags retired) |
| `imc-companion-turn-evil.json` | playful evil |
| `imc-companion-set-team.json` / `set-equipment.json` | team / gear |
| `imc-teamfight-enable.json` | teamfight on |
| `imc-companion-spawn-leader.json` / `spawn-child.json` | leader / interaction spawn |
| `imc-companion-ask.json` | AI ask |
| `command-azscci-greet-outcome.json` | Fabric `/azscci` |
| `command-summon-wolf-alongside.json` | CCI-native `/summon` (not our bridge) |

Example ask outcome:

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_ask",
  "message": "message=Thanks $username — what should we build next?"
}
```

---

## 10. Fabric vs NeoForge

| Topic | NeoForge | Fabric |
|-------|----------|--------|
| Jar | `neoforge-cci` | `fabric-cci` |
| AI config | `azscompanions-ai.toml` | `azscompanions-ai.json` |
| IMC | InterModComms poll + packet | IMCOutcome mixin + packet |
| Command fallback | — | `/azscci <subject> [message]` |
| Subjects | Same enum / aliases | Same |

---

## 11. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Unknown subject | Check spelling/aliases; mod id must be `azscompanions`. Fabric `/azscci` prints a chat error for bad subjects. |
| No companion nearby | Charm-summon or `companion_summon` first; stay within ~96 blocks — action bar: **No companion nearby** |
| Summon failed | Hit `maxCompanionsPerPlayer` (Bits/fight spawns exempt) — toast **Summon failed** |
| AI ask disabled | Set `provider` in AI config; check `/az ai status` or `ai_status` — toast **Companion AI** |
| Greet still canned | Provider still `disabled`, or AI worker busy (falls back) |
| Teamfight spawn idle | `/az teamfight on` first — toast uses existing teamfight lang keys |
| Wrong loader CCI | Match CCI + iChunUtil to Fabric vs NeoForge 1.21.1 |
| Dedicated server | Streamer client with CCI must be online; actions bind to that player’s UUID |
| Wrong player’s Kon | Name ask / CCI find are owner-scoped — check you own the companion |

Streamer feedback: successful and failed CCI outcomes show a short **action-bar** line (`Title — body`) and, when the CCI API is present, an informational toast. Strings live under `assets/azscompanions/lang/en_us.json` (`toast.azscompanions.cci.*`, `message.azscompanions.cci.*`, `dialogue.azscompanions.cci.*`).

---

## 12. Permissions

- Teamfight commands: permission level **2** (ops).
- CCI outcomes run as the **local streamer player** (owner context).
- Griefing/mine/place respect server griefing + companion permissions tags where applicable.
