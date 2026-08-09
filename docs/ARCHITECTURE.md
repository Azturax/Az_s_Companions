# Architecture notes

## Authority

- Task execution, inventory mutation, combat, block breaking, and recruitment are **server-side only**.
- Clients render entities/GUIs and play voice/subtitles; they send intent packets (`RecruitCompanionPacket`, `RadialCommandPacket`).

## Companion data

1. Built-in Kon fallback (`BuiltinCompanions`)
2. Datapack JSON under `data/<ns>/companions/*.json` (reload listener)
3. External mods via `CompanionApi.registerCompanion` during common setup

## Tasks

`TaskQueue` holds a priority-ordered deque. Each `CompanionTask` ticks on the server thread. Never touch world state from async pathfinding callbacks without marshalling back to the server thread.

## Safety

`ProtectionHelper` + `ClaimProtectionApi` + datapack blacklists prevent griefing. Companions never damage owners, trusted players, or `OwnableEntity` pets of those players.

## Compatibility

Prefer NeoForge capabilities (`ItemHandler`, energy/fluid later), item/block tags, and optional modules under `compat.optional.*` with `ModList.isLoaded` — no hard deps.

## Voice

`VoiceService` (server) → `CompanionDialoguePacket` → `ClientVoiceController` (sounds/subtitles/TTS/Voicemod bridge). Voicemod is never required.
