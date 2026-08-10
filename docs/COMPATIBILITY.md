# Version compatibility

Az's Companions ([repo](https://github.com/Azturax/Az_s_Companions), [v0.3.6](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.6)) currently **ships and supports**:

- **Minecraft:** **1.21.1** only (`minecraft_version_range=[1.21.1]`)
- **NeoForge:** **21.1.x** (`neo_version_range=[21.1,21.2)`, compile pin `21.1.248`)
- **Fabric:** Loader `>=0.16`, Minecraft `~1.21.1` (see `fabric.mod.json`)

Minecraft / NeoForge **26.2** is tracked in [MULTI_VERSION.md](MULTI_VERSION.md) but **not** shipped. Runtime version ranges intentionally do **not** advertise 26.x until a dedicated NeoForge 26.2 build exists.

## Loaders / editions (1.21.1)

| Loader | Module | Status |
|--------|--------|--------|
| NeoForge | `:neoforge` | Full 0.3.6 set (companions, AI, behavior, etc.) |
| NeoForge CCI | `:neoforge-cci` | Same + IMC bridge (needs CCI 1.13.0 + iChunUtil 1.0.3) |
| Fabric | `:fabric` | Full 0.3.6 set |
| Fabric CCI | `:fabric-cci` | Same + Fabric CCI bridge |

## Why two version properties?

| Property | Purpose |
|----------|---------|
| `minecraft_version` / `neo_version` | Exact pair used to **compile and run** the MDK |
| `minecraft_version_range` / `neo_version_range` | What the built jar **accepts at runtime** |

NeoForge requires the compile `neo_version` to match its Minecraft pair. You cannot compile one jar against both 21.1 and 26.2 toolchains at once — use a **separate module or branch** for 26.2.

## Building for 26.2 (when porting)

1. Prefer a new module (e.g. `:neoforge-26`) or branch — do **not** retarget the 1.21.1 production modules in place.
2. Pin (check Maven for newer):
   ```properties
   minecraft_version=26.2
   neo_version=26.2.0.59
   parchment_minecraft_version=26.2
   ```
   Java **25**, unobfuscated Minecraft.
3. Fix API breakages for that line (`ModVersionCompat.isNeo26Line()` can gate shims once the module exists).
4. Ship standalone first; CCI is **unavailable** on Modrinth for 26.2 as of Aug 2026.

## Runtime helpers

`com.azscompanions.util.ModVersionCompat` exposes helpers for a *future* multi-line window. Until a 26.2 jar ships, treat production support as **1.21.1 / NeoForge 21.1.x** only:

- `isWithinSupportedWindow()`
- `isNeo21_1Line()` / `isNeo26Line()`
- `logSupportBanner()` (called during common setup)

## Optional map mods (1.21.1)

Soft-deps only — see [COMPAT.md](COMPAT.md#map-mods-minimap--world-map):

| Mod | Entity on radar | Extra |
|-----|-----------------|-------|
| Xaero Minimap / World Map | Yes (LivingEntity + icon assets) | Enable Entity Radar in Xaero |
| JourneyMap | Yes (PathfinderMob + API plugin) | Names / hide toggles via config |
| FTB Chunks | Claims overlay (not entity radar) | Existing FTB soft-compat |

## Online from LAN / Essential

Friends joining SP via Essential / e4mc / World Host / Open-to-LAN — soft-compat, no hard deps. See [COMPAT.md](COMPAT.md#online-from-lan--essential).
