# Az's Companions

Wholesome adult companion mod for Minecraft **1.21.1** (NeoForge + Fabric). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v0.3.10](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.10)
- **CCI Documentation:** [docs/CCI.md](docs/CCI.md)
- **Companion AI:** [docs/COMPANION_AI.md](docs/COMPANION_AI.md)
- **Gather / deposit:** [docs/GATHER.md](docs/GATHER.md)
- **Multi-version plan:** [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.


## Editions (pick one jar)

| Loader | Module | Jar | Notes |
|--------|--------|-----|-------|
| **NeoForge** (standalone) | `:neoforge` | `azscompanions-neoforge-0.3.10+1.21.1.jar` | Default — no CCI |
| **NeoForge** (CCI) | `:neoforge-cci` | `azscompanions-neoforge-cci-0.3.10+1.21.1.jar` | Needs CCI + iChunUtil |
| **Fabric** (standalone) | `:fabric` | `azscompanions-fabric-0.3.10+1.21.1.jar` | Default — no CCI |
| **Fabric** (CCI) | `:fabric-cci` | `azscompanions-fabric-cci-0.3.10+1.21.1.jar` | Needs Fabric CCI + iChunUtil |

Install **one** Az's Companions jar per loader — never standalone + CCI together.

## Install

1. Minecraft **1.21.1**, Java **21**
2. NeoForge **21.1.x** *or* Fabric Loader ≥0.16 + Fabric API
3. Drop the matching edition jar into `mods/`
4. **CCI editions only** — also install CCI **1.13.0** + iChunUtil **1.0.3** for the **same loader** (see [docs/CCI.md](docs/CCI.md))

**Other Minecraft lines** (including NeoForge **26.2** → Minecraft **26.2**): not shipped yet — see [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md).

## Gameplay (0.3.10)

- Companion Charm, Follow/Stay/Wander, Customize, inventory, night sleep
- **Chunk tickets:** summoned companions and child Bits force-load their chunk (`companionChunkLoading`, default true) so AI/follow/sleep keep running when you walk away — NeoForge `config/azscompanions-server.toml` `[performance]`; Fabric mirrors defaults in code. Cap: `maxForcedChunksPerPlayer` (default 16). Not FTB claims.
- **Optional AI:** name chat (`Kon, …`), `/ask` · `/az ask`, thinking HUD — text dialogue only (no AI Mode menu) — [COMPANION_AI.md](docs/COMPANION_AI.md)
- **Admin:** `/az admin` · `/az ai config` (ops / host / whitelist) — edit LLM profiles & save AI file (restart to apply) — [ADMIN.md](docs/ADMIN.md)
- **Commands:** primary root `/az` (alias `/azscompanions`)
- **CCI:** full manual **[CCI Documentation](docs/CCI.md)** — CCI-summoned companions use streamer chat; AI when configured

## Build

```bash
./gradlew buildAll
```

Outputs under `*/build/libs/azscompanions-*-0.3.10+1.21.1.jar`.

## License

MIT (see `LICENSE`).
