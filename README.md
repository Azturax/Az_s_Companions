# Az's Companions

Wholesome adult companion mod for Minecraft (**1.21.1**, **1.21.5**, **1.20.1**). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v1.0.4](https://github.com/Azturax/Az_s_Companions/releases/tag/v1.0.4)
- **CCI Documentation:** [docs/CCI.md](docs/CCI.md)
- **Companion AI:** [docs/COMPANION_AI.md](docs/COMPANION_AI.md)
- **Activity skins:** [docs/CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Gather / deposit:** [docs/GATHER.md](docs/GATHER.md)
- **Multi-version plan:** [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.


## Editions (pick one jar)

Install **one** Az's Companions jar per loader — never standalone + CCI together.

### Minecraft 1.21.1 (LTS)

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge (standalone) | `:neoforge` | `azscompanions-neoforge-1.0.4+1.21.1.jar` |
| NeoForge (CCI) | `:neoforge-cci` | `azscompanions-neoforge-cci-1.0.4+1.21.1.jar` |
| Fabric (standalone) | `:fabric` | `azscompanions-fabric-1.0.4+1.21.1.jar` |
| Fabric (CCI) | `:fabric-cci` | `azscompanions-fabric-cci-1.0.4+1.21.1.jar` |

Java **21**. NeoForge **21.1.x** or Fabric Loader ≥0.16 + Fabric API. CCI editions need CCI **1.13.0** + iChunUtil **1.0.3** (same loader).

### Minecraft 1.21.5 (latest CCI MC)

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge (standalone) | `:neoforge-21.5` | `azscompanions-neoforge-1.0.4+1.21.5.jar` |
| NeoForge (CCI) | `:neoforge-cci-21.5` | `azscompanions-neoforge-cci-1.0.4+1.21.5.jar` |
| Fabric (standalone) | `:fabric-21.5` | `azscompanions-fabric-1.0.4+1.21.5.jar` |
| Fabric (CCI) | `:fabric-cci-21.5` | `azscompanions-fabric-cci-1.0.4+1.21.5.jar` |

Java **21**. NeoForge **21.5.x** or Fabric API `0.128.2+1.21.5`. CCI editions need CCI **1.13.0** + iChunUtil **1.0.7** (same loader — Modrinth pins in [MULTI_VERSION.md](docs/MULTI_VERSION.md)).

### Minecraft 1.20.1 (Forge, not NeoForge)

| Loader | Module | Jar |
|--------|--------|-----|
| Forge (standalone) | `:forge-1.20.1` | `azscompanions-forge-1.0.4+1.20.1.jar` |
| Forge (CCI) | `:forge-cci-1.20.1` | `azscompanions-forge-cci-1.0.4+1.20.1.jar` |
| Fabric (standalone) | `:fabric-1.20.1` | `azscompanions-fabric-1.0.4+1.20.1.jar` |
| Fabric (CCI) | `:fabric-cci-1.20.1` | `azscompanions-fabric-cci-1.0.4+1.20.1.jar` |

Java **17**. Forge **47.4.x** or Fabric API `0.92.11+1.20.1`. There is **no** NeoForge 20.1 line. CCI editions need CCI **1.13.0** + iChunUtil **1.0.3** for Forge or Fabric (pins in [MULTI_VERSION.md](docs/MULTI_VERSION.md)). Honest API omissions: [fabric-1.20.1/NOTES.md](fabric-1.20.1/NOTES.md), [forge-1.20.1/NOTES.md](forge-1.20.1/NOTES.md).

## Install

1. Pick the Minecraft version + loader table above
2. Drop the matching edition jar into `mods/`
3. **CCI editions only** — also install CCI + iChunUtil for the **same Minecraft version and loader** (see [docs/CCI.md](docs/CCI.md) and [MULTI_VERSION.md](docs/MULTI_VERSION.md))

## Gameplay (1.0.4)

Same product feature set on every shipped jar (AI, inventory, Wiggly/UUID perks, dimension follow, gifts, loot, logout park, etc.):

- Companion Charm, Follow/Stay/Sit/Wander (command menu + **K** keybind), Customize, inventory, night sleep
- **Treasure loot:** Companion Charm in desert pyramids (5%). Disable with `world.enableLoot=false`
- **UUID perks:** Wolfy; survival flight + Wiggly toggle; Mister Wiggly default ON — hard-capped to one; `/az wiggly` or keybind
- Flower gifts, logout park / login restore, form coat arrows, swim follow, ride-along, cat/wolf scare, dimension follow
- **Activity skins** — [CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Chunk tickets** (`companionChunkLoading`, default true)
- **Optional AI:** `/ask` · `/az ask` — [COMPANION_AI.md](docs/COMPANION_AI.md)
- **Admin:** `/az admin` · `/az ai config` — [ADMIN.md](docs/ADMIN.md)
- **CCI:** [CCI Documentation](docs/CCI.md)
- Soft-compat: Simple Voice Chat + dynamic lights + FTB/map hooks — [COMPAT.md](docs/COMPAT.md)

### Known limitations

- **NeoForge 26.1.2 / 26.2** — WIP modules only; **no jars** in this release ([MULTI_VERSION.md](docs/MULTI_VERSION.md))
- **1.20.1** API omissions listed above (JourneyMap, wolf body armor, scale attribute, etc.)
- VoiceMod TTS / Simple Voice Chat entity audio — detect-only soft-compat

## Build

```bash
./gradlew buildAll       # 1.21.1 ×4
./gradlew buildAll215    # 1.21.5 ×4
./gradlew buildAll1201   # 1.20.1 ×4 (fabric + forge, ±CCI)
```

Outputs under `*/build/libs/azscompanions-*-1.0.4+*.jar`. NeoForge 26.x uses `buildNeoForge26` / `buildNeoForge261` and is **not** part of release.

## License

MIT (see `LICENSE`).
