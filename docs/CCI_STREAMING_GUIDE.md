# CCI Streaming Guide — Az's Companions

Stream setup for **iChun Content Creator Integration (CCI)** with the Az's Companions **CCI edition** jars.

- Mod repo: [Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- Release: [v0.3.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.3.0)
- Official CCI docs: [content-creator-integration.readthedocs.io](https://content-creator-integration.readthedocs.io/)

---

## What you can do today

Implemented in **both** `neoforge-cci` and `fabric-cci`. Unless noted, the companion must be summoned and within ~96 blocks.

### Chat / modes

| IMC `subject` | Aliases | What happens | Uses `message`? |
|---------------|---------|--------------|-----------------|
| `companion_say` | `say` | Companion chat-lines the exact text | Yes (default `"Hello!"`) |
| `companion_greet` | `greet` | Thanks for the support, `<name>`! | Yes — usually `$username` |
| `companion_wave` | `wave` | Hello, `<name>`! | Yes — optional name |
| `companion_follow` | `follow` | Mode **FOLLOW** | Ignored |
| `companion_sit` | `sit` | Mode **SIT** | Ignored |
| `companion_stay` | `stay` | Mode **STAY** (no teleport) | Ignored |

### Attitude, teams, summon, equipment

| IMC `subject` | Message examples | Effect |
|---------------|------------------|--------|
| `companion_set_attitude` | `hostile` / `passive` / `attitude=hostile` | Persist `PASSIVE` or `HOSTILE` |
| `companion_set_team` | `red` / `$username` / `team=blue` | Persist `teamId` (nametag tint); different teams fight |
| `companion_summon` | `form=zombie;attitude=hostile;team=red` | Recruit owned companion with form/attitude/team/skin |
| `companion_summon_passive` | `form=chicken;team=blue` | Summon with **PASSIVE** attitude |
| `companion_summon_hostile` | `form=skeleton;skin=Notch;team=red` | Summon with **HOSTILE** attitude |
| `companion_set_mainhand` | `minecraft:diamond_sword` or `mainhand=…` | Set main hand (or `clear`) |
| `companion_set_offhand` | `minecraft:shield` / `clear` | Set off hand |
| `companion_set_armor` | `helmet=minecraft:iron_helmet;boots=…` | Set armor slots |
| `companion_set_hand` / `companion_set_equipment` | `mainhand=…;offhand=…;helmet=…` | Any equipment keys |
| `companion_modify` | `form=wolf;skin=Notch;name=Fluffy;attitude=passive` | Edit the **already called/summoned** companion (no new recruit) |
| `companion_turn_evil` | `seconds=10` (optional, 5–15) | Playful temporary HOSTILE (~10s default), then restore prior attitude |

**Modify (called / summoned companion)**

- Requires an owned companion within ~96 blocks (charm-called or CCI-summoned).
- Same keys as summon: `form`, `skin`/`player`, `name`, `attitude`, `team`, plus equipment slots.
- Does **not** create a new companion — only updates the nearest owned one.
- Fabric fallback: `/azscci companion_modify form=cat;name=Mochi`

**Playful “turn evil”**

- Temporary `HOSTILE` toward nearby players/mobs **except** the owner/trusted (same as normal hostile attitude).
- Duration 5–15 seconds (default 10), then attitude restores automatically. Ownership/charm binding unchanged.
- CCI: `companion_turn_evil` / aliases `turn_evil`, `go_evil`, `berserk`
- Hidden in-game: right-click your companion with a **fermented spider eye** (consumes 1; ~10s).
- Dialogue + smoke/angry particles on activate; purr + hearts when calming down.

**Attitude**

- **PASSIVE** — defend-owner only (default friendly companion).
- **HOSTILE** — aggressive toward nearby players/mobs except owner/trusted. Still owned by the streamer.

**Teams**

- Companions with different non-empty `teamId` values fight each other.
- Same team = allied. Never attack the assigned owner.
- Useful for channel-point “join red/blue” redemptions (`message` = `$username` or redemption input).

**Summon forms**

- Mob forms: `form=chicken|wolf|cat|cow|pig|sheep|fox|rabbit|bee|zombie|skeleton|spider|enderman|husk|stray`
- Player skin: `form=player;skin=Notch` (Mojang username → `player:<uuid>` skin)
- Optional: `name=…`, `team=…`, `attitude=…`, plus equipment keys in the same message

**Equipment**

- Item ids validated server-side (`minecraft:diamond_sword`, bare `diamond_sword`, or `clear`/`none`/`air`).
- Slots: `mainhand`, `offhand`, `helmet`/`head`, `chestplate`/`chest`, `leggings`/`legs`, `boots`/`feet`.

**Fabric note:** same subjects via IMCOutcome mixin + `/azscci <subject> [message]` CommandOutcome fallback.

---

## Twitch channel points / bits

1. CCI Editor → connect Twitch (or Streamlabs / SE).
2. Config Event conditioned on **channel points** (`custom-reward-id`) or **bits/cheers**.
3. Add **IMC** outcome:
   - `modId` = `azscompanions`
   - `subject` = e.g. `companion_set_team` or `companion_summon_hostile`
   - `message` = `$username`, redemption user input, or structured `form=zombie;team=red`
4. Use Event Viewer for exact variable names.

Examples ship in the jar under `cci-examples/`.

---

## Which jars

Install **one** Az's Companions jar — never standalone + CCI together:

| Jar | Role |
|-----|------|
| `azscompanions-neoforge-cci-0.3.0+1.21.1.jar` | NeoForge CCI |
| `azscompanions-fabric-cci-0.3.0+1.21.1.jar` | Fabric CCI |
| `azscompanions-neoforge-0.3.0+1.21.1.jar` | NeoForge standalone |
| `azscompanions-fabric-0.3.0+1.21.1.jar` | Fabric standalone |

Plus CCI **1.13.0** + iChunUtil **1.0.3** for Minecraft **1.21.1**.

---

## Flow

```
Stream event → CCI IMCOutcome { modId, subject, message }
  → Az's Companions bridge (client) → packet → server apply
```

Summoned event companions remain **owned** by the streamer (charm-compatible). Hostile/team fights are intentional stream tools, not free-for-all grief.
