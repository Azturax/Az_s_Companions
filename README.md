# Kon Companions

Multi-loader companion mod for Minecraft **1.21.1** (NeoForge + Fabric).

| Loader | Module | Jar |
|--------|--------|-----|
| **NeoForge** | `:neoforge` | `koncompanions-neoforge` |
| **Fabric** | `:fabric` | `koncompanions-fabric` |

Shared assets/data live in `:common`. Characters are explicitly **adult**, **wholesome**, and **non-sexual**.

## Requirements

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.x **or** Fabric Loader ≥0.16 + Fabric API

## Build

```bash
./gradlew :neoforge:build
./gradlew :fabric:build
```

Jars land in `neoforge/build/libs/` and `fabric/build/libs/`.

## Quick start

1. Find a **Companion Charm** in desert pyramid chests (loot-only; one charm per player)
2. Right-click the charm to recruit / summon / store **Kon**
3. First summon also grants a **Kon Bed** (once)
4. Kon **follows** you; at night she sleeps in a nearby Kon Bed (placing one sets home)
5. **Shift + right-click** Kon to open Customize (NeoForge). Fabric: Shift+right-click opens inventory
6. Chat near your owned Kon to talk — see [Chat with Kon](#chat-with-kon)

Commands: `/koncompanions rename <name>`, `/koncompanions customize`, `/koncompanions home`

## Chat with Kon

Stand within ~16 blocks of your owned Kon and type in chat:

- Address her: `Kon hello` / `Kon, how are you?` (or her custom name)
- Or ask clear companion questions nearby: `follow me`, `where's home`, `what do you like`

Example questions: hello, how are you, what's your name, follow me, go to bed, where's home, I love you, are you hungry, what do you like, thank you, goodbye.

Replies are canned wholesome lines (editable in `assets/koncompanions/lang/en_us.json`). Owner-only, ~1.5s cooldown.

## Customize

NeoForge creator: name, skin (classic 64×64 / local PNG), body scale & proportions. Size **0.5–3.0** (default **0.7**).

There is **no** contract item, sewing table, or wardrobe in this build.

## Adding companions (datapack)

Add `data/<namespace>/companions/<id>.json`. Skins: classic Minecraft player PNGs (64×64). Local imports: `config/koncompanions/skins/` with path `local:filename.png`.

## Kon

Adult fox-girl / shrine-maiden companion (reference art in `docs/reference/`).

## License

MIT (see `LICENSE`).
