# Multi-version targets

Az's Companions product line is developed on **Minecraft 1.21.1** (NeoForge + Fabric). Additional Minecraft / loader lines ship when feature-parity is honest.

**Feature rule:** every shipped jar must carry the **current product feature set** from main (AI, inventory, Wiggly/UUID perks, dimension follow, gifts, loot config, logout park, no Jindujun/whistle, etc.). Omit a feature only when the MC/loader API makes it impossible — document omissions in release notes. Do **not** ship feature-stripped stubs as production jars.

**CCI soft-compat:** On lines where CCI exists (1.21.1 / 1.21.5 / 1.20.1), the **main** NeoForge/Fabric/Forge jar includes optional Content Creator Integration support. Works with or without CCI installed — **no separate `*-cci` jars**. You still need **separate loader jars** (NeoForge vs Fabric; Forge on 1.20.1). NeoForge 26.x has no CCI.

## Loader ↔ Minecraft mapping (research Aug 2026)

| User / target request | Minecraft | NeoForge / Forge | Fabric | CCI soft-compat | Status |
|-----------------------|-----------|------------------|--------|-----------------|--------|
| Production LTS | **1.21.1** | NeoForge **21.1.248** | Fabric API `0.116.15+1.21.1` | **Yes** (1.13.0 optional) | **Shipped** |
| Latest CCI MC | **1.21.5** | NeoForge **21.5.98** | Fabric API `0.128.2+1.21.5` | **Yes** (1.13.0 optional) | **Shipped** |
| `1.20.1` | **1.20.1** | **Forge 47.4.22** (no NeoForge 20.1) | Fabric API `0.92.11+1.20.1` | Fabric + Forge **Yes** | **Shipped** |
| NeoForge `26.1.2` | **26.1.2** | **26.1.2.95** | Fabric API exists | **None** | **Shipped** |
| NeoForge `26.2` | **26.2** | **26.2.0.59** | Fabric API `0.156.0+26.2` | **None** | **Shipped** |

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
| `:common` / `:neoforge` / `:fabric` (CCI soft-compat in-jar) | **Yes** |
| Unit tests (`:common:test`) | **Pass** |
| Feature parity with product 1.0.4+ | **Yes** |

### Minecraft 1.21.5 (shipped)

| Gate | Status |
|------|--------|
| `:neoforge-21.5` / `:fabric-21.5` (CCI soft-compat in-jar) | **Yes** |
| Pins | NeoForge **21.5.98**, Fabric API **0.128.2+1.21.5**, CCI/iChunUtil table above |
| Feature parity | **Yes** (EntityRenderState / NBT Optional / Equippable ports) |
| GameTests | Disabled on this line (annotation package churn) |

### Minecraft 1.20.1 (shipped — Forge, not NeoForge)

| Gate | Status |
|------|--------|
| `:fabric-1.20.1` / `:forge-1.20.1` (CCI soft-compat in-jar) | **Yes** |
| NeoForge | **Unavailable** on Maven for 20.1 |

**Honest omissions (1.20.1 Fabric + Forge):** JourneyMap soft-dep (API jar needs JVM 21+); wolf body armor (`AnimalArmorItem` / `BODY`); `Attributes.SCALE` (scale via entity data); nametag attachment → BB-height offset. See `fabric-1.20.1/NOTES.md` and `forge-1.20.1/NOTES.md`.

### NeoForge 26.2 (`:neoforge-26`) / 26.1.2 (`:neoforge-26-1`) — shipped

| Gate | Status |
|------|--------|
| `:neoforge-26` / `:neoforge-26-1` | **Yes** |
| Pins | NeoForge **26.2.0.59** / **26.1.2.95**; Java **25**; unobfuscated |
| CCI | **None** for 26.1.2 / 26.2 |
| Feature parity | **Yes** (AvatarRenderState / SubmitNodeCollector / GuiGraphicsExtractor ports) |

**Honest omissions (26.x):** no CCI; no JourneyMap API jar; deposit chest world outlines (selection still works); bed-home clear on block break (no `BlockEvent.BreakEvent`); GameTests unregistered. See `neoforge-26/NOTES.md` and `neoforge-26-1/NOTES.md`.

## Gradle tasks

```bash
./gradlew buildAll          # 1.21.1: fabric + neoforge (CCI soft-compat included)
./gradlew buildAll215       # 1.21.5: fabric + neoforge
./gradlew buildAll1201      # 1.20.1: fabric + forge
./gradlew buildNeoForge26   # 26.2
./gradlew buildNeoForge261  # 26.1.2
```

## Shipped jar naming

Pattern: `azscompanions-<loader>-<mod_version>+<minecraft_version>.jar`

One jar per (Minecraft version × loader). NeoForge and Fabric remain **separate** jars — there is no universal cross-loader jar.

Release tagging (from **v1.0.8**): **8 jars** total — 1.21.1×2, 1.21.5×2, 1.20.1×2, 26.1.2×1, 26.2×1. No `*-cci*` artifacts.
