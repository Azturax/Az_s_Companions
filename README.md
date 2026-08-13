# Az's Companions

Wholesome adult companion mod for Minecraft (**1.21.1**, **1.21.5**, **1.20.1**). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v1.0.8](https://github.com/Azturax/Az_s_Companions/releases/tag/v1.0.8)
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
| NeoForge | `:neoforge` | `azscompanions-neoforge-1.0.8+1.21.1.jar` |
| Fabric | `:fabric` | `azscompanions-fabric-1.0.8+1.21.1.jar` |

Java **21**. NeoForge **21.1.x** or Fabric Loader ≥0.16 + Fabric API. For streaming: optionally install CCI **1.13.0** + iChunUtil **1.0.3** (same loader).

### Minecraft 1.21.5 (latest CCI MC)

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge | `:neoforge-21.5` | `azscompanions-neoforge-1.0.8+1.21.5.jar` |
| Fabric | `:fabric-21.5` | `azscompanions-fabric-1.0.8+1.21.5.jar` |

Java **21**. NeoForge **21.5.x** or Fabric API `0.128.2+1.21.5`. Optional CCI **1.13.0** + iChunUtil **1.0.7** (pins in [MULTI_VERSION.md](docs/MULTI_VERSION.md)).

### Minecraft 1.20.1 (Forge, not NeoForge)

| Loader | Module | Jar |
|--------|--------|-----|
| Forge | `:forge-1.20.1` | `azscompanions-forge-1.0.8+1.20.1.jar` |
| Fabric | `:fabric-1.20.1` | `azscompanions-fabric-1.0.8+1.20.1.jar` |

Java **17**. Forge **47.4.x** or Fabric API `0.92.11+1.20.1`. There is **no** NeoForge 20.1 line. Optional CCI **1.13.0** + iChunUtil **1.0.3**. Honest API omissions: [fabric-1.20.1/NOTES.md](fabric-1.20.1/NOTES.md), [forge-1.20.1/NOTES.md](forge-1.20.1/NOTES.md).

### NeoForge 26.x

| Loader | Module | Jar |
|--------|--------|-----|
| NeoForge 26.1.2 | `:neoforge-26-1` | `azscompanions-neoforge-1.0.8+26.1.2.jar` |
| NeoForge 26.2 | `:neoforge-26` | `azscompanions-neoforge-1.0.8+26.2.jar` |

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

### Known limitations

- **26.x** — no CCI; other omissions in [MULTI_VERSION.md](docs/MULTI_VERSION.md)
- **1.20.1** API omissions listed above (JourneyMap, wolf body armor, scale attribute, etc.)
- VoiceMod TTS / Simple Voice Chat entity audio — detect-only soft-compat

## Build

```bash
./gradlew buildAll       # 1.21.1 ×2 (fabric + neoforge)
./gradlew buildAll215    # 1.21.5 ×2
./gradlew buildAll1201   # 1.20.1 ×2 (fabric + forge)
./gradlew buildNeoForge26 buildNeoForge261
```

Outputs under `*/build/libs/azscompanions-*-1.0.8+*.jar` (**8 jars** total).

## Publishing (Modrinth / CurseForge)

GitHub Actions workflow [`.github/workflows/publish.yml`](.github/workflows/publish.yml) mirrors **GitHub Release** jars to Modrinth and CurseForge (one store version per jar / loader / MC line).

1. Create **Az's Companions** Modrinth + CurseForge projects (suggested slug `azs-companions`)
2. Add secrets `PUBLISH_MODRINTH_TOKEN`, `PUBLISH_CURSEFORGE_TOKEN` (legacy `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` also work) and variables `MODRINTH_PROJECT_ID`, `CURSEFORGE_PROJECT_ID` (Az's Companions project IDs)
3. Publish a GitHub Release, or run **Actions → Publish Modrinth / CurseForge** with tag `v1.0.8`

Full setup: [docs/PUBLISHING.md](docs/PUBLISHING.md). Store paste copy: [docs/STORE_DESCRIPTION.md](docs/STORE_DESCRIPTION.md).

## License

MIT (see `LICENSE`).
