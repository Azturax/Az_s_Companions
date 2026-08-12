# Unreleased features (dig)

Friendly local dig of everything in the tree that is **not yet a published GitHub release**.

| | |
|---|---|
| **Pending publish** | _(none — last ship was **v1.0.0**)_ |
| **Last published tag** | **v1.0.0** |
| **CHANGELOG** | Notes under `## Unreleased` (empty after 1.0.0) |
| **Loaders this ship** | NeoForge + Fabric **1.21.1** (+ CCI). NeoForge **26.2** still not shipped. |

Snapshot for local dig / release prep. Not a Modrinth/GitHub release note — do not treat as published.

---

## In Unreleased (not published)

_(empty)_

---

## Shipped in 1.0.0 (reference)

- First stable **1.0.0** product release for MC **1.21.1** (four jars).
- Docs/copy polish; teamfight hint uses `/az`; WIP shared-hooks draft quarantined under `docs/dev/`.
- Jindujun / whistle remain removed (as of 0.4.7).

## Still WIP / not in this publish

- **NeoForge 26.2** (`:neoforge-26`) — port continues; **no jar** in the 1.0.0 loader matrix. Some container/capability paths are still stubbed.
- **`docs/dev/CompanionRecentActionHooks.java.wip`** — shared hooks draft; live wiring is via loader event classes / mixins.
- VoiceMod TTS bridge / Simple Voice Chat entity audio emission — still not shipped (detect-only / soft-compat as before).
