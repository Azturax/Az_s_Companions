# Changelog

## 0.3.9

Release: [v0.3.9](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.9) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fix: Glowing companions rendered as outline-only
- **Symptom:** Player-form companion shows only a bright white silhouette outline + nametag (skin missing). Common for the UUID-gated special perk (forces vanilla Glowing) and any companion with a Glowing potion.
- **Root cause:** The 0.3.7 cutout fix checked `glowing` before `bodyVisible` in `getRenderType`, so a glowing visible companion used `RenderType.outline` instead of the skin mesh. Vanilla only uses outline when the body is hidden.
- **Fix:** NeoForge + Fabric player-form renderers draw cutout/translucent body when visible; outline only when the body is invisible but should still glow. Special-perk glow still applies (skin + outline).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.9+1.21.1.jar` | `azscompanions-fabric-0.3.9+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.9+1.21.1.jar` | `azscompanions-fabric-cci-0.3.9+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.8

Release: [v0.3.8](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.8) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### AI Mode (per companion)
- Shift+RMB menu → **AI Mode: ON/OFF** (“Let the LLM play the game!”).
- When ON: normal follow/wander/combat goals pause; LLM tools drive play (requires provider ≠ disabled).
- When OFF: text/chat only for that companion even if server `enableAiActions` is true.
- Persists as `AiPlayMode` NBT / synced data. CCI: `companion_modify` `aiMode=` / `aiPlayMode=` / `llmPlay=`.

### Name chat without slash
- **`nameListen`** (default true): say the companion’s name in normal chat (`Kon, how are you?`) — no `/ask` or `/az ask` required.
- Works even when `chatListenMode` is `off`. Full multi-sentence messages preserved (up to `maxInputChars`, default 2000).
- While busy, requests queue (`queueMaxDepth`, default 4) instead of dropping. Owner vs stranger modes; strangers stay social-safe.

### Thinking HUD
- Top-right client HUD while a companion AI request is in flight (name + progress/timeout).
- S2C thinking packet; Fabric + NeoForge overlays. Action-bar “thinking” on explicit ask still works.

### Persona setup (scrollable)
- Persona GUI shows **all** fields in a scrollable panel: Who / What / How / speech / relationship / quirks.
- Mouse wheel / scrollbar when content exceeds the panel. Re-open anytime with `/az persona edit`.

### Child store badge
- Menu badge (stored/max) + tooltip; badge click / charm RMB / empty-hand RMB on parent calls the next stored Bit.
- Remains available with AI Mode and other menu actions.

### CCI
- `aiMode` on `companion_modify` for remote AI Mode toggle.
- Interaction / support spawn and persona keys unchanged; AI subjects share the same name-listen + queue pipeline.

### Admin AI profiles
- `/az admin` / `/az ai config` profiles (LM Studio, Ollama, OpenRouter, OpenAI, Groq, MCP, Custom…) remain the in-game way to write `azscompanions-ai.json` / `.toml` (restart to apply).

### Docs
- [COMPANION_AI.md](docs/COMPANION_AI.md) updated for AI Mode, name chat, input limits, thinking HUD, and persona scroll UI.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.8+1.21.1.jar` | `azscompanions-fabric-0.3.8+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.8+1.21.1.jar` | `azscompanions-fabric-cci-0.3.8+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.7

Release: [v0.3.7](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.7) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fix: invisible player-form skins
- **Root cause:** 0.3.6 Fancy Anim compat forced `RenderType.entityTranslucent` for companions. `FeminineCompanionModel` extends `PlayerModel` (translucent by default); falling back to `super.getRenderType` did not restore cutout. On Iris/Sodium (and some GPUs) non-player translucent meshes can draw fully invisible.
- **Fix:** Player-form + cape use `entityCutoutNoCull` / solid by default. Translucent only when `fancyAnim.translucentPlayerSkins=true` **and** EMF/ETF is loaded. Cape/ears unchanged otherwise.
- Fabric + NeoForge **1.21.1**. Docs: [COMPAT.md](docs/COMPAT.md).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.7+1.21.1.jar` | `azscompanions-fabric-0.3.7+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.7+1.21.1.jar` | `azscompanions-fabric-cci-0.3.7+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.6

Release: [v0.3.6](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.6) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Stats / info screen
- **Companion | Owner** read-only panel: Shift+RMB menu → **Stats**, or `/az stats` (nearest) / `/az stats <name>`.
- Companion: name, form, mode, attitude, team, health, follow/personal/wander radii, children, armor visibility, short persona who/what/how, AI snippet when enabled.
- Owner: name, health, food, owned companion count, charm bound status.
- Persona / child count / charm / AI synced via S2C; other fields from entity data. Fabric + NeoForge.

### Az Admin + in-game AI config
- **`/az admin`** / **`/az ai config`** (ops, singleplayer/LAN host, or `adminWhitelist` / `azAdminUsers`): Overview + **AI Config** tabs.
- Provider **profiles** (Disabled, LM Studio, Ollama, OpenRouter, OpenAI, Groq, MCP HTTP, **Custom…**); Save writes `azscompanions-ai.json` / `.toml` **without** hot-reload — chat: *Companion AI settings saved. Restart the server/game for them to apply.*
- NeoForge `[admin]` in `azscompanions-server.toml`; Fabric `azscompanions-server.json`. Docs: [ADMIN.md](docs/ADMIN.md).

### FTB suite soft-compat
- Optional **FTB Teams / Chunks / Ranks** via reflection (no hard dep). Config under AI `[ftb]` / `"ftb"`: `ftbTeamsCompat`, `ftbChunksAllowPresence`, `ftbChunksBlockInteraction`, `ftbChunksAiClaim`, `ftbRanksCompat`, `trustSameTeamAsOwner`, permission nodes.
- **Walk OK, interact blocked:** companions may enter claims; mine/place/build/containers gated by FTB perms. Owner AI optional `claim_chunk` / `unclaim_chunk` (owner quota; no steal).
- Same-team → trusted interact; optional owner-level AI tools; ranks gate ask/actions/CCI/teamfight/spawn.
- Docs: [COMPAT.md](docs/COMPAT.md).

### Map mods soft-compat
- **Xaero Minimap / World Map:** companions show on entity radar (LivingEntity) + bundled charm icon definition.
- **JourneyMap:** API v2 soft plugin — name, charm icon, owner tooltip; hide via `showOnMinimap` / `showChildrenOnMap`.
- Config: NeoForge client `[map]` · Fabric `config/azscompanions-map.json`.
- **FTB Chunks** stays claim-overlay only (map package does not touch FTB claim code). Docs: [COMPAT.md](docs/COMPAT.md).

### Online from LAN / Essential
- Soft-compat for **Essential** (`essential`), **e4mc**, **World Host**, **LAN Server Properties**, and vanilla Open-to-LAN: detect hosted integrated multiplayer without hard deps.
- `integratedMultiplayerSharedLlm` (default true) — host LLM stays authoritative when friends join even if `serverLlmOnly=false`. Dedicated servers unchanged.
- `ownerNameFallback` (default true, integrated hosted MP only) — persist `OwnerName`, match/heal owner UUID after offline↔online remaps. Never on dedicated.
- Docs: [COMPAT.md](docs/COMPAT.md) (“Online from LAN / Essential”).

### Fancy Animations / EMF+ETF soft-compat
- Player-form skins use translucent buffers (player-like) so ETF skin features / emissives / animated frames with alpha render correctly; cape matches.
- Mob-form proxies sync the companion UUID for stable Fresh Animations CEM + ETF random variants on vanilla entity paths.
- Bundled ETF `entityRenderLayerOverride=translucent` hints for companion texture paths. Config: NeoForge `[fancyAnim]` · Fabric `azscompanions-fancyanim.json`. Docs: [COMPAT.md](docs/COMPAT.md).

### Behavior / follow spacing
- **Behavior screen** (Shift+RMB menu → Behavior): **Follow radius** (1–128, default 48), **Personal space** (1–12, default 2), **Wander radius** (3–48, default 16).
- Persists per companion (`FollowRadius` / `PersonalSpace` / `WanderRadius` NBT + synched data) across reload, dimension change, charm store/summon, form change.
- Child Bits inherit parent spacing at 75%. CCI `companion_modify`: `followRadius=` / `teleportDistance=`, `personalSpace=`, `wanderRadius=`.

### Child store / call UX
- Parent keeps a FIFO **`StoredChildren`** list (synced **stored count** + **maxChildren** for UI).
- Default **max 3** Bits per companion (living + stored); CCI `maxChildren=` / `companion_modify` overrides (up to 64).
- Menu **Remove child** / child **Dismiss child**: world Bit → store (count up); inventory stays in the snapshot.
- **Charm RMB or empty-hand RMB** on parent (owner): call next stored Bit in order (count down). Charm air-use still summons/stores the parent; living Bits are parked into that list instead of deleted.
- Menu badge (top-left) shows **stored/max**; tooltip: *Stored children: N / max M — click charm on companion to call*. Badge click also calls next.
- CCI: `dismiss_child` / `companion_dismiss_child` / `store_bit` (works without teamfight).
- **CCI interaction spawn:** `companion_interaction` / `support_spawn` (aliases include `companion_spawn_child`). Message `amount=500;user=Alice` → spawn count = `amount ÷ supportAmountPerCompanion` (default **100** → **5**). Explicit `count=` overrides. No amount/sub/fight-spawn ceilings; only per-companion `maxChildren` (default 3). Gear tiers still scale from amount. CCI-first — no hardcoded cheer/gift chat parser.

### Companion chunk loading
- Summoned companions **and child Bits** each hold an entity chunk ticket for the chunk they occupy (AI/follow/sleep stay active when the owner walks away).
- NeoForge: `companionChunkLoading` (default **true**) + `maxForcedChunksPerPlayer` (default **16**) under `[performance]` in `azscompanions-server.toml`. Fabric mirrors the same defaults.
- Ticket updates on chunk move; released on despawn / death / charm store. Only while the owner is in the same dimension (or offline). Not an FTB claim.

### Companion AI
- **Server-loaded LLM:** `serverLlmOnly` (default true) — configure provider once on the dedicated/LAN host; all companions share that **endpoint**. Clients need no local LM Studio or API keys. Ask / name-mention / idle / CCI AI run server-side.
- **Separate minds:** `perCompanionMemory` (default true) + `memoryMaxMessages` (default 16) — each companion UUID keeps its own chat buffer; system prompt uses that companion’s name/form/attitude/child-parent only. Docs: [COMPANION_AI.md](docs/COMPANION_AI.md) (“shared server LLM, separate minds”).
- **Chat listen:** `chatListenMode` = `off` (default) | `player` | `global` — auto LLM replies to chat (ignores `/`, cooldowns, range).
- **Name mention:** `nameListen` (default true) — `Bit, come here` triggers that companion; owner vs stranger modes. Strangers get helpful social play; grief/inventory actions blocked.
- **Censor:** `censorChat` (default true) + optional `censorExtraWords` on AI input/`speakLine`.
- **Idle + call-away:** `idleChat`, `callPlayerWhenAway` (+ interval/distance keys); defaults off.
- **World actions:** `enableAiActions` (default false) — LLM JSON/tool actions: `mine`, `place`/`build`, `craft`, move/modes, play (`run_at_player`, hide/seek, dance…), inventory (`pickup`, `use_item`, `equip`, `drop`, …). Craft/build/mine run via real task queue ticks.
- **Child Bits:** inherit parent form/skin/attitude/team/armor visibility; `childAutonomy` cling/balanced/curious + soft parent leash; less frequent idle; AI tools when enabled; **own** AI memory (not the parent’s).
- Docs: [COMPANION_AI.md](docs/COMPANION_AI.md).

### CCI
- **Full manual:** [docs/CCI.md](docs/CCI.md) (install, all subjects, teamfight, AI, troubleshooting).
- **AI subjects:** `companion_ask` / `ai_ask`, `ai_status`, `ai_chat` / `stream_chat`, **`ai_config`** (session `chatListenMode=` / `enableAiActions=`).
- **Persona:** `companion_persona` (+ summon/modify keys `whoAmI`/`whatAmIDoing`/`howWillIBe`/…); `op=get|clear`; marks initialized → skips first-create onboarding.
- **Play:** `companion_play` / `companion_rush` / `companion_hide_seek` (dance/peekaboo/stop via `mode=`).
- **FTB claim:** `claim_chunk` / `unclaim_chunk` (owner quota; needs Chunks + `ftbChunksAiClaim`).
- **Chunk loading:** `companion_modify` `chunkLoading=true|false` per-companion override (server global must allow tickets).
- **Behavior spacing** (already on modify): `followRadius` / `personalSpace` / `wanderRadius`; **showArmor** unchanged.
- **CCI summon → chat + AI:** greet/wave use LLM when provider enabled (canned fallback); ask/chat-listen/idle share the charm-companion pipeline; ownership stays on the streamer.
- Fabric + NeoForge CCI parity. Examples under `cci-examples/`.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.6+1.21.1.jar` | `azscompanions-fabric-0.3.6+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.6+1.21.1.jar` | `azscompanions-fabric-cci-0.3.6+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress (Java 25 / unobfuscated); no jar shipped. See [MULTI_VERSION.md](docs/MULTI_VERSION.md).

## 0.3.5

Release: [v0.3.5](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.5) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Companion AI (text-only)
- Optional LLM replies via `/azscompanions ask <message>` · `/azscompanions ai status`.
- Providers: `disabled` (default, offline-safe), `local`, `openai_compatible`, `mcp`.
- **Dedicated config files:** Fabric `config/azscompanions-ai.json` · NeoForge `config/azscompanions-ai.toml` (not inside `azscompanions-server.toml`).
- No VoiceMod / TTS — owner chat / `speakLine` only. Docs: [COMPANION_AI.md](docs/COMPANION_AI.md).

### Customize / UI
- **Armor visibility:** Customize → Name tab toggle **Armor: Show/Hide** (equipment stays equipped; render only). Persists per companion (`ShowArmor` NBT / synched data).
- **Donate:** Shift+RMB companion menu has a donate icon button (top-right) opening `https://paypal.me/azturax`.

### CCI
- **`companion_modify`:** `showArmor=true|false` (aliases `show_armor`, `armor_visible`) toggles armor rendering remotely.

### CCI — team fights + Bit children
- **Enable:** `/azscompanions teamfight on|off|status` (ops) or CCI `teamfight_enable` / `teamfight_disable` / `teamfight_toggle`.
- **HUD:** left/right team scoreboard (scores, bits, members, tier table, top bits/kills); `teamfight_scoreboard` show/hide/reset. Synced on login.
- **`companion_spawn_leader`:** subs → hostile team leader (form/name/gear); requires teamfight ON.
- **`companion_spawn_child`:** bits → Bits under leader with tiered gear (100 leather+stick → 1000 netherite); aliases `spawn_child`, `spawn_bit`, …
- **Auto kills:** rival-team companion deaths score the HUD automatically.
- Shared helpers: `spawnChild` / `spawnFightLeader`; cake also calls `spawnChild`.
- Caps: default **3** children/leader (`maxChildrenPerCompanion`); CCI `maxChildren=`/`childCap=` per parent (hard max 64). Docs: [CCI_STREAMING_GUIDE](docs/CCI_STREAMING_GUIDE.md).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.5+1.21.1.jar` | `azscompanions-fabric-0.3.5+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.5+1.21.1.jar` | `azscompanions-fabric-cci-0.3.5+1.21.1.jar` |

## 0.3.4

Release: [v0.3.4](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.4) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fixes
- **Player form armor:** companions render equipped helmet/chest/legs/boots (and elytra) via `HumanoidArmorLayer` / `ElytraLayer` like a normal player.
- **Animal / spider armor:** inventory rejects humanoid plate armor on forms without armor layers; incompatible pieces move to backpack (or drop) on form change / load. **Wolf** can equip wolf armor in the chest slot (renders on the proxy via BODY).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.4+1.21.1.jar` | `azscompanions-fabric-0.3.4+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.4+1.21.1.jar` | `azscompanions-fabric-cci-0.3.4+1.21.1.jar` |

## 0.3.3

Release: [v0.3.3](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.3) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Sleep
- **Nearest bed:** at night (Follow mode), companions path to the closest usable empty bed — no longer lock to a stored home bed.
- **Search radius:** **48** blocks horizontal **and** vertical (was ±4 vertical).
- Occupied beds (block flag or another sleeper) are skipped; Kon-named companions still prefer Kon beds first.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.3+1.21.1.jar` | `azscompanions-fabric-0.3.3+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.3+1.21.1.jar` | `azscompanions-fabric-cci-0.3.3+1.21.1.jar` |

## 0.3.2

Release: [v0.3.2](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.2) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fixes
- **Inventory GUI:** `companion_inventory.png` panel fill is vanilla light gray (no solid black interiors); slot layout unchanged.
- **Nametag height:** form/scale sync refreshes hitbox + name-tag attachment on clients; renderers recompute nametag Y from the **current** form/scale every frame (chicken↔player swaps no longer stick).
- **Mob form animations:** proxy visuals copy walk limb position/speed, attack swing, and aggressive state each frame so animal/hostile forms walk/idle/attack like vanilla.
- **Mob held items:** equipment synced to proxy (zombie/skeleton/armor layers); animals/spider get mainhand/offhand drawn via `ItemInHandRenderer`.

### Customize
- **Form tab:** when an animal/hostile is selected, a **Display name** field sets the companion custom name (same persistence as Name tab). Pecker default name still applies on first recruit.

### CCI
- **`companion_modify`:** edit the owner's currently called/summoned companion (form, skin, name, attitude, team, equipment) without recruiting a new one. Aliases: `modify`, `customize`, `edit`.
- **`companion_turn_evil`:** playful temporary HOSTILE (5–15s, default 10) toward nearby non-owner targets, then restore prior attitude. Aliases: `turn_evil`, `go_evil`, `berserk`.
- Hidden: right-click companion with a **fermented spider eye** → same playful evil burst (~10s). Ownership/charm unchanged.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.2+1.21.1.jar` | `azscompanions-fabric-0.3.2+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.2+1.21.1.jar` | `azscompanions-fabric-cci-0.3.2+1.21.1.jar` |

## 0.3.1

Release: [v0.3.1](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.1) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fixes
- **Form crash:** non-player forms no longer pass `CompanionEntity` into vanilla models that cast to Wolf/Fox/etc. (`ClassCastException` in `CompanionMobFormRenderer`). Forms render via client-only proxy entities + vanilla mob renderers.
- **Upside-down form preview:** animal/hostile Customize Form previews (and world forms) render upright with correct standing pose (no sleeping/sitting).
- **Inventory GUI graphic:** custom `textures/gui/companion_inventory.png` sized for `194×220` layout — armor+shield column, 3×9 storage, 9-slot companion hotbar, gap, player inventory — replacing mismatched `generic_54` fills/blits.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.1+1.21.1.jar` | `azscompanions-fabric-0.3.1+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.1+1.21.1.jar` | `azscompanions-fabric-cci-0.3.1+1.21.1.jar` |

## 0.3.0

Release: [v0.3.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.0) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Companion forms
- **Form system:** companions can look like a player or selected animals/hostiles (`CompanionForm` in NBT + synched data). Customize → **Form** tab: Player / Animals / Hostiles.
- **Animals:** Chicken, Wolf, Cat, Cow, Pig, Sheep, Fox, Rabbit, Bee
- **Hostiles:** Zombie, Skeleton, Spider, Enderman, Husk, Stray (no Creeper)
- Ownership, charm store/recall, Follow/Stay/Wander, inventory, and CCI actions still apply to any form
- **Pecker:** UUID `966ebb69-a63d-4bb2-ac90-ed39d8c64b80` recruits a chicken-form companion named **Pecker** by default

### Attitude / teams (CCI + NBT)
- Persisted **`Attitude`**: `PASSIVE` (defend-owner) or `HOSTILE` (aggro nearby players/mobs except owner/trusted)
- Persisted **`TeamId`**: different teams fight each other; same team allied; never attack owner
- Nametag tint for known team colors (red/blue/…)

### CCI IMC (full parity in NeoForge CCI + Fabric CCI jars)
- Modes/chat: `companion_say` / `greet` / `wave` / `follow` / `sit` / `stay`
- `companion_set_attitude`, `companion_set_team`
- `companion_summon` / `companion_summon_passive` / `companion_summon_hostile` with `form=` / `skin=` / `team=` / `attitude=`
- Equipment: `companion_set_mainhand` / `set_offhand` / `set_armor` / `set_hand` (`mainhand=…;helmet=…;clear`)
- Docs: `docs/CCI_STREAMING_GUIDE.md` + `cci-examples/`

### UI / polish
- Inventory: shield under armor column, 9-slot companion hotbar, gap before player inventory, plain vanilla slots (no colored specialty frames), label alignment
- Customize: **Nametag Show/Hide** toggle (persisted; charm store/recall syncs)
- Removed UI hints that advertised the Kon name easter egg (behavior unchanged)

### Capes / AI
- Mojang player skins also fetch/render that player's cape when present (client cache only)
- **Stay/Sit:** never teleport to owner (home-bed, follow rescue, or special-perk snaps)

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.0+1.21.1.jar` | `azscompanions-fabric-0.3.0+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.0+1.21.1.jar` | `azscompanions-fabric-cci-0.3.0+1.21.1.jar` |

### Build / metadata
- Mod version **0.3.0**; published archives tagged `+1.21.1`

## 0.2.1

Release: [v0.2.1](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.2.1) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### UI
- **Companion inventory layout:** left vertical armor column (helmet→boots) with vanilla empty armor icons; 3×9 storage to the right; companion hotbar/tools on a separated strip still inside the companion panel; player inventory below with normal spacing (no floating equipment row in the gap)

### AI / teleport
- **Happy Ghast–inspired Wander:** slow leisurely strolls (speed 0.55), rare starts, linger pauses between legs, soft looks; roam radius 3–16 (owner) / 2–10 (home bed); if outside radius **walks back** — never teleports for that
- **Wander / home-idle no short-range snaps:** removed home-bed leash teleport at ~8 blocks (`PREFERRED+2`); any teleport-to-owner now requires ≥ **24** blocks (`MIN_TELEPORT_DISTANCE`)
- **Wander mode teleports:** zero FollowGoal / stuck / ground-leash teleports — only the home-bed rule (owner >35 from bed) may teleport, and only if also ≥24 from owner
- Special perk land-snap no longer teleports when floating while already close (<24)

### Loaders / editions (jar matrix)
Jar names use `0.2.1+<minecraft>` so the game version is visible in the filename.

| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.2.1+1.21.1.jar` | `azscompanions-fabric-0.2.1+1.21.1.jar` | `azscompanions-neoforge-cci-0.2.1+1.21.1.jar` | `azscompanions-fabric-cci-0.2.1+1.21.1.jar` |

**Shipped this release:** four jars for Minecraft **1.21.1** (NeoForge + Fabric standalone and CCI). See `docs/MULTI_VERSION.md`.

### Build / metadata
- Mod version **0.2.1**; published archives tagged `+1.21.1`

## 0.2.0

Release: [v0.2.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.2.0) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Gameplay
- **Kon-only gating:** sleep purr (`CAT_PURR`), Kon Bed sleep priority, Kon skin easter egg + one-time Kon Bed grant apply only when the companion display name equals `Kon` (case-insensitive). Non-Kon companions use generic sleep/bed/skin defaults. UUID special perks (ears, fly/glow, Wiggly) unchanged.

### Loaders / editions (jar matrix)
Jar names use `0.2.0+<minecraft>` so the game version is visible in the filename.

| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.2.0+1.21.1.jar` | `azscompanions-fabric-0.2.0+1.21.1.jar` | `azscompanions-neoforge-cci-0.2.0+1.21.1.jar` | `azscompanions-fabric-cci-0.2.0+1.21.1.jar` |
| **1.20.1** | *not shipped — no NeoForge 20.1 on releases Maven; Forge port deferred* | *deferred — API backport* | *CCI: no NeoForge build (Forge-only on Modrinth)* | *CCI available (see table) but Az port deferred* |
| **26.1.2** | *deferred — unobfuscated MC + Java 25 rewrite* | *deferred* | *CCI: none on Modrinth* | *CCI: none on Modrinth* |
| **26.2** | *deferred — unobfuscated MC + Java 25 rewrite* | *deferred* | *CCI: none on Modrinth* | *CCI: none on Modrinth* |

### CCI / iChunUtil dependency pins (Modrinth lookup)

| Minecraft | Loader | CCI | Modrinth id | iChunUtil | Modrinth id | Used in 0.2.0? |
|-----------|--------|-----|-------------|-----------|-------------|----------------|
| **1.21.1** | NeoForge | **1.13.0** | `AySbAgcO` | **1.0.3** | `OvIyyNh4` | Yes (`neoforge-cci`) |
| **1.21.1** | Fabric | **1.13.0** | `PERd6IT9` | **1.0.3** | `gfAOoiwe` | Yes (`fabric-cci`) |
| **1.20.1** | Fabric | **1.13.0** | `7tk12xkN` | **1.0.3** | `JjEWQx5u` | No (port deferred) |
| **1.20.1** | Forge | **1.13.0** | `nNaAlKHI` | **1.0.3** | `W6d0pCyu` | No (no NeoForge CCI; Forge module deferred) |
| **1.20.1** | NeoForge | — | none | — | none | N/A |
| **26.1.2** | any | — | none | — | none | N/A — ship standalone only when ported |
| **26.2** | any | — | none | — | none | N/A — ship standalone only when ported |

**Shipped this release:** four jars for Minecraft **1.21.1** (NeoForge + Fabric standalone and CCI). See `docs/MULTI_VERSION.md`.

### Build / metadata
- Mod version **0.2.0**; published archives tagged `+1.21.1`
- NeoForge / Minecraft dependency ranges tightened to **1.21.1 / NeoForge 21.1.x** (no longer advertise untested 26.x binary compat)

## 0.1.1

Release: [v0.1.1](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.1) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fixes
- **Dedicated NeoForge server:** `RegisterPayloadHandlersEvent` no longer classloads client GUI (`Screen`) via `OpenCompanionCreatorPacket` / `OpenCompanionMenuPacket` / `CompanionDialoguePacket` handlers — S2C handlers live in `ClientNetworkHandlers` (client dist only)

### UX / commands
- Shift+RMB **Companion Menu** (shared on NeoForge + Fabric): Customize · **Command** (Follow / Stay / Wander) · Inventory — server packets only
- Removed V-key **radial** command UI (and related menus/packets/keybinds); Command lives in the companion menu
- Clearer Follow / Stay / Wander AI (no idle free-roam that ignores commands)
- Ownership denial message for non-owners; inventory / command / customize stay owner-gated
- Inventory: backpack + distinct armor/tool equipment strip
- Removed unused Kon card/portrait GUI textures (mod icon uses companion charm)
- Feed with edible food: consumes 1, small heal, hearts + cheer (not placed in hands)
- Sleeping companions softly purr (`CAT_PURR`) every ~5s server-side (no sleep-skin texture override)
- Charm Hello / Bye lines toggleable via NeoForge config
- Scaled companions step up **1 full block** at any body size (`STEP_HEIGHT` 1.0 + `JUMP_STRENGTH` 0.42)
- Fabric customize / creator parity improvements (appearance draft, skin lookup, shared menu screens)

### Home bed / follow
- **Home-bed proximity (35 blocks, configurable on NeoForge):** near bed → home-idle; owner farther than 35 from bed → teleport + follow. Stay ignores the auto rule; Wander strolls near the bed until the owner leaves radius
- Night sleep **prefers Kon Bed**, then home bed; leave bed if owner moves far (~35) with wake cooldown to avoid thrashing
- Follow bands: personal space **2**, preferred **~6**, start **10**, stop **5**, teleport **48**; home-bed radius **35**
- Owner **explore vs idle** still used for soft stroll when no home bed is set

### Loaders / editions
- Four jars for Minecraft 1.21.1: NeoForge + Fabric **standalone**, NeoForge + Fabric **CCI** (same dependency story as 0.1.0)

## 0.1.0 — first public

Release: [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Breaking
- Mod id / namespace renamed from `koncompanions` → `azscompanions` (display name **Az's Companions**). Old worlds with `koncompanions:*` items/entities will not load those content ids.

### Gameplay
- Companion Charm (desert pyramid loot): summon / store; **one companion per player**
- First summon defaults to player name + player skin
- Follow by day; night sleep in nearest bed (any bed / Kon Bed)
- Owner **explore vs idle**: exploring → loose follow; standing still (~2.5s) → free wander **24–40**, **no teleport / no approach**
- Loose follow bands: personal space **2**, comfort stroll **2–12**, preferred **~6**, start **32**, stop **8**, teleport **48** (exploring only, never while fighting)
- Environmental hazard immunity: fall, cactus, sweet berry bush, drowning, in-wall, campfire (still take combat damage)
- **Combat:** defend living attackers of the owner (ignores environmental damage); SIT/sleep suppress combat targeting
- **Hands:** give items into main → offhand (swap if both full); empty-hand take
- **Potions:** ground auto-pickup **beneficial only** (skip harmful + water/awkward neutrals); manually given harmful splash thrown at enemies
- Customize (NeoForge): name, gender, Mojang skin, size/proportions; **Done** saves, Cancel discards
- Typing a valid Minecraft username fetches that player's Mojang skin (live preview + entity sync); **Kon** applies Kon special skin + one-time Kon Bed
- Skin lookup waits for texture download + legacy 64×32→64×64 processing before applying `player:<uuid>`
- Skins are Mojang-only (no local PNG import)
- Charm store/recall persists appearance (name, skin, gender, size, proportions, home bed, etc.)
- Charm Hello / Bye chat lines (owner only)
- UUID-gated **special player perks** (flight follow / glow / Kon ears) for designated owners
- Kon ears cosmetic on UUID `42901453-b2b5-4d95-9b7b-e0ed40da504f` (client render layer; meow nametag removed)
- SIT/STAY (CCI modes) suppress wander/follow movement

### Loaders / editions
- Four jars for Minecraft 1.21.1: NeoForge + Fabric **standalone**, NeoForge + Fabric **CCI**
- **NeoForge CCI** (`azscompanions-neoforge-cci`): hard-depends on CCI **1.13.0** + iChunUtil **1.0.3**; IMC bridge for `companion_say` / `greet` / `wave` / `follow` / `sit` / `stay`
- **Fabric CCI** (`azscompanions-fabric-cci`): same subjects via IMCOutcome mixin (iChunUtil Fabric has no InterModComms) plus `/azscci` CommandOutcome fallback
- Standalone jars unchanged (no CCI required); never install standalone + CCI together

### Deferred / foundation
- **Baritone** pathfinding not bundled this pass — prefer future home beacon + owned deposit box foundation
- Auto-equip tools config exists (default **off**); full auto-equip logic deferred
- Sharing companions with other players deferred (ownership UUID hooks left extensible)
