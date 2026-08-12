# Unreleased features (dig)

Friendly local dig of everything in the tree that is **not yet a published GitHub release**.

| | |
|---|---|
| **Pending publish** | _(none — last ship was **v0.4.7**)_ |
| **Last published tag** | **v0.4.7** |
| **CHANGELOG** | Notes under `## Unreleased` (empty after 0.4.7) |
| **Loaders this ship** | NeoForge + Fabric **1.21.1** (+ CCI). NeoForge **26.2** still not shipped. |

Snapshot for local dig / release prep. Not a Modrinth/GitHub release note — do not treat as published.

---

## In Unreleased (not published)

_(empty)_

---

## Shipped in 0.4.7 (reference)

- **Removed Jindujun / Flying Nimbus** entirely (entity, renderer, particles, textures, registration).
- **Removed Jindujun Whistle** (item, creative tab, Trail Ruins loot, lang).
- Treasure loot now Companion Charm only under `world.enableLoot`.

## Shipped in 0.4.6 (reference)

- **Jindujun sit gap:** `RIDER_Y_OFFSET` `0.32×SCALE` (~0.80) → `0.22×SCALE` (~0.55); feet slightly into cream deck.
- **Jindujun spin:** yaw sync once in `travel` from rider look; no `*O` overwrite / no second sync in `tick`.

## Shipped in 0.4.5 (reference)

- **Jindujun sit gap:** scale wraps Blockbench feet pivot; `RIDER_Y_OFFSET` ~0.80 so rider sits flush.
- **Jindujun enchant stream:** sparser, further behind cloud at foot height; no upward glyph blast into rider face.

## Shipped in 0.4.4 (reference)

- **Jindujun polish:** 2.5× scale, rider yaw sync, sit-on-cloud offset, 56s idle despawn, cloud-only `ENCHANT` stream.
- **ITEM_FIND reactive chatter:** ~14-day real-time per-owner cooldown.
- **Join-time LLM consent** remembered once per server key (`azscompanions-ai-join-consent.json`).

## Shipped in 0.4.3 (reference)

- **Jindujun:** Blockbench cloud mesh + classic yellow Nimbus texture; ridden **ENCHANT** shaped particle stream only.
- **Removed** flight ki aura bubbles / motion trails (players, companions, Bits, old nimbus trail).
- **No natural despawn:** `setPersistenceRequired` + scoreboard tag `azscompanions.nodespawn` for owned companions / Bits.

---

## Still WIP / not in this publish

- **NeoForge 26.2** (`:neoforge-26`) — port continues; **no jar** in the 0.4.7 loader matrix.
- **`CompanionRecentActionHooks.java.wip`** — shared hooks draft still under a `.wip` name; live wiring is via loader event classes / mixins.
- VoiceMod TTS bridge / Simple Voice Chat entity audio emission — still not shipped (detect-only / soft-compat as before).
