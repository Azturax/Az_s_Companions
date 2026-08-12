# Changelog

## Unreleased

## 1.0.0

First stable product release for Minecraft **1.21.1** (NeoForge + Fabric, standalone + CCI).

### Product
- **Companions:** Charm summon/store, Follow / Stay / Sit / Wander, Customize (forms + coat variants, proportions, activity skins), inventory, night sleep, Kon Bed, child Bits
- **Worldplay:** desert-pyramid Companion Charm loot (`world.enableLoot`), flower gifts, logout park / login restore, dimension follow (vanilla + modded), swim follow, ride-along mounts, wander mob play, cat/wolf scare, chunk tickets
- **Combat / perks:** fixed netherite-sword melee, UUID Wolfy / Wiggly (+ survival flight) perks, team fights (CCI / `/az teamfight`)
- **Optional AI:** `/ask` · `/az ask`, ambient + reactive chatter, persona onboarding, admin LLM profiles + join consent — [COMPANION_AI.md](docs/COMPANION_AI.md)
- **CCI editions:** streamer-chat companions when CCI + iChunUtil are installed — [CCI.md](docs/CCI.md)
- **Soft-compat:** FTB, map icons, dynamic lights, Simple Voice Chat detect — [COMPAT.md](docs/COMPAT.md)

### Changed (from 0.4.7)
- **Docs / copy:** README and version docs describe **1.0.0**; teamfight hint prefers primary `/az teamfight on` (alias `/azscompanions` still works)
- **Housekeeping:** unfinished shared-hooks draft moved to `docs/dev/` (live wiring remains in loader event classes)

### Not shipped
- NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar
- Removed earlier experiments stay gone: Jindujun / Flying Nimbus / whistle, Glowing Orb form, flight ki aura

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-1.0.0+1.21.1.jar` | `azscompanions-fabric-1.0.0+1.21.1.jar` | `azscompanions-neoforge-cci-1.0.0+1.21.1.jar` | `azscompanions-fabric-cci-1.0.0+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.7

### Removed
- **Jindujun / Flying Nimbus** — rideable cloud entity, Blockbench mesh/renderer, enchant particle stream, textures, and all registration hooks (NeoForge, Fabric, NeoForge 26.2).
- **Jindujun Whistle** — summon/dismiss item, creative-tab entry, Trail Ruins archaeology loot injection (Fabric pool + NeoForge taiga GLM), lang strings, and related tests.

### Changed
- **Treasure loot:** `world.enableLoot` now only gates Companion Charm desert-pyramid injection (whistle loot gone).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.7+1.21.1.jar` | `azscompanions-fabric-0.4.7+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.7+1.21.1.jar` | `azscompanions-fabric-cci-0.4.7+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.6

### Fixed
- **Jindujun sit gap (again):** remaining air above the cream deck — `RIDER_Y_OFFSET` lowered from `0.32×SCALE` (~0.80) to `0.22×SCALE` (~0.55) so feet sit slightly into the top fluff.
- **Jindujun violent spin:** yaw sync no longer overwrites previous-tick rot (`yRotO` / body / head) or re-applies in both `travel` and `tick`. Steering applies rider look yaw once per travel tick (normalized), keeping client lerp stable.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.6+1.21.1.jar` | `azscompanions-fabric-0.4.6+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.6+1.21.1.jar` | `azscompanions-fabric-cci-0.4.6+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.5

### Fixed
- **Jindujun sit gap:** 2.5× render scaled *after* the Blockbench `-1.501` feet pivot, sinking the mesh ~2.25 blocks under the hitbox so the rider floated above the cloud. Scale now wraps the pivot; `RIDER_Y_OFFSET` lowered from `HEIGHT×0.88` (~1.21) to `0.32×SCALE` (~0.80) so the player sits flush on the deck.
- **Jindujun enchant stream:** denser / taller glyph cloud reduced — compact pixels, stream further behind at foot height, far fewer particles per tick, and enchant velocity no longer lifts glyphs into the rider. Spawn origin remains the nimbus entity only.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.5+1.21.1.jar` | `azscompanions-fabric-0.4.5+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.5+1.21.1.jar` | `azscompanions-fabric-cci-0.4.5+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.4

### Changed
- **Join-time LLM consent remembered:** Yes/No on the “use server LLM?” (or local-probe) prompt is saved per server key in client `config/azscompanions-ai-join-consent.json`, so the screen asks **at most once**. Host **Use server LLM** (`serverLlmOnly`) still lives in AI config and is changeable later only via `/az admin` → AI Config (no re-prompt on every join / JVM restart).
- **Jindujun 2.5× size:** hitbox, shadow, Blockbench mesh render scale, and enchant stream offsets all use `JindujunSupport.SCALE` (2.5).
- **Jindujun turns with rider:** while mounted, cloud `yRot` / body / head sync to the controlling player look yaw each tick (no locked world-axis facing).
- **Jindujun sit offset:** passenger rides on the cloud top (`RIDER_Y_OFFSET` ≈ 88% of scaled height).
- **Jindujun idle dismiss:** if left unridden for **56s** continuously, the cloud discards itself (timer resets on mount / ride / right-click mount; persisted in NBT).
- **ITEM_FIND reactive chatter rarer:** "nice find" / notable-item reactions now use a **~14-day real-time** per-owner cooldown (`System.currentTimeMillis`, also ~24 192 000 ticks at 20 TPS). Explosion / darkness / craft paths unchanged.

### Fixed
- **Enchant trail only on Jindujun:** shaped `ENCHANT` stream spawns only from the nimbus entity tick at cloud/foot height — not on the player passenger, companions, or Bits (flight-aura leftovers already removed).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.4+1.21.1.jar` | `azscompanions-fabric-0.4.4+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.4+1.21.1.jar` | `azscompanions-fabric-cci-0.4.4+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.3

### Added
- **No natural despawn for owned companions / Bits:** owned companions and child Bits get vanilla persistence (`setPersistenceRequired`) plus scoreboard tag `azscompanions.nodespawn` on create/own/load (recruit, Bit spawn, charm summon, logout restore). Intentional discard (logout park, charm store, kill) unchanged. Fabric + NeoForge (+26.2).

### Changed
- **Jindujun visuals:** Desktop Blockbench cloud mesh + texture (`Jindujun.java` / `Jindujun.png`) replaces the billboard cloud. While ridden, only the nimbus shows a **shaped `ParticleTypes.ENCHANT` stream** (plugin-style cross/ladder silhouette behind the cloud at foot height — not in first-person face).
- **Jindujun texture:** classic bright yellow / soft-gold fluffy Nimbus paint over the existing UV layout (replaces Blockbench face-debug colors).

### Removed
- **Flight ki aura bubbles and motion trails** for players / companions / Bits (and the old nimbus aura trail). Creative/elytra flight no longer draws the soft shell or afterimages.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.3+1.21.1.jar` | `azscompanions-fabric-0.4.3+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.3+1.21.1.jar` | `azscompanions-fabric-cci-0.4.3+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.2

### Added
- **`world.enableLoot` config** (default **true**): master switch for mod treasure loot injections (Companion Charm in desert pyramids, Jindujun Whistle in Trail Ruins archaeology). Set **false** to disable all of them. NeoForge: `config/azscompanions-common.toml` → `[world] enableLoot`; Fabric: `config/azscompanions-common.json` → `world.enableLoot`.
- **Reactive AI chatter (recent actions):** owned companions (Idle chat ON, in range) react to short-lived nearby events — TNT/explosions, entering darkness (asks for a torch), notable item finds, last ingredient toward watched crafts (swords/tools/armor), and crafting gear (e.g. **NICE SWORD!**). LLM prompt includes event context; scripted fallbacks when LLM is off. Rate-limited; Fabric + NeoForge (+26.2) hooks.
- **Ride-along mounts:** when you mount a horse/camel/llama, boat, minecart, pig, or strider, following companions try to tame (if needed) and mount the nearest empty matching rideable, then keep pace nearby. They never take your vehicle or another player's pet. If nothing empty is nearby they briefly approach a candidate, then cool down. They dismount when you do.
- **Cat-form creeper scare:** companions in **Cat** form scare creepers like vanilla cats (creepers flee within ~6 blocks). Player and other forms do not.
- **Wolf-form skeleton scare:** companions in **Wolf** (dog) form scare skeletons the same way — Skeleton, Stray, Wither Skeleton, and Bogged flee within ~6 blocks. Other forms do not.
- **Wander mob play:** in **Wander** mode only, companions occasionally circle, sneak around, nudge/push, or give a light punch (knockback; tiny damage only if combat is allowed) to nearby passive/hostile mobs. Rate-limited; skips players, owner pets, bosses, and protected entities. Follow/Stay/Sit unchanged.
- **Sit pose (minecart-like):** Command **Sit** now applies a visible passenger / bent-leg sit pose for **Player**, **Zombie**, **Skeleton**, **Husk**, **Stray**, and **Enderman** forms (same as riding a minecart). Wolf / Cat / Fox keep their native sit. Other animals and spider still hold still without a sit mesh. Stay remains upright hold-still.
- **Mob form variants:** Customization → Form shows small `<` / `>` buttons beside Wolf, Cat, Fox, Rabbit, and Sheep to cycle coats/breeds/types/wool. Persisted as synched `CompanionFormVariant` NBT; live preview. Player and other forms have no arrows.
- **Logout park / login restore:** owned companions despawn when the owner disconnects and respawn near them on join. Snapshots go to player persistent data (NeoForge) / overworld SavedData (Fabric), and the bound Companion Charm mirrors with `LogoutParked` so charm state stays consistent. Manual charm store is unchanged (no auto-summon). Children fold into parent `StoredChildren` before parking.
- **Flower gifts:** right-click your companion with any `#minecraft:flowers` item (poppies, tulips, torchflower, tall flowers, etc.) to gift one — hearts appear, then they **throw** a **context-weighted** return gift as an item entity toward you (mild arc; short pickup delay). Quiet moments lean on classic flowers; darkness/night → torch/lantern, low hunger → food, sleeping/bathing/adventuring, recent combat/craft/find, biome, and attitude also bias the pool. The tossed stack is newly created (never pulled from task inventory). Empty-hand right-click remains a fallback if a pending offer could not be thrown. ~3s cooldown. Owner (Fabric) / owner or trusted (NeoForge).
- **Flight aura + Jindujun Whistle:** soft **ki aura** + foot-level motion trails on flying players (creative/survival flight, elytra); no rising particle columns into first-person view. **Jindujun Whistle** summons a rideable **Flying Nimbus** cloud (steer WASD + jump/sneak). Creative tab + Trail Ruins archaeology loot (chance below).

### Fixed
- **Companions follow into every dimension:** on any owner dimension change (vanilla Nether/End **and** modded dims via registry key — no mod-id allowlist), owned companions **teleport with the player** (`Entity.teleportTo` / `DimensionTransition`). Not logout park/respawn — form, skin, persona, and proportions stay continuous for the world save. First-create persona UI only on new companion creation. Logout park remains disconnect-only.

### Changed
- **Companion AI snappier `/ask`:** up to **2 parallel** LLM calls (`maxParallelRequests`, 1–4) so ask is not stuck behind idle ambient; interactive prompts jump the queue ahead of `[ambient]`/`[react]`/`[call]`. New `connectTimeoutSeconds` default **8** (fail dead endpoints faster; full `timeoutSeconds` still **30** for slow local models). Ambient completions capped at **128** tokens. Defaults: idle interval **75–180s** (was 90–240), chat-react cooldown **12s** (was 20), `memoryMaxMessages` **12** (was 16). See [COMPANION_AI.md](docs/COMPANION_AI.md).
- **Behavior radii:** Wander radius is always **≥ follow radius** (raising follow bumps wander; wander cannot be set below follow). Wander max raised **48 → 128** (same as follow). Defaults: follow **48**, wander **48** (was wander 16). Persists via existing NBT / Behavior screen / CCI.
- **Treasure loot rarer + small finds:** Companion Charm desert-pyramid chest chance **100% → 5%** (1 charm when it hits). Jindujun Whistle Trail Ruins archaeology chance **2% → 0.5%** (taiga GLM on NeoForge). Mod treasure appends **1** unique item per successful roll (within a 1–3 item policy; no multi-roll stacks).
- **Companion melee damage:** fixed to vanilla netherite sword Attack Damage (**8**). Ignores Bit gear tiers / held tool material (no more base-4 + weapon modifier stacking).
- **Special perk UUID mapping:** **Wolfy** (`7c97e337-2c49-448c-b710-7655487f18df`) brown wolf grant **only**; special flight UUID (`4274c47f-d61f-4850-bf29-9e5c185db4ac`) gets survival flight + flying companion + toggleable **Wiggly** (H / `/az wiggly`) with **auto-glowing removed**. 0.4.0 had wrongly attached Wiggly to the Wolfy UUID.

### Removed
- **Mob-form held-item rendering:** companions in non-player forms no longer draw mainhand/offhand items (animal overlays or humanoid `ItemInHandLayer` on zombie/skeleton/husk/stray/enderman proxies). Player form hand items unchanged. Armor on mob forms still syncs when visible.
- **Glowing Orb** companion form and all related settings/UI/render/particles/dynamic-light hooks, evil-mode orb lightning, and CCI `glowing_orb`/`orb` form aliases. Old saves with that form migrate to the default **Player** form on load.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.2+1.21.1.jar` | `azscompanions-fabric-0.4.2+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.2+1.21.1.jar` | `azscompanions-fabric-cci-0.4.2+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.1

Draft-only (never published). UUID perk fix, orb polish, flight aura, and Jindujun from that draft are superseded by **0.4.2** (orbs removed; other items shipped there).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.1+1.21.1.jar` | `azscompanions-fabric-0.4.1+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.1+1.21.1.jar` | `azscompanions-fabric-cci-0.4.1+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.4.0

### Glowing Orb form
- New selectable form **Glowing Orb** (Customization → Form → Special / `glowing_orb`): ~0.5×0.5 floating companion — no player/mob mesh, no humanoid/wolf armor.
- Always-air follow with personal-space ring + owner-local **Offset X/Y/Z**, float height/bob/speed, RGB color, and **Brightness** (0–15) for dynamic-lights soft-compat.
- Children of orb parents inherit orb form + orb settings (cake / CCI / spawnChild).
- Render: Sodium + Iris/Oculus-safe **cutout core + eyes emissive** (no translucent-only path). Docs: [COMPAT.md](docs/COMPAT.md).

### Air personal space + wander flight
- Flight follow holds the same personal-space ring as ground/swim (default stand-off **~3.5** blocks via `preferredDistance(personalSpace)`; default personal space **2**) instead of bee-lining into the owner.
- Far flight snaps use the follow leash (default **48**, floor **12**), never under personal space. Applies to special-perk companions and toggle Wiggly dog flight.
- **Wander** while the owner is flying: leisurely air roam inside the wander radius, outside personal space; lands when the owner stops flying (no permanent hover).

### Toggleable Wiggly dog
- Pink-collar dog named **Wiggly** with toggle keybind **H** / `/az wiggly` (UUID mapping corrected in **0.4.1** — see above).
- Behavior: ground-follows when you walk; floats beside you only while you are flying / elytra (same rule as special companions — no permanent flight).
- Playful sit/stand wiggle on the ground; light bob while floating. Visibility persists via `azscompanions.wiggly_dog_hidden`.

### Wolfy UUID perk
- UUID `7c97e337-2c49-448c-b710-7655487f18df` receives a one-time **wolf-form** companion named **Wolfy** (brown / `minecraft:chestnut` coat) on join/perk apply.
- Idempotent: after grant, player tag/NBT `azscompanions.wolfy_granted` (Fabric/NeoForge 1.21.1 scoreboard tag; NeoForge 26.2 persistent data); skips if Wolfy already exists in-world or stored in a Companion Charm.
- Shared helpers in common (`WolfyPerkSupport`); loader hooks via existing `SpecialPlayerPerks.applyPlayerPerks`. Charm recruit for that UUID also defaults to Wolfy.

### Companion swim-with-player
- Follow mode keeps companions with you in water: direct swim steering when both are wet, goal stays active while the owner is in water.
- Shared helpers (`CompanionSwimFollowSupport`) + Fabric / NeoForge follow-goal wiring.

### CCI user-facing messages
- CCI outcomes (greet/wave/say, modes, summon/modify, persona, play, AI status/ask, teamfight, interaction Bit spawn, dismiss) now use **lang keys** (`toast.azscompanions.cci.*` / `message.azscompanions.cci.*` / dialogue keys) instead of hardcoded English.
- Feedback via CCI informational toast when available, plus action-bar fallback (`Title — body`).
- Fabric `/azscci` shows usage on bare invoke and a chat deny for unknown subjects.
- Shared helper: `CciMessages` in common. Docs: [CCI.md](docs/CCI.md).

### Command menu icons + Sit + keybind
- Command menu shows icon buttons for **Follow**, **Stay**, **Sit**, and **Wander** (name + short description in tooltip). Close with **ESC** (no Back button).
- **Sit** is a separate mode from Stay (sitting pose / hold still).
- Keybind **Open Command Menu** (default **K**) under Options → Controls → **Az's Companions** — targets look-at companion, else nearest owned/trusted within 32 blocks. Still available via charm + Shift+RMB → Command.

### Simple Voice Chat soft-compat (plus VoiceMod detect)
- Soft-detect **Simple Voice Chat** (`voicechat` / `voicechat_api`) alongside optional `voicemod` awareness — no hard jar dependency.
- Reference pin for NeoForge 1.21.1: **`voicechat-neoforge-1.21.1-2.6.21.jar`** (2.6.21); Fabric equivalent documented.
- Optional soft-deps in NeoForge `neoforge.mods.toml` + Fabric `suggests`; bootstrap logs when present; Voicechat API class probed via reflection (entity audio emission not wired yet).
- VoiceMod desktop TTS bridge remains **not shipped** (text dialogue only).
- Docs: [COMPAT.md](docs/COMPAT.md).

### Activity / context skins (player form only)
- Companion Customization top tab **Activity**: Sleeping / Bathing / Adventuring outfits from **local** (`config/azscompanions/skins/`) or **URL**.
- **Player form only** — mob forms keep form rendering; settings still save. Priority: context outfit → custom skin → base/default.
- Sleeping = in bed; bathing = in water; adventuring = owner exploring. NBT + synced for multiplayer.
- Docs: [CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md).

### Server LLM optional (opt-in)
- **`serverLlmOnly` default OFF** (admin **Use server LLM**). Personal local/remote LLM on SP/integrated is the default path; shared host endpoint is explicit opt-in.
- **`integratedMultiplayerSharedLlm` default OFF** — friends joining no longer silently force shared-host status.
- Join prompt is optional: **Yes** on a local probe enables your LLM without turning Use server LLM on; **Yes** on a server offer opts hosts into shared mode; **No** skips and leaves personal AI Config available.
- Dedicated: `/ask` still runs on the server process (no per-joiner client LLM); Use server LLM is no longer forced on by dedicated alone.
- Docs: [COMPANION_AI.md](docs/COMPANION_AI.md), [ADMIN.md](docs/ADMIN.md), [COMPAT.md](docs/COMPAT.md).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.4.0+1.21.1.jar` | `azscompanions-fabric-0.4.0+1.21.1.jar` | `azscompanions-neoforge-cci-0.4.0+1.21.1.jar` | `azscompanions-fabric-cci-0.4.0+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.17

### `/ask` empty reply UX (Gemma / LiteLLM)
- Root cause: Gemma 4 often returns HTTP 200 with empty `message.content` (thinking budget / `reasoning_content`). Idle chat still “spoke” via **scripted fallback**; `/ask` dumped the exception **including truncated JSON** into chat (looked like `"role"` / `"content"` lines).
- Parser falls back to `reasoning_content` / `reasoning` / `thinking` when content is null/blank.
- Gemma-like model ids send `think:false` + `reasoning_effort:none` and bump request `max_tokens` to at least 512.
- Player-facing `/ask` errors are **short** (no raw JSON body). Full body + diagnosis go to the **server log** only.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.17+1.21.1.jar` | `azscompanions-fabric-0.3.17+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.17+1.21.1.jar` | `azscompanions-fabric-cci-0.3.17+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.16

### Empty LLM reply / talk path
- OpenAI-compatible parser now reads **array** `message.content` parts and `refusal` text (no longer drops valid replies as empty).
- HTTP 200 with no assistant text becomes a clear **Companion AI error** (includes truncated body + model/LiteLLM hint) instead of a vague empty-reply-only path.
- `/ask` strips leftover action fences before `speakLine` so dialogue stays visible owner chat.

### Ambient idle chat (speech only)
- **`idleChat` default ON** (new installs / NeoForge TOML default). Admin AI Config toggle **Idle chat: ON/OFF**.
- Prefers LLM ambient prompts when the server provider is enabled; on empty/error or when AI is disabled, uses sparse scripted fallback lines.
- Skips sleep, combat, busy LLM worker, and ~45s after any recent speak line. Interval still `idleChatSecondsMin`/`Max` (default 75–180s).
- Docs: [COMPANION_AI.md](docs/COMPANION_AI.md), [ADMIN.md](docs/ADMIN.md).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.16+1.21.1.jar` | `azscompanions-fabric-0.3.16+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.16+1.21.1.jar` | `azscompanions-fabric-cci-0.3.16+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.15

### Admin AI Config — Use server LLM + API key
- **Use server LLM: ON/OFF** toggle in `/az admin` → AI Config (maps to `serverLlmOnly`; default ON). Host LLM endpoint is authoritative for companions; joining clients do not need their own provider or keys. Dedicated servers always use the server endpoint.
- Masked **`apiKey`** field: status only over S2C (`config` / `env` / not set); blank keeps current; **Clear** clears the config key. **Save & apply** writes the AI file and hot-applies to the live LLM runtime (no restart for these fields). Non-blank `apiKey` still wins over `apiKeyEnv`.
- Docs: [ADMIN.md](docs/ADMIN.md), [COMPANION_AI.md](docs/COMPANION_AI.md).

### Join-time LLM consent
- On world/server join, if the **server has AI configured** (S2C offer) or (integrated/SP) a **local LLM** answers a short TCP probe on LiteLLM `:4000` / Ollama `:11434` / LM Studio `:1234`, the client shows a yes/no prompt: **Use the server LLM?**
- **Yes** — remembers accept for this server key for the client session; hosts/admins enable **Use server LLM** and may apply a local LiteLLM/Ollama/LM Studio profile when AI was disabled; tip points to `/ask`.
- **No** — dismisses for this server key (no re-prompt until reconnect to a different key / new JVM); does not enable AI; never auto-connects without consent.
- Dedicated servers only offer when the server AI config is already enabled (no client-side remote probe).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.15+1.21.1.jar` | `azscompanions-fabric-0.3.15+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.15+1.21.1.jar` | `azscompanions-fabric-cci-0.3.15+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.14

Release: [v0.3.14](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.14) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### LiteLLM / openai_compatible API key
- `/ask` no longer fails with **Missing LLM API key** solely because `apiKey` / `AZS_LLM_API_KEY` is empty. Open local proxies (LiteLLM without `master_key`, etc.) work without a Bearer header.
- Status still shows `(no API key)` when none is resolved (informational). Secured APIs return HTTP 401 from the proxy if a key is required.
- Admin AI save still does **not** touch `apiKey` (env name only); set the key via server env or config file.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.14+1.21.1.jar` | `azscompanions-fabric-0.3.14+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.14+1.21.1.jar` | `azscompanions-fabric-cci-0.3.14+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.13

Release: [v0.3.13](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.13) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Commands menu
- Removed **Gather…**, Gather status, Cancel gather, **Deposit chests…**, Deposit done, and Clear deposit from the companion **Commands** screen (Fabric + NeoForge 1.21.1). NeoForge 26.2 already had movement-only.
- `/az gather`, `/deposit`, and CCI gather/deposit remain available.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.13+1.21.1.jar` | `azscompanions-fabric-0.3.13+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.13+1.21.1.jar` | `azscompanions-fabric-cci-0.3.13+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.12

Release: [v0.3.12](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.12) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Admin AI Config — editable profiles
- Selecting any Profile (LM Studio, Ollama, OpenRouter, OpenAI, Groq, LiteLLM, MCP, Disabled, Custom…) still **fills defaults**, then **all fields stay editable**: `provider`, `baseUrl`, `model`, `apiKeyEnv`, `inputLanguage`, `mcpUrl`, `serverLlmOnly`.
- Tweaking `provider` / `baseUrl` / `mcpUrl` away from a preset switches the label to **Custom...**.
- Save still writes `azscompanions-ai.json` / `.toml` + restart message (Fabric + NeoForge + NeoForge 26.2 admin screens).

### Ask-only AI chat
- Removed **chatListenMode** auto chat react, **nameListen** name-mention wiring, and **enableAiActions** LLM world tools from runtime + admin UI.
- Companions reply via **`/ask`** / **`/az ask`** only (plus CCI `companion_ask`). Kept **`serverLlmOnly`**.
- Legacy config / CCI session keys for those three flags are **ignored**; admin toggles removed.

### Dynamic lighting soft-compat
- Optional soft-compat with LambDynamicLights / RyoamicLights / similar (`compat/dynamiclights`) — companions expose held torch/lantern light via LivingEntity hand slots; optional legacy API registration when those mods are present.
- NeoForge client `[dynamicLights]`; Fabric `config/azscompanions-dynamiclights.json`. Docs: [COMPAT.md](docs/COMPAT.md).

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.12+1.21.1.jar` | `azscompanions-fabric-0.3.12+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.12+1.21.1.jar` | `azscompanions-fabric-cci-0.3.12+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

## 0.3.10

Release: [v0.3.10](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.10) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Remove AI Mode completely
- Shift+RMB menu no longer shows **AI Mode: ON/OFF** (button, tooltip, and cancel-row layout from 0.3.8/0.3.9).
- Removed per-companion `AiPlayMode` synced data / NBT, `TOGGLE_AI_MODE` menu packet, CCI `aiMode=` / `aiPlayMode=` / `llmPlay=`, and related lang keys.
- LLM remains **text chat only** via `/ask`, name listen, and idle/call-away when the server provider is enabled — no LLM world puppeting or goal pausing.
- Use Behavior gather/deposit (and other tasks) for material work instead.

### Companion menu + charm
- Menu opens only with **charm in hand + Shift + right-click** on the companion (empty hand / other items no longer open the menu).
- Companions can never hold or store the Companion Charm (inventory/equipment blocked; any charm present is ejected).

### Gather + deposit
- `/az gather <item> <count> [chest|look|nearest]` + status/cancel; dynamic item + craft recipe catalogs on server start.
- `/deposit` multi-select chests (RMB toggle, Esc/`done` exit; highlights only while mode on). Gather uses nearest of selected chests.
- Commands menu scroll list: Gather… / Deposit / status / cancel — **draggable scrollbar** (also persona + customize).
- Tool swap, off-hand torch, place torch when dark (NeoForge); ask owner + craft missing tools when possible.

Docs: [GATHER.md](docs/GATHER.md).

### LiteLLM / proxy auth
- MCP HTTP client sends `Authorization: Bearer <key>` on **every** request (including `POST /mcp/`), using the same `apiKey` / `AZS_LLM_API_KEY` resolution as chat. Fixes LiteLLM `401` / “Malformed API Key… Ensure Key has `Bearer ` prefix.”
- OpenAI-compatible + MCP clients normalize Bearer (no double-prefix if the env/config value already includes `Bearer `).
- Admin AI profile **LiteLLM** preset: `openai_compatible` @ `http://127.0.0.1:4000/v1` (also seeds `mcp.url` to `http://127.0.0.1:4000/mcp/`). Config alias `litellm`.
- Hardening: empty baseUrl / missing remote API key → clear `/ask` errors; connection refused / timeouts / malformed JSON caught on the AI worker (no server-tick crash).

Docs: [COMPANION_AI.md](docs/COMPANION_AI.md).

### Dynamic lights (soft-compat)
- Optional soft-compat with LambDynamicLights / RyoamicLights / similar: detect present mods and register companion entity light handlers when APIs allow.
- NeoForge client config `[dynamicLights]`; Fabric `config/azscompanions-dynamiclights.json`. Bundled LDL entity JSON for companions.

### Loaders
| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.3.10+1.21.1.jar` | `azscompanions-fabric-0.3.10+1.21.1.jar` | `azscompanions-neoforge-cci-0.3.10+1.21.1.jar` | `azscompanions-fabric-cci-0.3.10+1.21.1.jar` |

**Not in this release:** NeoForge **26.2** (`:neoforge-26`) — port in progress; no jar shipped.

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
