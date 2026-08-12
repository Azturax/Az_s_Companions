# Version compatibility

Az's Companions ([repo](https://github.com/Azturax/Az_s_Companions), [v1.0.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v1.0.0)) currently **ships and supports**:

- **Minecraft:** **1.21.1** only (`minecraft_version_range=[1.21.1]`)
- **NeoForge:** **21.1.x** (`neo_version_range=[21.1,21.2)`, compile pin `21.1.248`)
- **Fabric:** Loader `>=0.16`, Minecraft `~1.21.1` (see `fabric.mod.json`)

Minecraft / NeoForge **26.2** is tracked in [MULTI_VERSION.md](MULTI_VERSION.md) but **not** shipped. Runtime version ranges intentionally do **not** advertise 26.x until a dedicated NeoForge 26.2 build is release-ready.

## Loaders / editions (1.21.1)

| Loader | Module | Status |
|--------|--------|--------|
| NeoForge | `:neoforge` | Full 1.0.0 set (companions, AI, behavior, etc.) |
| NeoForge CCI | `:neoforge-cci` | Same + IMC bridge (needs CCI 1.13.0 + iChunUtil 1.0.3) |
| Fabric | `:fabric` | Full 1.0.0 set |
| Fabric CCI | `:fabric-cci` | Same + Fabric CCI bridge |

## Why two version properties?

| Property | Purpose |
|----------|---------|
| `minecraft_version` / `neo_version` | Exact pair used to **compile and run** the MDK |
| `minecraft_version_range` / `neo_version_range` | What the built jar **accepts at runtime** |

NeoForge requires the compile `neo_version` to match its Minecraft pair. You cannot compile one jar against both 21.1 and 26.2 toolchains at once — use a **separate module or branch** for 26.2.

## Building for 26.2 (when porting)

1. Prefer the existing `:neoforge-26` module — do **not** retarget the 1.21.1 production modules in place.
2. Pin (check Maven for newer):
   ```properties
   minecraft_26_version=26.2
   neo_26_version=26.2.0.59
   ```
   Java **25**, unobfuscated Minecraft.
3. Finish API breakages for that line (container capabilities, networking, rendering).
4. Ship standalone first; CCI is **unavailable** on Modrinth for 26.2 as of Aug 2026.

See [MULTI_VERSION.md](MULTI_VERSION.md) for the full matrix.
