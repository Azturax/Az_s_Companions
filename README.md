# Kon Companions

Multi-loader companion mod for Minecraft **1.21.1** (NeoForge + Fabric).

| Loader | Module | Jar |
|--------|--------|-----|
| **NeoForge** | `:neoforge` | `koncompanions-neoforge` |
| **Fabric** | `:fabric` | `koncompanions-fabric` |

Characters are explicitly **adult**, **wholesome**, and **non-sexual**.

## Requirements

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.x **or** Fabric Loader ≥0.16 + Fabric API

## Build

```bash
./gradlew :neoforge:build
./gradlew :fabric:build
```

## Quick start

1. Find a **Companion Charm** in desert pyramid chests (loot-only; one per player)
2. Right-click the charm to recruit / summon / store your companion
3. First summon uses **your username + your skin** (not Kon special)
4. Companion **follows** by day; at night sleeps in the **nearest bed** (any vanilla bed or Kon Bed)
5. **Shift + right-click** to Customize (NeoForge). Fabric: Shift+right-click opens inventory
6. Charm appear → `<Name> Hello!` · charm store → `<Name> Bye!`

## Customize

NeoForge creator: name, gender (Female/Male), skin, size & proportions. Size **0.5–3.0** (default **0.7**). Male hides bust morph.

### Kon special name

Rename the companion to **Kon** (case-insensitive) to load the Kon skin and receive a **Kon Bed** once. Other names keep player/custom skins; sleep still works on regular beds.

## License

MIT (see `LICENSE`).
