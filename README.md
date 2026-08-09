# Az's Companions

Multi-loader companion mod for Minecraft **1.21.1** (NeoForge + Fabric). Mod id: `azscompanions`.

| Loader | Module | Jar | Notes |
|--------|--------|-----|-------|
| **NeoForge** (standalone) | `:neoforge` | `azscompanions-neoforge-0.1.0.jar` | Default — no CCI required |
| **NeoForge** (CCI edition) | `:neoforge-cci` | `azscompanions-neoforge-cci-0.1.0.jar` | Requires iChun CCI + iChunUtil |
| **Fabric** (standalone) | `:fabric` | `azscompanions-fabric-0.1.0.jar` | Default — no CCI required |
| **Fabric** (CCI edition) | `:fabric-cci` | `azscompanions-fabric-cci-0.1.0.jar` | Requires Fabric CCI + iChunUtil |

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.

> **Breaking rename:** This mod was previously id `koncompanions`. Worlds/items from the old id will not migrate automatically — start fresh or re-loot charms.

## Requirements

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.x **or** Fabric Loader ≥0.16 + Fabric API

### CCI edition extra requirements

Install the matching **CCI edition** jar **instead of** the standalone jar for that loader (do not install both), plus CCI + iChunUtil for the same loader:

| Mod | NeoForge 1.21–1.21.1 | Fabric 1.21–1.21.1 |
|-----|----------------------|--------------------|
| [Content Creator Integration](https://modrinth.com/mod/content-creator-integration) **1.13.0** | [AySbAgcO](https://modrinth.com/mod/content-creator-integration/version/AySbAgcO) (`…-NeoForge-1.13.0.jar`) | [PERd6IT9](https://modrinth.com/mod/content-creator-integration/version/PERd6IT9) (`…-Fabric-1.13.0.jar`) |
| [iChunUtil](https://modrinth.com/mod/ichunutil) **1.0.3** | [OvIyyNh4](https://modrinth.com/mod/ichunutil/version/OvIyyNh4) | [gfAOoiwe](https://modrinth.com/mod/ichunutil/version/gfAOoiwe) |

CCI is iChun’s **stream integration** mod (Twitch / Streamlabs / StreamElements / etc.), not a character/skin API. CCI editions hard-depend on it and wire companions via IMC outcomes (Fabric uses a mixin bridge because iChunUtil Fabric has no InterModComms).

> Do **not** use the 1.21.3+ / 1.21.5 CCI jars with this project — those target newer Minecraft. For 1.21.1 use the `1.21-*-1.13.0` artifacts above.

## Build

```bash
./gradlew :neoforge:build
./gradlew :neoforge-cci:build
./gradlew :fabric:build
./gradlew :fabric-cci:build
# or everything:
./gradlew buildAll
```

Outputs:

- `neoforge/build/libs/azscompanions-neoforge-0.1.0.jar`
- `neoforge-cci/build/libs/azscompanions-neoforge-cci-0.1.0.jar`
- `fabric/build/libs/azscompanions-fabric-0.1.0.jar`
- `fabric-cci/build/libs/azscompanions-fabric-cci-0.1.0.jar`

### Gradle / Maven coords used by the CCI editions

| Dependency | Mod id | Release | NeoForge Maven | Fabric Maven |
|------------|--------|---------|----------------|--------------|
| CCI | `contentcreatorintegration` | **1.13.0** | `…:AySbAgcO` | `…:PERd6IT9` |
| iChunUtil | `ichunutil` | **1.0.3** | `…:OvIyyNh4` | `…:gfAOoiwe` |

Repository: `https://api.modrinth.com/maven` (group `maven.modrinth`). Full coords: `maven.modrinth:content-creator-integration:<id>` / `maven.modrinth:ichunutil:<id>`.

## Quick start

1. Find a **Companion Charm** in desert pyramid chests (loot-only; one per player)
2. Right-click the charm to recruit / summon / store your companion
3. First summon uses **your username + your skin** (not Kon special)
4. Companion **follows** by day; at night sleeps in the **nearest bed** (any vanilla bed or Kon Bed)
5. **Shift + right-click** to Customize (NeoForge). Fabric: Shift+right-click opens inventory
6. Charm appear → `<Name> Hello!` · charm store → `<Name> Bye!`

## Customize

NeoForge creator: name, gender (Female/Male), Mojang skin (via username), size & proportions. Size **0.5–3.0** (default **0.7**). Male hides bust morph.

Skins are **Mojang-only**: typing a valid Minecraft username fetches that player's profile skin. Local PNG import is not supported. Charm store/recall keeps name, skin, gender, size, proportions, and home bed.

### Kon special name

Rename the companion to **Kon** (case-insensitive) to load the Kon skin and receive a **Kon Bed** once. Other names keep player/Mojang skins; sleep still works on regular beds.

## CCI / Content Creator Integration

**Guide:** [docs/CCI_STREAMING_GUIDE.md](docs/CCI_STREAMING_GUIDE.md) — install steps, IMC subjects, and example stream setups.

| Edition | Jar |
|---------|-----|
| Standalone NeoForge | `azscompanions-neoforge-0.1.0.jar` |
| NeoForge CCI | `azscompanions-neoforge-cci-0.1.0.jar` |
| Standalone Fabric | `azscompanions-fabric-0.1.0.jar` |
| Fabric CCI | `azscompanions-fabric-cci-0.1.0.jar` |

Use a CCI jar with CCI **1.13.0** + iChunUtil **1.0.3** for the **same loader**. Do not install standalone + CCI jars together. Standalone jars have no bridge. Fabric CCI also supports `/azscci <subject> [message]` via CCI CommandOutcome.

In the CCI Editor, wire any stream Config Event (sub, cheer, tip, channel points, etc.) to an **IMCOutcome**:

| Field | Value |
|-------|--------|
| `modId` | `azscompanions` |
| `subject` | see below |
| `message` | Text or CCI variables such as `$username` |

Companion must be **summoned** and within **~96 blocks**.

### What you can do now (implemented)

Bridge target: CCI `IMCOutcome` → `modId: azscompanions`

| Subject | Effect |
|---------|--------|
| `companion_say` | Companion chat-lines `message` (e.g. bits/tip text with `$username`) |
| `companion_greet` | Thanks for the support, `<message>`! (pass `$username`) |
| `companion_wave` | Hello, `<message>`! (or Hello there! if empty) |
| `companion_follow` | Mode FOLLOW (clears tasks); message ignored |
| `companion_sit` | Mode SIT; message ignored |
| `companion_stay` | Mode STAY; message ignored |

Aliases `say` / `greet` / `wave` / `follow` / `sit` / `stay` also work.

### Alongside (CCI itself, not our bridge)

Spawn mobs via CCI `CommandOutcome` + `/summon` (example: `cci-examples/command-summon-wolf-alongside.json`). Can stack with greet/say in the same event.

### Limitations

- No spawn / inventory / skin / summon-companion IMC from us
- Standalone jars have no bridge — need the matching CCI edition + CCI **1.13.0** + iChunUtil **1.0.3** (same loader)
- No auto-imported full stream config — copy examples or build in the CCI Editor

### Documented examples in the guide

- Sub / follow → greet
- Bits / tip → say
- Channel points → wave
- Channel points → follow / sit / stay
- Sub → greet + CCI `/summon`
- Tip threshold → say + stay

Example snippets ship inside the jar under `cci-examples/` (copy into CCI configs; not auto-loaded).

## License

MIT (see `LICENSE`).
