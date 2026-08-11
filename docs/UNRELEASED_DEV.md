# Unreleased features (dig)

Friendly local dig of everything in the tree that is **not yet a published GitHub release**.

| | |
|---|---|
| **Pending publish** | _(none)_ — tree matches **0.4.3** |
| **Last published tag** | **v0.4.3** |
| **CHANGELOG** | `## Unreleased` empty; ship notes under `## 0.4.3` |
| **Loaders this ship** | NeoForge + Fabric **1.21.1** (+ CCI jars). NeoForge **26.2** still not shipped. |

Snapshot for local dig / release prep. Not a Modrinth/GitHub release note — do not treat as published.

---

## Shipped in 0.4.3 (reference)

- **Jindujun:** Blockbench cloud mesh + classic yellow Nimbus texture; ridden **ENCHANT** shaped particle stream only.
- **Removed** flight ki aura bubbles / motion trails (players, companions, Bits, old nimbus trail).
- **No natural despawn:** `setPersistenceRequired` + scoreboard tag `azscompanions.nodespawn` for owned companions / Bits.

---

## Still WIP / not in this publish

- **NeoForge 26.2** (`:neoforge-26`) — port continues; **no jar** in the 0.4.3 loader matrix.
- **`CompanionRecentActionHooks.java.wip`** — shared hooks draft still under a `.wip` name; live wiring is via loader event classes / mixins.
- VoiceMod TTS bridge / Simple Voice Chat entity audio emission — still not shipped (detect-only / soft-compat as before).

---

## Dig notes

- Source of truth for wording: `CHANGELOG.md` → `## 0.4.3`.
- After the next feature lands, move notes into `## Unreleased` and bump **Pending publish** above.
