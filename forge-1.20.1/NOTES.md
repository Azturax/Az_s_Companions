# Az's Companions - Forge 1.20.1

NeoForge has NO 1.20.1 line. Targets Forge 47.4.22 via net.neoforged.moddev.legacyforge.

## Build
- `:forge-1.20.1:jar` -> `build/libs/azscompanions-forge-1.0.4+1.20.1.jar`
- `:forge-cci-1.20.1:jar` -> CCI edition (CCI 1.13.0 `nNaAlKHI` + iChunUtil 1.0.3 `W6d0pCyu`)
- Java 17

## Honest omissions
Same vanilla gaps as Fabric 1.20.1 NOTES, plus:
- No mixin-based recent-action hooks (Forge uses event bus equivalents)
- JourneyMap soft-dep omitted (JVM 21+ API jar)