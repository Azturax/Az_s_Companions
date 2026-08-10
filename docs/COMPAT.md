# Compatibility soft-deps

Optional integrations for Minecraft **1.21.1** (Fabric + NeoForge). The mod runs when none of these are installed.

---

## FTB suite (Teams / Chunks / Ranks)

Integration uses **reflection** (`FtbReflectionBridge`) — no compile-time FTB dependency. See also changelog notes under 0.3.6.

### Behavior when FTB is absent

| Feature | Without FTB |
|---------|-------------|
| Teams trust | No-op (`FtbCompatHooks.NOOP`) |
| Chunk claim checks | No-op; AI mine/place only use existing grief/blacklist rules |
| Rank gates | Treated as allowed (no permission deny) |

### Config keys

Stored under the companion AI config:

| Loader | File |
|--------|------|
| Fabric | `config/azscompanions-ai.json` → `"ftb": { … }` |
| NeoForge | `config/azscompanions-ai.toml` → `[ftb]` |

| Key | Default | Meaning |
|-----|---------|---------|
| `ftbTeamsCompat` | `true` | Same FTB team as owner → trusted for combat/interact |
| `ftbChunksAllowPresence` | `true` | Companions may walk into claims (presence OK) |
| `ftbChunksBlockInteraction` | `true` | Block AI/task break/place/containers where FTB would prevent the **owner** from editing. Alias: `ftbChunksProtect`. NeoForge also needs `respectClaimMods=true` |
| `ftbChunksAiClaim` | `false` | Allow owner AI `claim_chunk` / `unclaim_chunk` (owner quota; no steal) |
| `ftbRanksCompat` | `false` | When FTB Ranks is present, gate features via permission nodes |
| `trustSameTeamAsOwner` | `false` | Same FTB team gets full AI action trust (`OWNER` tools) |
| `permAiAsk` | `azscompanions.ai.ask` | Rank node for `/ask` and chat AI |
| `permAiActions` | `azscompanions.ai.actions` | Rank node for executing AI world actions |
| `permCci` | `azscompanions.cci` | Rank node for CCI companion outcomes |
| `permTeamfight` | `azscompanions.teamfight` | Rank node for teamfight leader spawn |
| `permSpawn` | `azscompanions.spawn` | Rank node for recruit / child / fight spawns |

Missing rank nodes **default to allow**.

### Hooked mods

| Mod ID | Role |
|--------|------|
| `ftbteams` | Same-team trust |
| `ftbchunks` | Claim presence / edit interaction / optional AI claim |
| `ftbranks` | Boolean rank gates |

### Code entry points

- Common: `com.azscompanions.compat.ftb.FtbCompat`
- NeoForge: `FtbCompatModule` via `CompatBootstrap`
- Fabric: `FabricFtbCompat` from `AzsCompanionsFabric`

---

## Map mods (minimap / world map)

Package: `com.azscompanions.compat.map`. Soft-deps only — **does not** change FTB claim logic above.

Companions are `PathfinderMob` / `MobCategory.CREATURE`, so most entity radars already track them. We add icons + JourneyMap polish.

### What works

| Mod | How companions appear | Our integration |
|-----|----------------------|-----------------|
| **Xaero's Minimap** (`xaerominimap`) | Automatic entity radar for living mobs when Entity Radar is enabled | Soft-dep + icon `assets/xaerominimap/entity/icon/definition/azscompanions/companion.json` (charm sprite) |
| **Xaero's World Map** (`xaeroworldmap`) | Same entity radar when enabled | Soft-dep documented (no separate entity API) |
| **JourneyMap** (`journeymap`) | Automatic for `PathfinderMob`; also registered as villager/NPC-style | Soft-dep plugin (`@JourneyMapPlugin` / Fabric `journeymap` entrypoint): custom icon, name, owner tooltip, config hide. compileOnly `info.journeymap:journeymap-api-*:2.0.0-1.21.1` |
| **FTB Chunks** (`ftbchunks`) | Claim / party map overlay — **not** companion entity radar | Unchanged; see FTB section. Map package stays out of claim code |

Antique Atlas / other radars: no dedicated wiring; LivingEntity scanning may still show companions.

### Config

| Loader | Where |
|--------|-------|
| NeoForge | Client config → `[map]` |
| Fabric | `config/azscompanions-map.json` |

| Key | Default | Effect |
|-----|---------|--------|
| `showOnMinimap` | `true` | JourneyMap: hide companions when false. Xaero has no public hide API (disable Entity Radar there if needed) |
| `showChildrenOnMap` | `true` | Hide child Bits on JourneyMap when false |
| `showNameOnMap` | `true` | JourneyMap label = companion chat display name |
| `showOwnerOnMap` | `true` | JourneyMap tooltip `Owner: …` |
| `mapIconColor` / `iconColorArgb` | `0xFFE91E8C` | JourneyMap dot/label tint |

### Fabric JSON example

```json
{
  "showOnMinimap": true,
  "showChildrenOnMap": true,
  "showNameOnMap": true,
  "showOwnerOnMap": true,
  "iconColorArgb": "0xFFE91E8C"
}
```

### Player tips

- **Xaero:** Minimap settings → Entity Radar → enable; configure Living / Friendly icons & names.
- **JourneyMap:** Enable mob radar; companions use the charm icon + display name when the plugin loads.
- Works with **no** map mods installed.

### Code entry points

- Common: `MapCompat`, `MapCompatSettings`, `MapCompatConfigIO`
- NeoForge: `MapCompatModule`, `compat.map.jm.JourneyMapCompanionPlugin`
- Fabric: `FabricMapCompat`, `compat.map.jm.FabricJourneyMapCompanionPlugin`

---

## Online from LAN / Essential

Soft-compat for **friends joining a singleplayer-hosted world** over the internet (or LAN). The host still runs an **integrated** Minecraft server (`isDedicatedServer=false`); joining friends are normal `ServerPlayer`s. Dedicated servers are unchanged.

### Mods checked (1.21.1 Fabric / NeoForge)

| Mod | Mod ID(s) | Role | Status with Az's Companions |
|-----|-----------|------|------------------------------|
| **Essential** (essential.gg / SparkUniverse) | `essential` | Invite friends into SP without a dedicated server | Soft-detect + hosted-MP hooks; ownership/AI/CCI work on host |
| **e4mc** | `e4mc` | Reverse tunnel for Open-to-LAN | Soft-detect; same integrated-MP path |
| **World Host** | `world-host` (`worldhost` alt) | Host SP without port forward | Soft-detect; same path |
| **LAN Server Properties** | `lanserverproperties` | Offline/hybrid LAN auth | Soft-detect; name-fallback helps UUID remaps |
| Vanilla **Open to LAN** | — | Local network publish | Detected via `server.isPublished()` / player count |

No hard dependency — the mod loads if none of these are installed.

### What already worked out of the box

- Companion **recruit / ownership by UUID** when Microsoft accounts stay consistent (typical Essential sessions).
- **AI ask / name-mention / chat listen** run on the host integrated server; joining clients do not need LM Studio or API keys when `serverLlmOnly=true` (default).
- **Packets / screens** are owner-gated by `ServerPlayer` UUID — guests manage their own companions; strangers can name-mention with limited social actions.
- **CCI** (CCI edition jar): IMC is polled on the **host client** and applied server-side to the streamer's owned companions. Friends joining do not need CCI unless they stream their own companions.

### What we fixed / added

| Issue | Fix |
|-------|-----|
| Integrated host with friends looked like “solo SP” for shared-LLM status when `serverLlmOnly=false` | `integratedMultiplayerSharedLlm` (default **true**) forces host LLM authority whenever Essential/e4mc/World Host is present, LAN is published, or remote players are online |
| Offline↔online **UUID remap** (LAN hybrid / some tunnel hosts) breaking `isOwnedBy` | Persist `OwnerName`; optional `ownerNameFallback` (default **true**, **never on dedicated**) matches profile name and heals owner UUID on login |
| No visibility that hosted MP is active | Soft-detect host mods; log once; AI status shows `[hosted MP]` |

### Config toggles

Stored with companion AI config:

| Key | Default | Meaning |
|-----|---------|---------|
| `integratedMultiplayerSharedLlm` | `true` | Essential/e4mc/LAN integrated MP → host LLM authoritative even if `serverLlmOnly=false`. Ignored on dedicated. |
| `ownerNameFallback` | `true` | On integrated hosted MP only, matching player names count as owner if UUIDs diverge; heal UUID on join. Always off on dedicated. |
| `serverLlmOnly` | `true` | Existing: host/dedicated AI config for all companions |

Fabric JSON example:

```json
{
  "serverLlmOnly": true,
  "integratedMultiplayerSharedLlm": true,
  "ownerNameFallback": true
}
```

NeoForge: same keys in `config/azscompanions-ai.toml`.

### Streamer / CCI tips

- Host the world on the **streamer PC** (Essential invite / e4mc / Open to LAN). CCI outcomes target that host's owned companions.
- Configure AI once on the host (`azscompanions-ai.json` / `.toml` + env `AZS_LLM_API_KEY`).
- Joining friends: same mod set as the host; they can recruit their own companions and use ask/name-mention. They do **not** need a local LLM.
- Prefer leaving `ownerNameFallback=true` for LAN offline quirks; turn it off if you need strict UUID-only ownership on a hosted world.

### Code entry points

- Common: `compat.hosted.IntegratedMultiplayerCompat`, `HostedWorldMods`, `PlayerIdentityCompat`
- NeoForge: `compat.hosted.HostedWorldCompatModule` via `CompatBootstrap`
- Fabric: `FabricHostedWorldCompat` from `AzsCompanionsFabric`

---

## Fancy Animations / Fresh Animations / EMF+ETF

Soft client render compat for CEM / animated entity packs (no hard dep). **Continuity** is block CTM and does not affect companions.

| Pack family | What it needs | How companions pick it up |
|-------------|---------------|---------------------------|
| **Fresh Animations** (mobs) | OptiFine CEM **or** [EMF](https://modrinth.com/mod/entity-model-features) + [ETF](https://modrinth.com/mod/entitytexturefeatures) | **Mob forms** proxy through vanilla chicken/zombie/… renderers, so pack `textures/entity/...` + `optifine/cem/*.jem` apply as on real mobs |
| **Fancy Player Animations** / **Fresh Moves** (players) | EMF + ETF; player `player.jem` / `player_slim.jem` | Targets the **player** entity path, not `azscompanions:companion`. Player-form companions keep `FeminineCompanionModel` (extra bust part) — they do **not** auto-load `player.jem` |
| ETF emissives / skin transparency / `.mcmeta` frames | Translucent entity layer | Player-form uses `RenderType.entityTranslucent` (config). Bundled ETF properties set `entityRenderLayerOverride=translucent` for companion texture paths |

### Texture paths

| Skin source | Resource path | Pack override |
|-------------|---------------|---------------|
| Default Kon | `azscompanions:textures/entity/companion/kon.png` | Override that mod path (or `kon_e.png` emissive beside it) |
| Custom resource path | Whatever you set in Customize | Same namespace/path in a resource pack |
| `player:<uuid>` Mojang skin | Dynamic `minecraft:skins/...` | Pack cannot replace CDN skins; ETF player-skin features still apply when translucency is on |
| Vanilla player examples | `minecraft:textures/entity/player/wide\|slim/*.png` | Only if the companion skin path points there |

Vanilla mob textures used by mob forms (`textures/entity/zombie/zombie.png`, etc.) are unchanged — Fresh Animations animates those via the proxy renderer.

### Player-form CEM (optional advanced)

EMF looks for modded models roughly at:

`assets/azscompanions/optifine/cem/modded/azscompanions/companion.jem`

(and a slim variant if you use one). To reuse Fancy Player Animations / Fresh Moves, copy their `player.jem` / `player_slim.jem` into that path in a **personal** add-on pack (do not redistribute third-party JEMs). Expect minor mismatches from the feminine bust part; EMF log “print model details” helps verify part names.

### Config

| Loader | Where |
|--------|-------|
| NeoForge | Client config → `[fancyAnim]` |
| Fabric | `config/azscompanions-fancyanim.json` |

| Key | Default | Effect |
|-----|---------|--------|
| `translucentPlayerSkins` | `true` | Player-form + cape use translucent buffers (player-like; safe without packs) |
| `syncMobFormUuid` | `true` | Mob-form proxies share the companion UUID for stable ETF random/emissive picks |

### Fabric JSON example

```json
{
  "translucentPlayerSkins": true,
  "syncMobFormUuid": true
}
```

### Player tips

- Install **EMF + ETF** (or OptiFine) before enabling Fancy / Fresh packs.
- Fresh Moves: ETF → Allow skin transparency → All skins; EMF → Prevent first person hand animation On.
- Without packs, defaults still look like vanilla translucent player skins (no crash, no missing textures).

### Code entry points

- Common: `FancyAnimCompat`, `FancyAnimSettings`, `FancyAnimConfigIO`
- NeoForge: `FancyAnimCompatModule`, `FancyAnimClientBridge`; renderers `CompanionRenderer` / `CompanionMobFormRenderer`
- Fabric: `FabricFancyAnimCompat`; renderers `FabricCompanionRenderer` / `CompanionMobFormRenderer`
