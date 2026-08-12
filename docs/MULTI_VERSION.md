# Multi-version targets

Az's Companions product line is developed on **Minecraft 1.21.1** (NeoForge + Fabric, standalone + CCI). Additional Minecraft / loader lines are tracked here.

**Feature rule:** every shipped jar must carry the **current product feature set** from main (AI, inventory, Wiggly/UUID perks, dimension follow, gifts, loot config, logout park, no Jindujun/whistle, etc.). Omit a feature only when the MC/loader API makes it impossible — document omissions in release notes. Do **not** ship feature-stripped stubs as production jars.

## Loader ↔ Minecraft mapping (research Aug 2026)

| User / target request | Minecraft | NeoForge | Fabric | CCI | Status |
|-----------------------|-----------|----------|--------|-----|--------|
| Production LTS | **1.21.1** | **21.1.248** | Fabric API `0.116.15+1.21.1` | **Yes** (1.13.0) | **Shipped** |
| Latest CCI MC | **1.21.5** | **21.5.98** | Fabric API `0.128.2+1.21.5` | **Yes** (1.13.0) | **Blocked** — full forward port required |
| `1.20.1` | **1.20.1** | **None** in NeoForged Maven | Fabric API `0.92.11+1.20.1` | Fabric **Yes** / NeoForge **No** / Forge **Yes** | **Blocked** — backport + no NeoForge |
| NeoForge `26.1.2` | **26.1.2** | **26.1.2.95** | Fabric API exists (`0.155.x+26.1.2`) | **None** | **Blocked** — no module; Java 25 / unobfuscated |
| NeoForge `26.2` | **26.2** | **26.2.0.59** | Fabric API `0.156.0+26.2` | **None** | **WIP** `:neoforge-26` — compiles with stubs; **not shipped** |

### NeoForge calver notes

Per [NeoForged versioning](https://docs.neoforged.net/docs/gettingstarted/versioning/):

| NeoForge line | Minecraft | Notes |
|---------------|-----------|-------|
| `21.1.x` | 1.21.1 | LTS production |
| `21.5.x` | 1.21.5 | Latest CCI-capable NeoForge line |
| `26.1.2.x` | **26.1.2** | Stable calver drop (Java 25, unobfuscated) |
| `26.2.0.x` | **26.2** | Second 2026 drop (Java 25, unobfuscated) |

Do **not** confuse NeoForge **26.2** with Minecraft **1.21.x**, or NeoForge **21.1** with **21.5**.

There is **no** NeoForge `20.1.x` release. For MC **1.20.1**, Forge `47.x` is the supported Forge-family loader; NeoForge starts cleanly at 1.20.2+.

## CCI + iChunUtil pins (Modrinth, Aug 2026)

| Minecraft | Loader | CCI | CCI Modrinth id | iChunUtil | iChunUtil Modrinth id |
|-----------|--------|-----|-----------------|-----------|------------------------|
| **1.21.1** | NeoForge | 1.13.0 | `AySbAgcO` | 1.0.3 | `OvIyyNh4` |
| **1.21.1** | Fabric | 1.13.0 | `PERd6IT9` | 1.0.3 | `gfAOoiwe` |
| **1.21.5** (latest CCI MC) | NeoForge | 1.13.0 | `WRDFe2RG` | 1.0.7 | `Hrl6YCrv` |
| **1.21.5** | Fabric | 1.13.0 | `5wvklhb4` | 1.0.7 | `BEq7Tobw` |
| **1.20.1** | Fabric | 1.13.0 | `7tk12xkN` | 1.0.3 | `JjEWQx5u` |
| **1.20.1** | Forge | 1.13.0 | `nNaAlKHI` | 1.0.3 | `W6d0pCyu` |
| **1.20.1** | NeoForge | **none** | — | **none** | — |
| **26.1.2 / 26.2** | any | **none** | — | **none** | — |

Do **not** reuse Modrinth version ids across Minecraft versions.

## Port readiness

### 1.21.1 (shipped)

| Gate | Status |
|------|--------|
| `:common` / `:neoforge` / `:fabric` / CCI editions | **Yes** |
| Unit tests (`:common:test`) | **Pass** |
| Feature parity with product 1.0.3 | **Yes** |

### NeoForge 26.2 (`:neoforge-26`)

| Gate | Status |
|------|--------|
| Compiles on Java 25 | Locally verified previously |
| Feature parity with 1.0.3 | **No** — renderer / mob-form / Kon ears / bed mesh / GLM / stats UI / some events still stubbed |
| CCI | **None** on Modrinth |

**Decision:** do **not** ship until stubs are replaced with real gameplay parity (or release notes list every omission and QA accepts them). Prefer fixing render/API gaps over publishing a hollow jar.

### NeoForge 26.1.2

No module yet. Same Java 25 / unobfuscated / AvatarRenderState migration class as 26.2. Plan: clone `:neoforge-26` → `:neoforge-26-1` with `neo_26_1_version=26.1.2.95` after 26.2 parity is honest. CCI: none.

### Minecraft 1.21.5 (latest CCI)

Needs a **full forward port** of loader modules (networking, rendering, registries) from 1.21.1 → 1.21.5 under NeoForge **21.5.x** / Fabric API **0.128.x**. CCI + iChunUtil pins are ready (table above). Until that port lands, ship CCI editions only on **1.21.1**.

### Minecraft 1.20.1

Needs a **full backport** (data components / networking / registries differ). Fabric + Forge CCI exist; **NeoForge does not**. Ship Fabric (+ optional Forge) when ported; document NeoForge as unavailable.

## Gradle tasks

```bash
./gradlew buildAll          # 1.21.1: fabric, fabric-cci, neoforge, neoforge-cci
./gradlew buildNeoForge26   # 26.2 WIP — not part of release until parity
```

## Shipped jar naming

Pattern: `azscompanions-<loader>[-cci]-<mod_version>+<minecraft_version>.jar`

Example (1.0.3 / 1.21.1):

- `azscompanions-neoforge-1.0.3+1.21.1.jar`
- `azscompanions-neoforge-cci-1.0.3+1.21.1.jar`
- `azscompanions-fabric-1.0.3+1.21.1.jar`
- `azscompanions-fabric-cci-1.0.3+1.21.1.jar`

Release tagging follows history: single product tag `v1.0.3` with all successful MC/loader jars attached (not per-MC tags unless a line ships on a different cadence later).
