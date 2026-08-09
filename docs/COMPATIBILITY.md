# Version compatibility

Az's Companions ([repo](https://github.com/Azturax/Az_s_Companions), [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0)) declares support for:

- **Minecraft:** `1.21.1` → `26.2` (Maven range `[1.21.1,26.3)`)
- **NeoForge:** `21.1.x` → `26.2.x` (Maven range `[21.1,26.3)`)
- **Fabric:** Loader `>=0.16`, Minecraft `~1.21.1` (see `fabric.mod.json`)

## Loaders / editions

| Loader | Module | Status |
|--------|--------|--------|
| NeoForge | `:neoforge` | Full 0.1.0 set (charm, customize, follow/wander/sleep, defend, hands, potions) |
| NeoForge CCI | `:neoforge-cci` | Same + IMC bridge (needs CCI 1.13.0 + iChunUtil 1.0.3) |
| Fabric | `:fabric` | Core 0.1.0 (charm, follow/wander/sleep, defend, hands, potions; Shift opens inventory) |
| Fabric CCI | `:fabric-cci` | Same + IMCOutcome mixin + `/azscci` (needs Fabric CCI 1.13.0 + iChunUtil 1.0.3) |

## Why two version properties?

| Property | Purpose |
|----------|---------|
| `minecraft_version` / `neo_version` | Exact pair used to **compile and run** the MDK |
| `minecraft_version_range` / `neo_version_range` | What the built jar **accepts at runtime** |

NeoForge requires the compile `neo_version` to match its Minecraft pair. You cannot compile one jar against both 21.1 and 26.2 toolchains at once.

## Building for 26.2

1. Set in `gradle.properties` (example — use a published NeoForge 26.2 build):
   ```properties
   minecraft_version=26.2
   neo_version=26.2.0.53-beta
   parchment_minecraft_version=26.2
   ```
2. Fix any API breakages for that line (`ModVersionCompat.isNeo26Line()` can gate shims).
3. `./gradlew build`

The runtime ranges already allow both lines; publish separate jars if bytecode diverges.

## Runtime helpers

`com.azscompanions.util.ModVersionCompat` exposes:

- `isWithinSupportedWindow()`
- `isNeo21_1Line()` / `isNeo26Line()`
- `logSupportBanner()` (called during common setup)
