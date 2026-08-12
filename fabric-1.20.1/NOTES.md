# Az's Companions — Fabric 1.20.1

NeoForge has **no** 1.20.1 line. Fabric Loader + Fabric API `0.92.11+1.20.1` only.

## Build

- `:fabric-1.20.1:remapJar` → `build/libs/azscompanions-fabric-1.0.4+1.20.1.jar`
- `:fabric-cci-1.20.1:remapJar` → CCI edition (CCI 1.13.0 `7tk12xkN` + iChunUtil 1.0.3 `JjEWQx5u`)
- Java 17

## Honest omissions

- JourneyMap soft-dep plugin (JM API requires JVM 21+)
- Wolf body armor (`AnimalArmorItem` / `EquipmentSlot.BODY`) — post-1.20.1
- `Attributes.SCALE` sync — scale via entity data only
- Damage recent-action uses `ALLOW_DAMAGE` (not `AFTER_DAMAGE`)
- Nametag `EntityAttachment` → uses BB-height offset

Networking / NBT / potion / recipe backports keep gameplay parity with 1.21.1 Fabric.
