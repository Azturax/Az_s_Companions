# Changelog

## Unreleased

- Owner **explore vs idle**: exploring → follow; standing still (~2.5s) → wander without teleport
- Environmental hazard immunity: fall, cactus, sweet berry bush, drowning, in-wall, campfire (still take combat damage)

## 0.1.0 — first public

Release: [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Breaking
- Mod id / namespace renamed from `koncompanions` → `azscompanions` (display name **Az's Companions**). Old worlds with `koncompanions:*` items/entities will not load those content ids.

### Gameplay
- Companion Charm (desert pyramid loot): summon / store; **one companion per player**
- First summon defaults to player name + player skin
- Follow by day; casual **wander** near owner (8–16 blocks) when idle in FOLLOW; night sleep in nearest bed (any bed / Kon Bed)
- Loose follow: path start **32**, stop **24**, teleport **48**; no teleport while fighting; stuck-recovery no longer snaps at 8
- **Combat:** defend living attackers of the owner (ignores environmental damage); SIT/sleep suppress combat targeting
- **Hands:** give items into main → offhand (swap if both full); empty-hand take
- **Potions:** ground auto-pickup **beneficial only** (skip harmful + water/awkward neutrals); manually given harmful splash thrown at enemies
- Customize (NeoForge): name, gender, Mojang skin, size/proportions; **Done** saves, Cancel discards
- Typing a valid Minecraft username fetches that player's Mojang skin (live preview + entity sync); **Kon** applies Kon special skin + one-time Kon Bed
- Skin lookup waits for texture download + legacy 64×32→64×64 processing before applying `player:<uuid>`
- Skins are Mojang-only (no local PNG import)
- Charm store/recall persists appearance (name, skin, gender, size, proportions, home bed, etc.)
- Charm Hello / Bye chat lines (owner only)
- UUID-gated **special player perks** (flight follow / glow / nametag) for designated owners
- SIT/STAY (CCI modes) suppress wander/follow movement

### Loaders / editions
- Four jars for Minecraft 1.21.1: NeoForge + Fabric **standalone**, NeoForge + Fabric **CCI**
- **NeoForge CCI** (`azscompanions-neoforge-cci`): hard-depends on CCI **1.13.0** + iChunUtil **1.0.3**; IMC bridge for `companion_say` / `greet` / `wave` / `follow` / `sit` / `stay`
- **Fabric CCI** (`azscompanions-fabric-cci`): same subjects via IMCOutcome mixin (iChunUtil Fabric has no InterModComms) plus `/azscci` CommandOutcome fallback
- Standalone jars unchanged (no CCI required); never install standalone + CCI together
