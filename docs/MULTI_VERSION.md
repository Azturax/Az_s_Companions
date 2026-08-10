# Multi-version targets

Az's Companions **0.3.9** ships production jars for **Minecraft 1.21.1** only (standalone + CCI for both loaders). Additional Minecraft lines are tracked here for follow-up ports.

## Loader â†” Minecraft mapping

| User request | Minecraft | NeoForge | Fabric | Notes |
|--------------|-----------|----------|--------|-------|
| `1.20.1` | 1.20.1 | **None** in NeoForged releases Maven (Forge `47.x` recommended) | Fabric API `0.92.x+1.20.1` | Port deferred |
| `1.21.11` â†’ **1.21.1** | **1.21.1** | **21.1.248** | Fabric API `0.116.15+1.21.1` | **Shipped in 0.3.9** |
| NeoForge `26.1.2` | **26.1.2** | **26.1.2.94** (check Maven) | Fabric API `0.155.2+26.1.2`, Java 25, unobfuscated | Port deferred |
| NeoForge `26.2` | **26.2** | **26.2.0.59** | Fabric API `0.156.0+26.2`, Java 25, unobfuscated | Port deferred â€” see below |

### NeoForge 26.2 â†” Minecraft (confirmed)

Per [NeoForged versioning](https://docs.neoforged.net/docs/gettingstarted/versioning/):

- Minecraft **26.2** is the second 2026 calver game drop (`year.release`).
- NeoForge **26.2.0.x** targets that Minecraft line (major/minor = MC year/release; next digit is MC patch `0`; last digit is NeoForge build).
- Latest checked on Maven (`maven.neoforged.net`, 2026-08-10): **26.2.0.59** (builds `26.2.0.57+` are non-`-beta`; earlier 26.2 builds were `*-beta`).

This is **not** Minecraft 1.21.x. Do not confuse with NeoForge **21.1.x** (MC **1.21.1**) or **21.11.x** (MC **1.21.11**).

### Port readiness (26.2)

| Gate | Status |
|------|--------|
| Mainline 1.21.1 compiles (`:common` / `:neoforge` / `:fabric`) | **Yes** (verified) |
| Unit tests (`:common:test`) | **Pass** |
| Separate `:neoforge-26` (or branch) module | **Not started** â€” prefer this so 1.21.1 jars keep shipping |
| Java 25 toolchain on build machine | Available |
| CCI / iChunUtil for MC 26.2 on Modrinth | **None** (0 versions) |
| Feature parity with 0.3.9 (AI, Behavior, CCI) | Blocked until API port exists |

**Decision:** do **not** rush a half-broken bump of the 1.21.1 module. When the port starts, add a dedicated NeoForge 26.2 module/branch, pin `neo_version=26.2.0.59` (or newer), ship standalone first, and document AI/CCI gaps honestly until parity.

## CCI + iChunUtil pins (Modrinth, Aug 2026)

| Minecraft | Loader | CCI version | CCI Modrinth id | iChunUtil | iChunUtil Modrinth id |
|-----------|--------|-------------|-----------------|-----------|------------------------|
| **1.21.1** | NeoForge | 1.13.0 | `AySbAgcO` | 1.0.3 | `OvIyyNh4` |
| **1.21.1** | Fabric | 1.13.0 | `PERd6IT9` | 1.0.3 | `gfAOoiwe` |
| **1.20.1** | Fabric | 1.13.0 | `7tk12xkN` | 1.0.3 | `JjEWQx5u` |
| **1.20.1** | Forge | 1.13.0 | `nNaAlKHI` | 1.0.3 | `W6d0pCyu` |
| **1.20.1** | NeoForge | **none** | â€” | **none** | â€” |
| **26.1.2** | Fabric / NeoForge / Forge | **none** | â€” | **none** | â€” |
| **26.2** | Fabric / NeoForge / Forge | **none** | â€” | **none** | â€” |

Do **not** reuse the 1.21.1 Modrinth version ids on other Minecraft versions â€” each MC/loader has its own file even when the CCI version number is also `1.13.0`.

When a MC line is ported later:
- If CCI exists for that loader â†’ ship `*-cci-0.x.y+<mc>.jar` pinned to the table above (re-check Modrinth at build time).
- If CCI is missing â†’ ship standalone only and document the gap (current situation for 26.x and 1.20.1 NeoForge).

## Why other lines are deferred

- **26.1.2 / 26.2:** Unobfuscated Minecraft, Java 25, large API migration (entities, networking, rendering â€” including 26.1â†’26.2 Blaze3d/Vulkan primer changes) across ~160+ Java sources. No CCI builds on Modrinth yet. Prefer a **separate module** so 1.21.1 production jars stay untouched.
- **1.20.1:** No NeoForge `20.1.x` releases; Fabric/Forge backports still need 1.20.1 registry/networking changes. CCI exists for Fabric + Forge only.

## Shipped 0.3.9 jars (1.21.1 only)

- `azscompanions-neoforge-0.3.9+1.21.1.jar`
- `azscompanions-neoforge-cci-0.3.9+1.21.1.jar` (CCI 1.13.0 `AySbAgcO` + iChunUtil 1.0.3 `OvIyyNh4`)
- `azscompanions-fabric-0.3.9+1.21.1.jar`
- `azscompanions-fabric-cci-0.3.9+1.21.1.jar` (CCI 1.13.0 `PERd6IT9` + iChunUtil 1.0.3 `gfAOoiwe`)

**NeoForge 26.2 jar:** *none yet* (no release tag / artifact). Label when ready: e.g. `0.3.9+neoforge-26.2` / `azscompanions-neoforge-0.3.9+26.2.jar`.
