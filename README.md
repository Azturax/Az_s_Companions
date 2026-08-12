# Az's Companions

Wholesome adult companion mod for Minecraft **1.21.1** (NeoForge + Fabric). Mod id: `azscompanions`.

- **Repo:** [github.com/Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- **Release:** [v1.0.2](https://github.com/Azturax/Az_s_Companions/releases/tag/v1.0.2)
- **CCI Documentation:** [docs/CCI.md](docs/CCI.md)
- **Companion AI:** [docs/COMPANION_AI.md](docs/COMPANION_AI.md)
- **Activity skins:** [docs/CONTEXT_SKINS.md](docs/CONTEXT_SKINS.md)
- **Gather / deposit:** [docs/GATHER.md](docs/GATHER.md)
- **Multi-version plan:** [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md)

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.


## Editions (pick one jar)

| Loader | Module | Jar | Notes |
|--------|--------|-----|-------|
| **NeoForge** (standalone) | `:neoforge` | `azscompanions-neoforge-1.0.2+1.21.1.jar` | Default — no CCI |
| **NeoForge** (CCI) | `:neoforge-cci` | `azscompanions-neoforge-cci-1.0.2+1.21.1.jar` | Needs CCI + iChunUtil |
| **Fabric** (standalone) | `:fabric` | `azscompanions-fabric-1.0.2+1.21.1.jar` | Default — no CCI |
| **Fabric** (CCI) | `:fabric-cci` | `azscompanions-fabric-cci-1.0.2+1.21.1.jar` | Needs Fabric CCI + iChunUtil |

Install **one** Az's Companions jar per loader — never standalone + CCI together.

## Install

1. Minecraft **1.21.1**, Java **21**
2. NeoForge **21.1.x** *or* Fabric Loader ≥0.16 + Fabric API
3. Drop the matching edition jar into `mods/`
4. **CCI editions only** — also install CCI **1.13.0** + iChunUtil **1.0.3** for the **same loader** (see [docs/CCI.md](docs/CCI.md))

**Other Minecraft lines** (including NeoForge **26.2** → Minecraft **26.2**): not shipped — see [docs/MULTI_VERSION.md](docs/MULTI_VERSION.md).

## Gameplay (1.0.2)

- Companion Charm, Follow/Stay/Sit/Wander (command menu + **K** keybind), Customize, inventory, night sleep
- **Treasure loot:** Companion Charm in desert pyramids (5%). Disable with `world.enableLoot=false` — NeoForge `azscompanions-common.toml` / Fabric `azscompanions-common.json` (default on)
- **UUID perks:** Wolfy grant only (`7c97…`); Wiggly toggle + survival flight, no glow (`4274…`) — **Wiggly dog default OFF**; toggle with `/az wiggly` or keybind (at most one dog)
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
- Soft-compat: Simple Voice Chat + dynamic lights + FTB/map hooks — [COMPAT.md](docs/COMPAT.md)

### Known limitations

- **NeoForge 26.2** (`:neoforge-26`) is a work-in-progress port — **no jar** in this release
- VoiceMod TTS / Simple Voice Chat entity audio emission — detect-only soft-compat (not full TTS bridge)
- Old saves with removed features (Glowing Orb form, Jindujun whistle/nimbus) migrate or simply no longer spawn those items/entities

## Build

```bash
./gradlew buildAll
```

Outputs under `*/build/libs/azscompanions-*-1.0.2+1.21.1.jar` (four editions). NeoForge 26.2 uses `./gradlew buildNeoForge26` and is not part of `buildAll`.

## License

MIT (see `LICENSE`).
