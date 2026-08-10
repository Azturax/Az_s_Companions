# Az's Companions

Wholesome adult companion mod for Minecraft **1.21.1** (NeoForge + Fabric). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v0.3.5](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.5)
- **Multi-version plan:** [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.


## Editions (pick one jar)

| Loader | Module | Jar | Notes |
|--------|--------|-----|-------|
| **NeoForge** (standalone) | `:neoforge` | `azscompanions-neoforge-0.3.5+1.21.1.jar` | Default — no CCI |
| **NeoForge** (CCI) | `:neoforge-cci` | `azscompanions-neoforge-cci-0.3.5+1.21.1.jar` | Needs CCI + iChunUtil |
| **Fabric** (standalone) | `:fabric` | `azscompanions-fabric-0.3.5+1.21.1.jar` | Default — no CCI |
| **Fabric** (CCI) | `:fabric-cci` | `azscompanions-fabric-cci-0.3.5+1.21.1.jar` | Needs Fabric CCI + iChunUtil |

Install **one** Az's Companions jar per loader — never standalone + CCI together.

## Install

1. Minecraft **1.21.1**, Java **21**
2. NeoForge **21.1.x** *or* Fabric Loader ≥0.16 + Fabric API
3. Drop the matching edition jar into `mods/`
4. **CCI editions only** — also install CCI **1.13.0** + iChunUtil **1.0.3** for the **same loader**:

| Mod | NeoForge 1.21–1.21.1 | Fabric 1.21–1.21.1 |
|-----|----------------------|--------------------|
| [CCI](https://modrinth.com/mod/content-creator-integration) **1.13.0** | [AySbAgcO](https://modrinth.com/mod/content-creator-integration/version/AySbAgcO) | [PERd6IT9](https://modrinth.com/mod/content-creator-integration/version/PERd6IT9) |
| [iChunUtil](https://modrinth.com/mod/ichunutil) **1.0.3** | [OvIyyNh4](https://modrinth.com/mod/ichunutil/version/OvIyyNh4) | [gfAOoiwe](https://modrinth.com/mod/ichunutil/version/gfAOoiwe) |

Do **not** use 1.21.3+ / 1.21.5 CCI jars with this project.

## Gameplay (0.3.5)

- **Companion Charm** — desert pyramid chest loot; **one companion per player** (UUID-based; unloaded companions may not count toward the limit)
- Charm **summon / store**; appear → `<Name> Hello!` · store → `<Name> Bye!` (owner only)
- First summon uses **your username + your skin** (Kon is an optional easter egg name)
- **Shift + right-click** → **Companion Menu**: Customize · **Command** (Follow / Stay / Wander) · Inventory
- **Follow / Stay / Wander** — Stay never teleports (like sitting pets)
- **Night sleep (Follow):** nearest usable empty bed within **48** blocks (horizontal + vertical); Kon-named prefer Kon beds
- Customize **Form** tab: player or animal/hostile looks; **Nametag** show/hide
- Inventory: armor+shield column, 9-slot hotbar, gapped player inv (plain slots); player/humanoid forms show armor; animals reject plate armor (wolf accepts wolf armor)
- Mojang skins can show the player's **cape**
- **Defend** living attackers of the owner (not environmental damage); optional **HOSTILE** attitude / **teams** via CCI
- UUID-gated **special perks** (flight follow / glow / Kon ears / Pecker chicken / Wiggly)
- **Optional companion AI** (default off, **text dialogue**): local / OpenAI-compatible / MCP — see [docs/COMPANION_AI.md](docs/COMPANION_AI.md). `/azscompanions ask <message>` · `/azscompanions ai status`

### Customize

Shift + right-click → Companion Menu → **Customize**: Name (nametag toggle), **Form**, Face/Skin, Body. Size **0.5–3.0** (default **0.7**).

Skins are **Mojang-only**. Rename to **Kon** for the Kon skin easter egg (no UI tip required).

## CCI / Content Creator Integration

Full guide: [docs/CCI_STREAMING_GUIDE.md](docs/CCI_STREAMING_GUIDE.md)

Use a **CCI edition** jar with CCI **1.13.0** + iChunUtil **1.0.3**. Fabric also supports `/azscci <subject> [message]`.

| Subject | Effect |
|---------|--------|
| `companion_say` / `greet` / `wave` | Chat lines |
| `companion_follow` / `sit` / `stay` | Modes |
| `companion_set_attitude` | `passive` / `hostile` |
| `companion_set_team` | Team id (`$username` or `red`) — rivals fight |
| `companion_summon` / `_passive` / `_hostile` | `form=` / `skin=` / `team=` / `attitude=` |
| `companion_modify` / `customize` | Edit **called/summoned** companion (`form=` / `skin=` / `name=` / …) |
| `companion_turn_evil` / `berserk` | Playful HOSTILE 5–15s then revert (`seconds=10`) |
| `companion_set_mainhand` / `offhand` / `armor` / `hand` | Equipment item ids or `clear` |

Message format: `key=value` pairs (`form=zombie;team=red;mainhand=minecraft:diamond_sword`).

Aliases: `say` / `greet` / `wave` / `follow` / `sit` / `stay` / `modify` / `turn_evil`.

Hidden: right-click companion with a **fermented spider eye** for the same playful evil burst (~10s).

Mob spawning is **CCI-native** (`CommandOutcome` + `/summon`), not our bridge — can stack with greet/say. Example snippets ship in the jar under `cci-examples/` (not auto-loaded).

## Build

```bash
./gradlew :neoforge:build
./gradlew :neoforge-cci:build
./gradlew :fabric:build
./gradlew :fabric-cci:build
# or:
./gradlew buildAll
```

Outputs:

- `neoforge/build/libs/azscompanions-neoforge-0.3.5+1.21.1.jar`
- `neoforge-cci/build/libs/azscompanions-neoforge-cci-0.3.5+1.21.1.jar`
- `fabric/build/libs/azscompanions-fabric-0.3.5+1.21.1.jar`
- `fabric-cci/build/libs/azscompanions-fabric-cci-0.3.5+1.21.1.jar`

### CCI edition Maven coords (Modrinth)

| Dependency | Release | NeoForge | Fabric |
|------------|---------|----------|--------|
| CCI (`contentcreatorintegration`) | **1.13.0** | `AySbAgcO` | `PERd6IT9` |
| iChunUtil (`ichunutil`) | **1.0.3** | `OvIyyNh4` | `gfAOoiwe` |

Repository: `https://api.modrinth.com/maven` — `maven.modrinth:content-creator-integration:<id>` / `maven.modrinth:ichunutil:<id>`.

## License

MIT (see `LICENSE`).
