# Changelog

## 0.1.0 — first public

### Breaking
- Mod id / namespace renamed from `koncompanions` → `azscompanions` (display name **Az's Companions**). Old worlds with `koncompanions:*` items/entities will not load those content ids.

### Gameplay
- Companion Charm (desert pyramid loot): summon / store; one charm per player
- Multiplayer: **1 companion per player** (UUID ownership; each player can have their own)
- First summon defaults to player name + player skin
- Follow by day; sleep in nearest bed at night (any bed)
- Customize: name, gender, Mojang skin, size/proportions (NeoForge)
- Typing a valid Minecraft username fetches that player's Mojang skin (live preview + entity sync); **Kon** applies Kon special skin
- Skin lookup waits for texture download + legacy 64×32→64×64 processing before applying `player:<uuid>` (fixes UV/Alex-Steve flicker)
- Skins are Mojang-only (no local PNG import)
- Charm store/recall persists full appearance (name, skin, gender, size, proportions, home bed, etc.)
- Charm Hello / Bye chat lines (owner only)
- Loose follow: path starts at **32**, stops at **24**, teleport at **48**; stuck-recovery no longer snaps at 8. Teleport-to-owner disabled while fighting. Special flight perk keeps its 5-block airborne leash.
- Ground potion loot: auto-pickup **beneficial only** (skip harmful + water/awkward neutrals); manually given harmful splash still handled by held-potion AI.

### Loaders
- NeoForge and Fabric jars for Minecraft 1.21.1
- **NeoForge CCI edition** (`:neoforge-cci` → `azscompanions-neoforge-cci`): hard-depends on iChun Content Creator Integration + iChunUtil; IMC bridge for stream-driven companion greet/say/wave/follow/sit/stay. Standalone NeoForge jar unchanged (no CCI required).
- **Fabric CCI edition** (`:fabric-cci` → `azscompanions-fabric-cci`): hard-depends on Fabric CCI 1.13.0 + iChunUtil 1.0.3; same IMC subjects via Fabric IMCOutcome mixin (iChunUtil Fabric has no InterModComms) plus `/azscci` CommandOutcome fallback. Standalone Fabric jar unchanged.
