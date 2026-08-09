# Version compatibility

Az's Companions declares support for:

- **Minecraft:** `1.21.1` → `26.2` (Maven range `[1.21.1,26.3)`)
- **NeoForge:** `21.1.x` → `26.2.x` (Maven range `[21.1,26.3)`)
- **Fabric:** Loader `>=0.16`, Minecraft `~1.21.1` (see `fabric.mod.json`; widen when shipping dedicated 26.2 Fabric builds)

## Loaders

| Loader | Module | Status |
|--------|--------|--------|
| NeoForge | `:neoforge` | Full feature set |
| Fabric | `:fabric` | Core recruit / follow / gather / GUIs / Kon |

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
