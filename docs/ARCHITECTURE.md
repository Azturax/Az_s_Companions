# Architecture notes

Az's Companions (mod id `azscompanions`). Public repo: [Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions).

## Authority

- Task execution, inventory mutation, combat, block breaking, and recruitment are **server-side only**.
- Clients render entities/GUIs and play voice/subtitles; they send intent packets (`RecruitCompanionPacket`, `CompanionCommandPacket`).
- CCI IMC outcomes arrive client-side; CCI edition bridges forward actions to the server.

## Companion data

1. Built-in Kon fallback (`BuiltinCompanions`)
2. Datapack JSON under `data/<ns>/companions/*.json` (reload listener)
3. External mods via `CompanionApi.registerCompanion` during common setup

## Tasks

`TaskQueue` holds a priority-ordered deque. Each `CompanionTask` ticks on the server thread. Never touch world state from async pathfinding callbacks without marshalling back to the server thread.

## Safety

`ProtectionHelper` + `ClaimProtectionApi` + datapack blacklists prevent griefing. Companions never damage owners, trusted players, or `OwnableEntity` pets of those players.

## Companion chunk tickets

While summoned, each companion entity (primary and child Bits) holds a loader chunk ticket for its current chunk so AI/follow/sleep keep ticking when the player is away (same dimension). NeoForge uses `TicketController`; Fabric uses a non-expiring `TicketType`. Config: `companionChunkLoading` / `maxForcedChunksPerPlayer`. Distinct from FTB chunk claims. On owner **logout**, companions are parked (despawned + NBT saved) and tickets released; they restore near the player on **login**.

## Companion logout persistence

Owned living companions (non-child roots; Bits folded via `StoredChildren`) are removed from the world when the owner disconnects and restored on join near the player. Storage: NeoForge `Player#getPersistentData` list `azscompanions.LogoutCompanions`; Fabric overworld SavedData `azscompanions_logout_companions`. Charm-bound companions also get `StoredCompanion` + `LogoutParked` so a missing bound entity does not trigger recruit-replacement. Manual charm dismiss (no `LogoutParked`) stays stored until the player summons.

**Dimension travel is not logout.** On `PlayerChangedDimension` / Fabric `AFTER_PLAYER_CHANGE_WORLD` (any `ResourceKey`, vanilla or modded), companions are teleported into the destination near the owner — they do not park. Persona/model are global per world save; first-create onboarding is not re-opened.

## Companion AI recent-action chatter

Owned companions with **Idle chat** ON can react to a short-lived per-owner event buffer (explosions, darkness, notable finds ~once per 14 days real-time, craft-ready, crafts, damage). Loaders record events; entity ambient ticks consume reactive ones early (~25s speak cooldown) and always ground idle prompts in recent context. See [COMPANION_AI.md](COMPANION_AI.md#idle-ambient-chat).

## Compatibility

Prefer NeoForge capabilities (`ItemHandler`, energy/fluid later), item/block tags, and optional modules under `compat.optional.*` with `ModList.isLoaded` — no hard deps. FTB Teams/Chunks/Ranks: reflection soft-deps — [COMPAT.md](COMPAT.md).

## Pathfinding / home blocks

Baritone is **not** a hard dependency in 0.1.x. Prefer a light foundation later: owned **home beacon** + **deposit box** blocks that companions recognize via UUID ownership, rather than shipping Baritone in this pass.

## Voice

`VoiceService` (server) → `CompanionDialoguePacket` → `ClientVoiceController` (owner text + optional Minecraft sound cues). Optional LLM/MCP **text** chat: [COMPANION_AI.md](COMPANION_AI.md). Optional **Simple Voice Chat** soft-detect (`voicechat`, ref `voicechat-neoforge-1.21.1-2.6.21.jar`) + VoiceMod awareness — no TTS bridge: [COMPAT.md](COMPAT.md#proximity-voice-simple-voice-chat--voicemod).
