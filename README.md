# Az's Companions

Wholesome adult companion mod for Minecraft **1.21.1** (NeoForge + Fabric). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v0.4.4](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.4.4)
- **CCI Documentation:** [docs/CCI.md](docs/CCI.md)
- **Companion AI:** [docs/COMPANION_AI.md](docs/COMPANION_AI.md)
- **Activity skins:** [docs/CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Gather / deposit:** [docs/GATHER.md](docs/GATHER.md)
- **Multi-version plan:** [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.


## Editions (pick one jar)

| Loader | Module | Jar | Notes |
|--------|--------|-----|-------|
| **NeoForge** (standalone) | `:neoforge` | `azscompanions-neoforge-0.4.4+1.21.1.jar` | Default — no CCI |
| **NeoForge** (CCI) | `:neoforge-cci` | `azscompanions-neoforge-cci-0.4.4+1.21.1.jar` | Needs CCI + iChunUtil |
| **Fabric** (standalone) | `:fabric` | `azscompanions-fabric-0.4.4+1.21.1.jar` | Default — no CCI |
| **Fabric** (CCI) | `:fabric-cci` | `azscompanions-fabric-cci-0.4.4+1.21.1.jar` | Needs Fabric CCI + iChunUtil |

Install **one** Az's Companions jar per loader — never standalone + CCI together.

## Install

1. Minecraft **1.21.1**, Java **21**
2. NeoForge **21.1.x** *or* Fabric Loader ≥0.16 + Fabric API
3. Drop the matching edition jar into `mods/`
4. **CCI editions only** — also install CCI **1.13.0** + iChunUtil **1.0.3** for the **same loader** (see [docs/CCI.md](docs/CCI.md))

**Other Minecraft lines** (including NeoForge **26.2** → Minecraft **26.2**): not shipped yet — see [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md).

## Gameplay (0.4.4)

- Companion Charm, Follow/Stay/Sit/Wander (command menu + **K** keybind), Customize, inventory, night sleep
- **Treasure loot:** Companion Charm (desert pyramids, 5%) + Jindujun Whistle (Trail Ruins archaeology, 0.5%). Disable with `world.enableLoot=false` — NeoForge `azscompanions-common.toml` / Fabric `azscompanions-common.json` (default on)
- **UUID perks:** Wolfy grant only (`7c97…`); Wiggly toggle + flight, no glow (`4274…`)
- **Jindujun Whistle** / Flying Nimbus — **2.5×** yellow cloud, turns with rider, sit on fluff, 56s idle despawn; shaped **ENCHANT** stream on the nimbus only (no player/companion trails)
- **No natural despawn:** owned companions / Bits get persistence + tag `azscompanions.nodespawn`
- **Flower gifts**, logout park / login restore, form coat arrows, minecart-like sit pose
- Cat scare creepers / wolf scare skeletons; wander mob play; ride-along when you mount
- **Dimension follow:** companions teleport with you into Nether/End/modded dims; persona/form/skin persist for the save
- **Swim follow:** companions swim with you in water while in Follow mode
- **Activity skins** (player form): Sleeping / Bathing / Adventuring outfits — [CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Chunk tickets:** summoned companions and child Bits force-load their chunk (`companionChunkLoading`, default true) so AI/follow/sleep keep running when you walk away — NeoForge `config/azscompanions-server.toml` `[performance]`; Fabric mirrors defaults in code. Cap: `maxForcedChunksPerPlayer` (default 16). Not FTB claims.
- **Optional AI:** `/ask` · `/az ask` + reactive chatter (ITEM_FIND ~once / 14 days); join LLM consent remembered once — [COMPANION_AI.md](docs/COMPANION_AI.md)
- **Admin:** `/az admin` · `/az ai config` (ops / host / whitelist) — editable LLM profiles + opt-in `serverLlmOnly` (Use server LLM, default OFF); save AI file — [ADMIN.md](docs/ADMIN.md) · [COMPANION_AI.md](docs/COMPANION_AI.md)
- **Commands:** primary root `/az` (alias `/azscompanions`); `/az wiggly` for UUID toggle dog
- **CCI:** full manual **[CCI Documentation](docs/CCI.md)** — CCI-summoned companions use streamer chat; AI when configured
- Soft-compat: Simple Voice Chat + dynamic lights — [COMPAT.md](docs/COMPAT.md)
- **No Glowing Orb** (removed; old saves migrate to Player form); **no flight aura** bubbles/trails

## Build

```bash
./gradlew buildAll
```

Outputs under `*/build/libs/azscompanions-*-0.4.4+1.21.1.jar`.

## License

MIT (see `LICENSE`).
