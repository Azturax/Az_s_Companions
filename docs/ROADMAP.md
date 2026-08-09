# Az's Companions — Development Roadmap

Public repo: [Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions) · Released: [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0)

All characters remain explicitly adult, wholesome, and non-sexual across every phase.

## Phase 1 — MVP (shipped as 0.1.0)

**Goal:** Charm companion, follow/wander/sleep, defend, hands/potions, customize, CCI editions.

- [x] NeoForge Gradle project (1.21.1)
- [x] `CompanionEntity` with ownership, trust, permissions, home
- [x] Data-driven companion definitions (`data/*/companions/*.json`)
- [x] Default companion: Kon (dialogue + personality + skin path)
- [x] Companion Selection screen + craftable selector item + `/azscompanions`
- [x] Management / inventory / radial command menus
- [x] Task queue (priority, pause/resume, cancel, reports)
- [x] Vanilla tasks: follow, stay, guard, gather, farm, chop, mine, combat, collect, deposit, build, craft, machine, sleep, home
- [x] Datapack tags for crops, tools, food, containers, blacklist
- [x] Config (common/server/client)
- [x] Debug commands (`/kondebug`)
- [ ] Polish Kon player-model texture from final art (placeholder PNG shipped)
- [ ] GameTest templates under `data/azscompanions/structure`
- [ ] Exact server-wide companion counting via SavedData

## Phase 2 — Expanded automation

**Goal:** Reliable long-running work loops with storage and blueprints.

- [ ] Schematic/blueprint serializer (structure NBT / custom plan format)
- [ ] Multi-step farm loops: harvest → replant → collect → deposit → report missing seeds
- [ ] Tree felling with leaf cleanup and sapling replant
- [ ] Strip-mining / branch mining with torch placement
- [ ] Item filters (whitelist/blacklist GUI)
- [ ] Durability policies: avoid break, return damaged, use backup, request repair
- [ ] Patrol routes and area markers
- [ ] Stuck detection improvements + hazard avoidance (lava, cliffs, powder snow)
- [ ] Optional hunger/stamina tuning presets

## Phase 3 — Mod compatibility

**Goal:** Broad soft integrations through capabilities, tags, and optional modules.

- [x] Capability-based container API + claim bridge stubs
- [x] Optional module package structure (storage/tech/farming/voicechat)
- [x] Public `CompanionApi` for tasks, machines, workstations, item rules
- [ ] Create / Mekanism / AE2 / Refined Storage handlers
- [ ] Farmer's Delight & other crop mods via tags
- [ ] Backpack mod slot bridges
- [ ] FTB Chunks / GriefPrevention / Open Parties claim integrations
- [ ] Compatibility report command with per-mod status
- [ ] JEI/EMI recipe transfer into CraftTask

## Phase 4 — Voice integration

**Goal:** Expressive client-side voice without proprietary lock-in.

- [x] Voice abstraction (`VoiceService`, profiles, subtitles, sound events)
- [x] Optional Voicemod-compatible external bridge (no SDK bundled)
- [x] TTS adapter hook
- [ ] Custom Kon voice pack assets
- [ ] Per-line subtitle timing
- [ ] Simple Voice Chat proximity emission (optional)
- [ ] Privacy mode (owner-only / party-only / silent)

## Testing matrix (target)

| Scenario | Phase | Status |
|----------|-------|--------|
| Multiplayer ownership & trust | 1 | GameTest stub |
| Protected / blacklisted blocks | 1 | GameTest stub |
| Tool selection | 2 | Pending world test |
| Crop harvest + replant | 1–2 | Task + GameTest stub |
| Chest deposit via item handler | 1 | Implemented |
| Crafting at workstation | 1–2 | MVP ingredient consume |
| Machine fueling | 1 | Vanilla furnace handler |
| Stuck pathfinding teleport | 1 | Config + entity tick |
| Task cancellation | 1 | Unit + GameTest stub |
