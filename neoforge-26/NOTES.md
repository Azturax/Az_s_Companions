# NeoForge 26.2 notes

Standalone jar for **Minecraft 26.2** / NeoForge **26.2.0.59** (Java 25, unobfuscated).

**CCI:** none for this Minecraft version — ship standalone only.

## Honest omissions

| Feature | Status |
|---------|--------|
| Product feature set (AI, inventory, perks, dimension follow, gifts, loot, logout park, deposit, craft/containers, render layers, Kon bed/ears, HUDs, etc.) | **Shipped** |
| CCI / iChunUtil edition | **Unavailable** on Modrinth for 26.2 |
| JourneyMap soft-dep plugin | **No** compatible MC 26 JourneyMap API jar |
| Deposit chest world outlines | Selection works; legacy level-stage outline draw API unavailable |
| Bed-home clear on block break | No `BlockEvent.BreakEvent` on pinned NeoForge 26.2 API |
| GameTests | Harness present; `@GameTestHolder` not re-registered against 26.x gametest API |
