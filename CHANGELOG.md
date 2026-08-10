# Changelog

## 0.2.1

Release: [v0.2.1](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.2.1) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### UI
- **Companion inventory layout:** left vertical armor column (helmet→boots) with vanilla empty armor icons; 3×9 storage to the right; companion hotbar/tools on a separated strip still inside the companion panel; player inventory below with normal spacing (no floating equipment row in the gap)

### AI / teleport
- **Happy Ghast–inspired Wander:** slow leisurely strolls (speed 0.55), rare starts, linger pauses between legs, soft looks; roam radius 3–16 (owner) / 2–10 (home bed); if outside radius **walks back** — never teleports for that
- **Wander / home-idle no short-range snaps:** removed home-bed leash teleport at ~8 blocks (`PREFERRED+2`); any teleport-to-owner now requires ≥ **24** blocks (`MIN_TELEPORT_DISTANCE`)
- **Wander mode teleports:** zero FollowGoal / stuck / ground-leash teleports — only the home-bed rule (owner >35 from bed) may teleport, and only if also ≥24 from owner
- Special perk land-snap no longer teleports when floating while already close (<24)

### Loaders / editions (jar matrix)
Jar names use `0.2.1+<minecraft>` so the game version is visible in the filename.

| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.2.1+1.21.1.jar` | `azscompanions-fabric-0.2.1+1.21.1.jar` | `azscompanions-neoforge-cci-0.2.1+1.21.1.jar` | `azscompanions-fabric-cci-0.2.1+1.21.1.jar` |

**Shipped this release:** four jars for Minecraft **1.21.1** (NeoForge + Fabric standalone and CCI). See `docs/MULTI_VERSION.md`.

### Build / metadata
- Mod version **0.2.1**; published archives tagged `+1.21.1`

## 0.2.0

Release: [v0.2.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.2.0) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Gameplay
- **Kon-only gating:** sleep purr (`CAT_PURR`), Kon Bed sleep priority, Kon skin easter egg + one-time Kon Bed grant apply only when the companion display name equals `Kon` (case-insensitive). Non-Kon companions use generic sleep/bed/skin defaults. UUID special perks (ears, fly/glow, Wiggly) unchanged.

### Loaders / editions (jar matrix)
Jar names use `0.2.0+<minecraft>` so the game version is visible in the filename.

| Minecraft | NeoForge | Fabric | NeoForge CCI | Fabric CCI |
|-----------|----------|--------|--------------|------------|
| **1.21.1** | `azscompanions-neoforge-0.2.0+1.21.1.jar` | `azscompanions-fabric-0.2.0+1.21.1.jar` | `azscompanions-neoforge-cci-0.2.0+1.21.1.jar` | `azscompanions-fabric-cci-0.2.0+1.21.1.jar` |
| **1.20.1** | *not shipped — no NeoForge 20.1 on releases Maven; Forge port deferred* | *deferred — API backport* | *CCI: no NeoForge build (Forge-only on Modrinth)* | *CCI available (see table) but Az port deferred* |
| **26.1.2** | *deferred — unobfuscated MC + Java 25 rewrite* | *deferred* | *CCI: none on Modrinth* | *CCI: none on Modrinth* |
| **26.2** | *deferred — unobfuscated MC + Java 25 rewrite* | *deferred* | *CCI: none on Modrinth* | *CCI: none on Modrinth* |

### CCI / iChunUtil dependency pins (Modrinth lookup)

| Minecraft | Loader | CCI | Modrinth id | iChunUtil | Modrinth id | Used in 0.2.0? |
|-----------|--------|-----|-------------|-----------|-------------|----------------|
| **1.21.1** | NeoForge | **1.13.0** | `AySbAgcO` | **1.0.3** | `OvIyyNh4` | Yes (`neoforge-cci`) |
| **1.21.1** | Fabric | **1.13.0** | `PERd6IT9` | **1.0.3** | `gfAOoiwe` | Yes (`fabric-cci`) |
| **1.20.1** | Fabric | **1.13.0** | `7tk12xkN` | **1.0.3** | `JjEWQx5u` | No (port deferred) |
| **1.20.1** | Forge | **1.13.0** | `nNaAlKHI` | **1.0.3** | `W6d0pCyu` | No (no NeoForge CCI; Forge module deferred) |
| **1.20.1** | NeoForge | — | none | — | none | N/A |
| **26.1.2** | any | — | none | — | none | N/A — ship standalone only when ported |
| **26.2** | any | — | none | — | none | N/A — ship standalone only when ported |

**Shipped this release:** four jars for Minecraft **1.21.1** (NeoForge + Fabric standalone and CCI). See `docs/MULTI_VERSION.md`.

### Build / metadata
- Mod version **0.2.0**; published archives tagged `+1.21.1`
- NeoForge / Minecraft dependency ranges tightened to **1.21.1 / NeoForge 21.1.x** (no longer advertise untested 26.x binary compat)

## 0.1.1

Release: [v0.1.1](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.1) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Fixes
- **Dedicated NeoForge server:** `RegisterPayloadHandlersEvent` no longer classloads client GUI (`Screen`) via `OpenCompanionCreatorPacket` / `OpenCompanionMenuPacket` / `CompanionDialoguePacket` handlers — S2C handlers live in `ClientNetworkHandlers` (client dist only)

### UX / commands
- Shift+RMB **Companion Menu** (shared on NeoForge + Fabric): Customize · **Command** (Follow / Stay / Wander) · Inventory — server packets only
- Removed V-key **radial** command UI (and related menus/packets/keybinds); Command lives in the companion menu
- Clearer Follow / Stay / Wander AI (no idle free-roam that ignores commands)
- Ownership denial message for non-owners; inventory / command / customize stay owner-gated
- Inventory: backpack + distinct armor/tool equipment strip
- Removed unused Kon card/portrait GUI textures (mod icon uses companion charm)
- Feed with edible food: consumes 1, small heal, hearts + cheer (not placed in hands)
- Sleeping companions softly purr (`CAT_PURR`) every ~5s server-side (no sleep-skin texture override)
- Charm Hello / Bye lines toggleable via NeoForge config
- Scaled companions step up **1 full block** at any body size (`STEP_HEIGHT` 1.0 + `JUMP_STRENGTH` 0.42)
- Fabric customize / creator parity improvements (appearance draft, skin lookup, shared menu screens)

### Home bed / follow
- **Home-bed proximity (35 blocks, configurable on NeoForge):** near bed → home-idle; owner farther than 35 from bed → teleport + follow. Stay ignores the auto rule; Wander strolls near the bed until the owner leaves radius
- Night sleep **prefers Kon Bed**, then home bed; leave bed if owner moves far (~35) with wake cooldown to avoid thrashing
- Follow bands: personal space **2**, preferred **~6**, start **10**, stop **5**, teleport **48**; home-bed radius **35**
- Owner **explore vs idle** still used for soft stroll when no home bed is set

### Loaders / editions
- Four jars for Minecraft 1.21.1: NeoForge + Fabric **standalone**, NeoForge + Fabric **CCI** (same dependency story as 0.1.0)

## 0.1.0 — first public

Release: [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0) · Repo: [Az_s_Companions](https://github.com/Azturax/Az_s_Companions)

### Breaking
- Mod id / namespace renamed from `koncompanions` → `azscompanions` (display name **Az's Companions**). Old worlds with `koncompanions:*` items/entities will not load those content ids.

### Gameplay
- Companion Charm (desert pyramid loot): summon / store; **one companion per player**
- First summon defaults to player name + player skin
- Follow by day; night sleep in nearest bed (any bed / Kon Bed)
- Owner **explore vs idle**: exploring → loose follow; standing still (~2.5s) → free wander **24–40**, **no teleport / no approach**
- Loose follow bands: personal space **2**, comfort stroll **2–12**, preferred **~6**, start **32**, stop **8**, teleport **48** (exploring only, never while fighting)
- Environmental hazard immunity: fall, cactus, sweet berry bush, drowning, in-wall, campfire (still take combat damage)
- **Combat:** defend living attackers of the owner (ignores environmental damage); SIT/sleep suppress combat targeting
- **Hands:** give items into main → offhand (swap if both full); empty-hand take
- **Potions:** ground auto-pickup **beneficial only** (skip harmful + water/awkward neutrals); manually given harmful splash thrown at enemies
- Customize (NeoForge): name, gender, Mojang skin, size/proportions; **Done** saves, Cancel discards
- Typing a valid Minecraft username fetches that player's Mojang skin (live preview + entity sync); **Kon** applies Kon special skin + one-time Kon Bed
- Skin lookup waits for texture download + legacy 64×32→64×64 processing before applying `player:<uuid>`
- Skins are Mojang-only (no local PNG import)
- Charm store/recall persists appearance (name, skin, gender, size, proportions, home bed, etc.)
- Charm Hello / Bye chat lines (owner only)
- UUID-gated **special player perks** (flight follow / glow / Kon ears) for designated owners
- Kon ears cosmetic on UUID `42901453-b2b5-4d95-9b7b-e0ed40da504f` (client render layer; meow nametag removed)
- SIT/STAY (CCI modes) suppress wander/follow movement

### Loaders / editions
- Four jars for Minecraft 1.21.1: NeoForge + Fabric **standalone**, NeoForge + Fabric **CCI**
- **NeoForge CCI** (`azscompanions-neoforge-cci`): hard-depends on CCI **1.13.0** + iChunUtil **1.0.3**; IMC bridge for `companion_say` / `greet` / `wave` / `follow` / `sit` / `stay`
- **Fabric CCI** (`azscompanions-fabric-cci`): same subjects via IMCOutcome mixin (iChunUtil Fabric has no InterModComms) plus `/azscci` CommandOutcome fallback
- Standalone jars unchanged (no CCI required); never install standalone + CCI together

### Deferred / foundation
- **Baritone** pathfinding not bundled this pass — prefer future home beacon + owned deposit box foundation
- Auto-equip tools config exists (default **off**); full auto-equip logic deferred
- Sharing companions with other players deferred (ownership UUID hooks left extensible)
