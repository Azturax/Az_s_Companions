# Multi-version targets

Az's Companions product line is developed on **Minecraft 1.21.1** (NeoForge + Fabric, standalone + CCI). Additional Minecraft / loader lines ship when feature-parity is honest.

**Feature rule:** every shipped jar must carry the **current product feature set** from main (AI, inventory, Wiggly/UUID perks, dimension follow, gifts, loot config, logout park, no Jindujun/whistle, etc.). Omit a feature only when the MC/loader API makes it impossible — document omissions in release notes. Do **not** ship feature-stripped stubs as production jars.

## Loader ↔ Minecraft mapping (research Aug 2026)

| User / target request | Minecraft | NeoForge / Forge | Fabric | CCI | Status |
|-----------------------|-----------|------------------|--------|-----|--------|
| Production LTS | **1.21.1** | NeoForge **21.1.248** | Fabric API `0.116.15+1.21.1` | **Yes** (1.13.0) | **Shipped** |
| Latest CCI MC | **1.21.5** | NeoForge **21.5.98** | Fabric API `0.128.2+1.21.5` | **Yes** (1.13.0) | **Shipped** |
| `1.20.1` | **1.20.1** | **Forge 47.4.22** (no NeoForge 20.1) | Fabric API `0.92.11+1.20.1` | Fabric + Forge **Yes** | **Shipped** |
| NeoForge `26.1.2` | **26.1.2** | **26.1.2.95** | Fabric API exists | **None** | **WIP** — not shipped |
| NeoForge `26.2` | **26.2** | **26.2.0.59** | Fabric API `0.156.0+26.2` | **None** | **WIP** — not shipped |

### NeoForge calver notes

| NeoForge line | Minecraft | Notes |
|---------------|-----------|-------|
| `21.1.x` | 1.21.1 | LTS production |
| `21.5.x` | 1.21.5 | Latest CCI-capable NeoForge line |
| `26.1.2.x` | **26.1.2** | Stable calver drop (Java 25, unobfuscated) |
| `26.2.0.x` | **26.2** | Second 2026 drop (Java 25, unobfuscated) |

There is **no** NeoForge `20.1.x` release. For MC **1.20.1**, Forge `47.4.x` is the supported Forge-family loader.

## CCI + iChunUtil pins (Modrinth, verified Aug 2026)

| Minecraft | Loader | CCI | CCI Modrinth id | iChunUtil | iChunUtil Modrinth id |
|-----------|--------|-----|-----------------|-----------|------------------------|
| **1.21.1** | NeoForge | 1.13.0 | `AySbAgcO` | 1.0.3 | `OvIyyNh4` |
| **1.21.1** | Fabric | 1.13.0 | `PERd6IT9` | 1.0.3 | `gfAOoiwe` |
| **1.21.5** | NeoForge | 1.13.0 | `WRDFe2RG` | 1.0.7 | `Hrl6YCrv` |
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
| Feature parity with product 1.0.4 | **Yes** |

### Minecraft 1.21.5 (shipped)

| Gate | Status |
|------|--------|
| `:neoforge-21.5` / `:fabric-21.5` / CCI editions | **Yes** |
| Pins | NeoForge **21.5.98**, Fabric API **0.128.2+1.21.5**, CCI/iChunUtil table above |
| Feature parity | **Yes** (EntityRenderState / NBT Optional / Equippable ports) |
| GameTests | Disabled on this line (annotation package churn) |

### Minecraft 1.20.1 (shipped — Forge, not NeoForge)

| Gate | Status |
|------|--------|
| `:fabric-1.20.1` / `:fabric-cci-1.20.1` | **Yes** |
| `:forge-1.20.1` / `:forge-cci-1.20.1` | **Yes** |
| NeoForge | **Unavailable** on Maven for 20.1 |

**Honest omissions (1.20.1 Fabric + Forge):** JourneyMap soft-dep (API jar needs JVM 21+); wolf body armor (`AnimalArmorItem` / `BODY`); `Attributes.SCALE` (scale via entity data); nametag attachment → BB-height offset. See `fabric-1.20.1/NOTES.md` and `forge-1.20.1/NOTES.md`.

### NeoForge 26.2 (`:neoforge-26`) / 26.1.2 (`:neoforge-26-1`)

Compiles for local/dev use. Residual gaps (armor/elytra/cape SubmitNodeCollector, mob forms, Kon ears/bed, container caps, HUD hooks, etc.) — **do not ship**. Details earlier in this file / prior release notes.

## Gradle tasks

```bash
./gradlew buildAll          # 1.21.1: fabric, fabric-cci, neoforge, neoforge-cci
./gradlew buildAll215       # 1.21.5: fabric, fabric-cci, neoforge, neoforge-cci
./gradlew buildAll1201      # 1.20.1: fabric, fabric-cci, forge, forge-cci
./gradlew buildNeoForge26   # 26.2 WIP — not part of release
./gradlew buildNeoForge261  # 26.1.2 WIP — not part of release
```

## Shipped jar naming

Pattern: `azscompanions-<loader>[-cci]-<mod_version>+<minecraft_version>.jar`

Release tagging: single product tag `v1.0.4` with all successful MC/loader jars attached.
