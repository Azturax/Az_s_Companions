# Multi-version targets

Az's Companions **0.2.1** ships production jars for **Minecraft 1.21.1** only (standalone + CCI for both loaders). Additional Minecraft lines are tracked here for follow-up ports.

## Loader ↔ Minecraft mapping

| User request | Minecraft | NeoForge | Fabric | Notes |
|--------------|-----------|----------|--------|-------|
| `1.20.1` | 1.20.1 | **None** in NeoForged releases Maven (Forge `47.x` recommended) | Fabric API `0.92.x+1.20.1` | Port deferred |
| `1.21.11` → **1.21.1** | **1.21.1** | **21.1.248** | Fabric API `0.116.15+1.21.1` | **Shipped in 0.2.1** |
| NeoForge `26.1.2` | **26.1.2** | **26.1.2.94** | Fabric API `0.155.2+26.1.2`, Java 25, unobfuscated | Port deferred |
| NeoForge `26.2` | **26.2** | **26.2.0.58** | Fabric API `0.156.0+26.2`, Java 25, unobfuscated | Port deferred |

Note: Minecraft **1.21.11** is a real later release (NeoForge **21.11.x**) between 1.21.1 and calendar **26.1**. This project targets **1.21.1**, not 1.21.11.

## CCI + iChunUtil pins (Modrinth, Aug 2026)

| Minecraft | Loader | CCI version | CCI Modrinth id | iChunUtil | iChunUtil Modrinth id |
|-----------|--------|-------------|-----------------|-----------|------------------------|
| **1.21.1** | NeoForge | 1.13.0 | `AySbAgcO` | 1.0.3 | `OvIyyNh4` |
| **1.21.1** | Fabric | 1.13.0 | `PERd6IT9` | 1.0.3 | `gfAOoiwe` |
| **1.20.1** | Fabric | 1.13.0 | `7tk12xkN` | 1.0.3 | `JjEWQx5u` |
| **1.20.1** | Forge | 1.13.0 | `nNaAlKHI` | 1.0.3 | `W6d0pCyu` |
| **1.20.1** | NeoForge | **none** | — | **none** | — |
| **26.1.2** | Fabric / NeoForge / Forge | **none** | — | **none** | — |
| **26.2** | Fabric / NeoForge / Forge | **none** | — | **none** | — |

Do **not** reuse the 1.21.1 Modrinth version ids on other Minecraft versions — each MC/loader has its own file even when the CCI version number is also `1.13.0`.

When a MC line is ported later:
- If CCI exists for that loader → ship `*-cci-0.x.y+<mc>.jar` pinned to the table above (re-check Modrinth at build time).
- If CCI is missing → ship standalone only and document the gap (current situation for 26.x and 1.20.1 NeoForge).

## Why other lines are deferred

- **26.1.2 / 26.2:** Unobfuscated Minecraft, Java 25, new Fabric Loom (`net.fabricmc.fabric-loom`), large API migration across ~195 Java sources. No CCI builds on Modrinth yet.
- **1.20.1:** No NeoForge `20.1.x` releases; Fabric/Forge backports still need 1.20.1 registry/networking changes. CCI exists for Fabric + Forge only.

## Shipped 0.2.1 jars (1.21.1)

- `azscompanions-neoforge-0.2.1+1.21.1.jar`
- `azscompanions-neoforge-cci-0.2.1+1.21.1.jar` (CCI 1.13.0 `AySbAgcO` + iChunUtil 1.0.3 `OvIyyNh4`)
- `azscompanions-fabric-0.2.1+1.21.1.jar`
- `azscompanions-fabric-cci-0.2.1+1.21.1.jar` (CCI 1.13.0 `PERd6IT9` + iChunUtil 1.0.3 `gfAOoiwe`)
