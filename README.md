# Az's Companions

Wholesome adult companion mod for Minecraft **1.21.1** (NeoForge + Fabric). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.

> **Breaking rename:** Previously mod id `koncompanions`. Old worlds/items do not migrate — start fresh or re-loot charms.

## Editions (pick one jar)

| Loader | Module | Jar | Notes |
|--------|--------|-----|-------|
| **NeoForge** (standalone) | `:neoforge` | `azscompanions-neoforge-0.1.0.jar` | Default — no CCI |
| **NeoForge** (CCI) | `:neoforge-cci` | `azscompanions-neoforge-cci-0.1.0.jar` | Needs CCI + iChunUtil |
| **Fabric** (standalone) | `:fabric` | `azscompanions-fabric-0.1.0.jar` | Default — no CCI |
| **Fabric** (CCI) | `:fabric-cci` | `azscompanions-fabric-cci-0.1.0.jar` | Needs Fabric CCI + iChunUtil |

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

## Gameplay (0.1.0)

- **Companion Charm** — desert pyramid chest loot; **one companion per player**
- Charm **summon / store**; appear → `<Name> Hello!` · store → `<Name> Bye!` (owner only)
- First summon uses **your username + your skin**
- **Follow** by day; casual **wander** near owner when idle (8–16 blocks); at night sleeps in the **nearest bed**
- Loose follow leash: path start **32** / stop **24** / teleport **48**; no teleport while fighting
- **Defend** living attackers of the owner (not environmental damage)
- **Hands:** right-click with an item to give (main → offhand → swap); empty hand takes back
- **Potions:** auto-pickup **beneficial only**; give a harmful splash and they throw it at enemies
- **Shift + right-click** — NeoForge: Customize · Fabric: inventory
- UUID-gated **special perks** for a few owners (flight follow / glow / nametag)

### Customize (NeoForge)

Shift + right-click → name, gender (Female/Male), Mojang skin via username, size & proportions. Size **0.5–3.0** (default **0.7**). Male hides bust morph. **Done** saves; Cancel discards.

Skins are **Mojang-only** (valid Minecraft username → profile skin). Local PNG import is not supported. Charm store/recall keeps name, skin, gender, size, proportions, and home bed.

Rename to **Kon** (case-insensitive) for the Kon skin and a one-time **Kon Bed**. Other names keep Mojang/player skins; sleep still works on any bed.

## CCI / Content Creator Integration

Full guide: [docs/CCI_STREAMING_GUIDE.md](docs/CCI_STREAMING_GUIDE.md)

Use a **CCI edition** jar with CCI **1.13.0** + iChunUtil **1.0.3**. Standalone jars have no bridge. Fabric CCI also supports `/azscci <subject> [message]` via CCI CommandOutcome.

In the CCI Editor, wire stream events to **IMCOutcome**:

| Field | Value |
|-------|--------|
| `modId` | `azscompanions` |
| `subject` | see table |
| `message` | text or CCI vars (e.g. `$username`) |

Companion must be **summoned** and within **~96 blocks**.

| Subject | Effect |
|---------|--------|
| `companion_say` | Chat-line `message` |
| `companion_greet` | Thanks for the support, `<message>`! |
| `companion_wave` | Hello, `<message>`! (or Hello there!) |
| `companion_follow` | Mode FOLLOW |
| `companion_sit` | Mode SIT |
| `companion_stay` | Mode STAY |

Aliases: `say` / `greet` / `wave` / `follow` / `sit` / `stay`.

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

- `neoforge/build/libs/azscompanions-neoforge-0.1.0.jar`
- `neoforge-cci/build/libs/azscompanions-neoforge-cci-0.1.0.jar`
- `fabric/build/libs/azscompanions-fabric-0.1.0.jar`
- `fabric-cci/build/libs/azscompanions-fabric-cci-0.1.0.jar`

### CCI edition Maven coords (Modrinth)

| Dependency | Release | NeoForge | Fabric |
|------------|---------|----------|--------|
| CCI (`contentcreatorintegration`) | **1.13.0** | `AySbAgcO` | `PERd6IT9` |
| iChunUtil (`ichunutil`) | **1.0.3** | `OvIyyNh4` | `gfAOoiwe` |

Repository: `https://api.modrinth.com/maven` — `maven.modrinth:content-creator-integration:<id>` / `maven.modrinth:ichunutil:<id>`.

## License

MIT (see `LICENSE`).
