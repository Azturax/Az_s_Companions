# Az's Companions

Wholesome adult companion mod for Minecraft (**1.21.1**, **1.21.5**, **1.20.1**). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v1.0.13](https://github.com/Azturax/Az_s_Companions/releases/tag/v1.0.13)
- **Publishing (Modrinth / CurseForge):** [docs/PUBLISHING.md](docs/PUBLISHING.md)
- **CCI Documentation:** [docs/CCI.md](docs/CCI.md)
- **Companion AI:** [docs/COMPANION_AI.md](docs/COMPANION_AI.md)
- **Activity skins:** [docs/CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Gather / deposit:** [docs/GATHER.md](docs/GATHER.md)
- **Multi-version plan:** [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.


## Jars (one per Minecraft × loader)

Install **one** Az's Companions jar for your loader. NeoForge and Fabric are **always separate** jars (no universal cross-loader jar). CCI support is **built into** the main jars as an optional soft-compat (works with or without CCI installed) — there are **no** separate `*-cci` jars.

### Minecraft 1.21.1 (LTS)

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge | `:neoforge` | `azscompanions-neoforge-1.0.13+1.21.1.jar` |
| Fabric | `:fabric` | `azscompanions-fabric-1.0.13+1.21.1.jar` |

Java **21**. NeoForge **21.1.x** or Fabric Loader ≥0.16 + Fabric API. For streaming: optionally install CCI **1.13.0** + iChunUtil **1.0.3** (same loader).

### Minecraft 1.21.5 (latest CCI MC)

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge | `:neoforge-21.5` | `azscompanions-neoforge-1.0.13+1.21.5.jar` |
| Fabric | `:fabric-21.5` | `azscompanions-fabric-1.0.13+1.21.5.jar` |

Java **21**. NeoForge **21.5.x** or Fabric API `0.128.2+1.21.5`. Optional CCI **1.13.0** + iChunUtil **1.0.7** (pins in [MULTI_VERSION.md](docs/MULTI_VERSION.md)).

### Minecraft 1.20.1 (Forge, not NeoForge)

| Loader | Module | Jar |
|--------|--------|-----|
| Forge | `:forge-1.20.1` | `azscompanions-forge-1.0.13+1.20.1.jar` |
| Fabric | `:fabric-1.20.1` | `azscompanions-fabric-1.0.13+1.20.1.jar` |

Java **17**. Forge **47.4.x** or Fabric API `0.92.11+1.20.1`. There is **no** NeoForge 20.1 line. Optional CCI **1.13.0** + iChunUtil **1.0.3**. Honest API omissions: [fabric-1.20.1/NOTES.md](fabric-1.20.1/NOTES.md), [forge-1.20.1/NOTES.md](forge-1.20.1/NOTES.md).

### NeoForge 26.x

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge 26.1.2 | `:neoforge-26-1` | `azscompanions-neoforge-1.0.13+26.1.2.jar` |
| NeoForge 26.2 | `:neoforge-26` | `azscompanions-neoforge-1.0.13+26.2.jar` |

Java **25**. No CCI for 26.x.

## Install

1. Pick the Minecraft version + loader table above
2. Drop the matching jar into `mods/`
3. **Optional streaming:** also install CCI + iChunUtil for the **same Minecraft version and loader** (see [docs/CCI.md](docs/CCI.md) and [MULTI_VERSION.md](docs/MULTI_VERSION.md))

## Gameplay

Same product feature set on every shipped jar (AI, inventory, Wiggly/UUID perks, dimension follow, gifts, loot, logout park, etc.):

- Companion Charm, Follow/Stay/Sit/Wander (command menu + **K** keybind), Customize, inventory, night sleep
- **Treasure loot:** Companion Charm in desert pyramids (5%). Disable with `world.enableLoot=false`
- **UUID perks:** Wolfy; survival flight + Wiggly toggle; Mister Wiggly default ON — hard-capped to one; `/az wiggly` or keybind
- Flower gifts, logout park / login restore, form coat arrows, swim follow, ride-along, cat/wolf scare, dimension follow
- **Activity skins** — [CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Chunk tickets** (`companionChunkLoading`, default true)
- **Optional AI:** `/ask` · `/az ask` — [COMPANION_AI.md](docs/COMPANION_AI.md)
- **Admin:** `/az admin` · `/az ai config` — [ADMIN.md](docs/ADMIN.md)
- **CCI (optional):** [CCI Documentation](docs/CCI.md)
- Soft-compat: Simple Voice Chat + dynamic lights + FTB/map hooks — [COMPAT.md](docs/COMPAT.md)

## Commands

Primary root is `/az`. `/azscompanions` is an alias of `/az`. Permission **0** unless noted. There is no `/az dismiss` or `/az charm` — store/recall is the **Companion Charm** item (right-click). Settings are `/az admin`.

### Charm / owned companions

These act on **your** persistent companion (Charm Kon / Bits / named perk companions). They never apply a timed death window.

```
/az select
/az recruit [id]                 # default azscompanions:kon
/az home                         # NeoForge / Forge — set home at your feet
/az rename <name>                # 1–32 characters
/az customize                    # NeoForge / Forge — creator screen
/az size <scale>                 # Fabric — body scale
/az skin <path>                  # Fabric — texture path (URLs disabled)
/az inventory                    # Fabric — open inventory
/az stats [Name]
/az wiggly                       # UUID perk toggle
```

Persona:

```
/az persona
/az persona nearest|clear|edit
/az persona set <field> <text>           # who|what|how|speech|relationship|quirks
/az persona <Name>
/az persona <Name> clear|edit
/az persona <Name> set <field> <text>
```

Ask / AI (owned companions only):

```
/ask <message>
/az ask [Name] <message>
/az ai status
/az ai config                    # same panel as /az admin
/az admin
```

Gather (NeoForge 1.21.1 / 1.21.5 and Forge 1.20.1 — not Fabric, not 26.x):

```
/az gather status
/az gather cancel
/az gather <item> <count> [deposit]
```

Deposit (`/deposit` on every line; `/az deposit` on 1.21.1 / 1.21.5 / 1.20.1):

```
/az deposit
/az deposit done
/az deposit clear
/deposit
/deposit done
/deposit clear
```

Team fight (permission **2**):

```
/az teamfight on|off|status
```

Debug (permission **2**): `/kondebug task|path|inventory|compat|reset|enqueue <type>`

Fabric CCI fallback (permission **0**): `/azscci <subject> [message]`

### CCI / streamer temporary summon

Permission **2**. All loaders including 26.x. Spawns an **extra** companion that follows the target player. It is **not** the charm companion, does **not** steal or replace Kon/Bits/Wiggly/Dox, and is excluded from unique-per-player spawn guards and logout/charm persistence.

```
/az summon [type] [player] [durationSeconds] [health] [armor] [weapon] [tool] [shield] [mode] [name]
```

Arguments are nested left-to-right. To reach `name` you must pass duration, health, the four equipment tokens, and `mode` (or `-` to skip mode).

| Arg | Default | Notes |
|-----|---------|--------|
| `type` | `player` (random Steve/Alex) | Omit, `player`, or `human` → player-form with a **random Steve (wide) or Alex (slim)** vanilla skin each summon. `steve` / `alex` force one. `kon`, `bits`, `wiggly` (wolf form, **not** the perk dog), `dox` keep those appearances. Any form (`wolf`, `zombie`, …) or a datapack id |
| `player` | command source | Who the summon follows (the streamer) |
| `durationSeconds` | `90` | Timed death window (`0`–`3600`). `0` = no expiry (testing). NeoForge/Forge config: `cciSummonDurationSeconds`. Fabric default is 90 |
| `health` | companion default | Integer `1`–`1024` (Brigadier — **not** skippable with `-`) |
| `armor` | none | Material (`leather`/`chainmail`/`iron`/`gold`/`diamond`/`netherite`) = full set, or a single item id. Skip with `-` / `none` / `default` / `skip` / `*` |
| `weapon` | none | Item id → main hand. Invalid ids ignored. Same skip tokens |
| `tool` | none | Item id → main hand if no weapon, else utility slot. Same skip tokens |
| `shield` | none | Item id → offhand. Same skip tokens |
| `mode` | `follow` | Behavior: `follow`, `stay`, `sit`, `idle`/`wander`, `attack`/`guard`, `patrol`, `home`. Skip with `-`. A non-mode token here is treated as `name` (so older `… - Alice` still names the summon) |
| `name` | unset | Username nametag (sub gift / bits). Always shown. Greedy string (spaces allowed) |

**Charm companions are not expired or killed by this command.** Only entities flagged `CciSummoned` die when the window ends or when `/az summon kill` runs. Charm Kon / Bits / Wiggly / Dox and the Wiggly dog are untouched. `/az summon wiggly` spawns a wolf-form companion **without** the extra sidekick dog.

Username + skin: `name` is the nametag. For **player-form** summons (`player`, `kon`, `bits`, `dox`), the mod looks up that Minecraft username’s skin (Mojang profile) and uses it when found. If lookup fails (or no name was given): default/`player` summons keep a random Steve/Alex skin; explicit `kon`/`bits`/`dox` keep Kon’s texture. Wolf/`wiggly`/mob forms skip skins so models are not replaced. Charm-owned Kon/Bits/Wiggly are unchanged.

Kill **only** temporary `/az summon` / CCI extras (permission **2**):

```
/az summon kill all              # every CCI summon on the server
/az summon kill all Steve        # CCI summons owned by Steve
/az summon kill nearest          # closest CCI summon to the executor
/az summon kill Alice            # one CCI summon whose nametag is Alice
```

Examples:

```
# Default: random Steve or Alex following you for 90s (Follow)
/az summon

# Sub gift: Alice’s skin, Follow (mode skipped with -)
/az summon player Steve 90 20 - - - - - Alice

# Wander/idle instead of glued follow
/az summon player Steve 90 20 - - - - idle Alice

# Guard/attack
/az summon kon Steve 90 20 diamond netherite_sword - shield attack Alice

# Explicit Kon model (not the default)
/az summon kon Steve 90 20 - - - - follow Alice

# Bits cheer: diamond kit, 120s
/az summon bits Steve 120 40 diamond netherite_sword diamond_pickaxe shield - CheerName

# Testing: no expiry
/az summon player Steve 0
```

CCI IMC (1.21.1 / 1.21.5 / 1.20.1 — **not** 26.x). Owned by the streamer player who sent the packet:

```
modId   = azscompanions
subject = companion_cci_summon
message = name=$username;duration=90;health=40;armor=diamond;weapon=netherite_sword;tool=diamond_pickaxe;shield=shield
```

Omit `type` (or use `type=player`) for a random Steve/Alex; set `type=kon` only when you want Kon’s appearance. Aliases: `cci_summon`, `temp_summon`, `companion_temp_summon`, `stream_summon`, `companion_stream_summon`. Fabric: `/azscci companion_cci_summon name=$username;duration=90`. IMC keys also accept `user`/`username`/`ign` for the nametag and `hp` for health.

Existing `companion_summon` still recruits a **persistent** owned companion (charm path). Use `companion_cci_summon` / `/az summon` for stream timed spawns.

### Known limitations

- **26.x** — no CCI IMC (`companion_cci_summon` etc.); `/az summon` still works. Other omissions in [MULTI_VERSION.md](docs/MULTI_VERSION.md)
- **1.20.1** API omissions listed above (JourneyMap, wolf body armor, scale attribute, etc.)
- VoiceMod TTS / Simple Voice Chat entity audio — detect-only soft-compat

## Build

```bash
./gradlew buildAll       # 1.21.1 ×2 (fabric + neoforge)
./gradlew buildAll215    # 1.21.5 ×2
./gradlew buildAll1201   # 1.20.1 ×2 (fabric + forge)
./gradlew buildNeoForge26 buildNeoForge261
```

Outputs under `*/build/libs/azscompanions-*-1.0.13+*.jar` (**8 jars** total).

## Publishing (Modrinth / CurseForge)

GitHub Actions workflow [`.github/workflows/publish.yml`](.github/workflows/publish.yml) mirrors **GitHub Release** jars to Modrinth and CurseForge (one store version per jar / loader / MC line).

1. Create **Az's Companions** Modrinth + CurseForge projects (suggested slug `azs-companions`)
2. Add secrets `PUBLISH_MODRINTH_TOKEN`, `PUBLISH_CURSEFORGE_TOKEN` (legacy `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` also work) and variables `MODRINTH_PROJECT_ID`, `CURSEFORGE_PROJECT_ID` (Az's Companions project IDs)
3. Publish a GitHub Release, or run **Actions → Publish Modrinth / CurseForge** with tag `v1.0.13`

Full setup: [docs/PUBLISHING.md](docs/PUBLISHING.md). Store paste copy: [docs/STORE_DESCRIPTION.md](docs/STORE_DESCRIPTION.md).

## License

MIT (see `LICENSE`).
