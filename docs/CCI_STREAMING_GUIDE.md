# CCI Streaming Guide — Az's Companions

Stream setup for **iChun Content Creator Integration (CCI)** with the Az's Companions **CCI edition** jars.

- Mod repo: [Azturax/Az_s_Companions](https://github.com/Azturax/Az_s_Companions)
- Release: [v0.1.0](https://github.com/Azturax/Az_s_Companions/releases/tag/v0.1.0)
- Official CCI docs: [content-creator-integration.readthedocs.io](https://content-creator-integration.readthedocs.io/)
- CCI issues / docs: [iChun/ContentCreatorIntegration-IssuesAndDocumentation](https://github.com/iChun/ContentCreatorIntegration-IssuesAndDocumentation)

---

## What you can do today

Implemented in **both** `neoforge-cci` and `fabric-cci`. Companion must be summoned and within ~96 blocks:

| IMC `subject` | Aliases | What happens | Uses `message`? |
|---------------|---------|--------------|-----------------|
| `companion_say` | `say` | Companion chat-lines the exact text | Yes (default `"Hello!"`) |
| `companion_greet` | `greet` | Thanks for the support, `<name>`! | Yes — usually `$username` |
| `companion_wave` | `wave` | Hello, `<name>`! (or Hello there!) | Yes — optional name |
| `companion_follow` | `follow` | Mode **FOLLOW** (clears task queue) | Ignored |
| `companion_sit` | `sit` | Mode **SIT** | Ignored |
| `companion_stay` | `stay` | Mode **STAY** | Ignored |

**Not** our bridge: spawn mobs, inventory gifts, skin changes, summon/despawn companion, dance/emotes beyond the lines above.

**Mob spawning** is CCI-native (`CommandOutcome` + `/summon`). Stack it with greet/say in the same Config Event if you want.

---

## Which jars to install

Install **one** Az's Companions jar per loader — never standalone + CCI together:

| Jar | Role |
|-----|------|
| `azscompanions-neoforge-cci-0.1.0.jar` | NeoForge CCI edition |
| `azscompanions-fabric-cci-0.1.0.jar` | Fabric CCI edition |
| `azscompanions-neoforge-0.1.0.jar` | NeoForge standalone — **no** bridge |
| `azscompanions-fabric-0.1.0.jar` | Fabric standalone — **no** bridge |

Plus for Minecraft **1.21.1** (matching loader):

| Mod | NeoForge | Fabric |
|-----|----------|--------|
| CCI **1.13.0** | [AySbAgcO](https://modrinth.com/mod/content-creator-integration/version/AySbAgcO) | [PERd6IT9](https://modrinth.com/mod/content-creator-integration/version/PERd6IT9) |
| iChunUtil **1.0.3** | [OvIyyNh4](https://modrinth.com/mod/ichunutil/version/OvIyyNh4) | [gfAOoiwe](https://modrinth.com/mod/ichunutil/version/gfAOoiwe) |

**Fabric note:** iChunUtil Fabric `sendIMCMessage` is a no-op. The Fabric CCI jar bridges the same IMCOutcome subjects via a client mixin, and also registers `/azscci <subject> [message]` for CommandOutcome.

Do **not** use CCI jars built for 1.21.3+ with this project.

---

## How the companion hooks into CCI

1. Attach an **IMCOutcome** (`type: "imc"`) to a stream Config Event.
2. CCI sends a **client-side** InterModComms runtime message to `modId = azscompanions`.
3. Our bridge maps `subject` → action and forwards a packet to the server.
4. The server finds your nearest owned/trusted companion and applies the action.
5. Feedback: companion chat line and/or action-bar toast.

```
Stream event (bits / sub / tip / channel points / …)
    → CCI Config Event (conditions + outcomes)
        → IMCOutcome { modId, subject, message }
            → Az's Companions IMC bridge
                → companion react on server
```

| Field | Required | Value for us |
|-------|----------|--------------|
| `type` | yes | `"imc"` |
| `modId` | yes | `"azscompanions"` |
| `subject` | yes | one of the subjects above |
| `message` | yes | text or CCI variables — may be `""` for mode actions |

---

## Setup in the CCI Editor

1. Launch with CCI + iChunUtil + **CCI edition** jar.
2. Open the **CCI Editor** → connect a socket (Twitch, Streamlabs, StreamElements, YouTube Chat, etc.).
3. Summon your companion (Companion Charm).
4. Create/edit a **Config Event** for the stream event you care about.
5. Add outcome type **IMC** / `imc`.
6. Set `modId` = `azscompanions`, pick a `subject`, set `message`.
7. Save and test.

Use the **Event Viewer** for real variable names (often `$username` / `$display-name`). Channel points: match `custom-reward-id`.

Example JSON also ships in the jar under `cci-examples/` (not auto-loaded).

---

## Example setups

### 1) Sub / follower → greet

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_greet",
  "message": "$username"
}
```

Result: `Thanks for the support, <name>!`

### 2) Bits / tip → say

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_say",
  "message": "Thanks for the bits, $username!"
}
```

### 3) Channel points → wave

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_wave",
  "message": "$username"
}
```

### 4) Channel points → follow / sit / stay

```json
{ "type": "imc", "modId": "azscompanions", "subject": "companion_follow", "message": "" }
```

```json
{ "type": "imc", "modId": "azscompanions", "subject": "companion_sit", "message": "" }
```

```json
{ "type": "imc", "modId": "azscompanions", "subject": "companion_stay", "message": "" }
```

### 5) Sub → greet + CCI `/summon` (native)

**Outcome A**

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_greet",
  "message": "$username"
}
```

**Outcome B** (adjust fields per [CommandOutcome](https://content-creator-integration.readthedocs.io/en/latest/components/config/outcome/CommandOutcome/))

```json
{
  "type": "command",
  "command": "summon minecraft:wolf ~ ~ ~ {Owner:$mc_uuid}",
  "executeAsSelf": true
}
```

Prefer `TwoHighSpaceCondition` before summoning; use queues / `RepeatOutcome` for gift bombs.

### 6) Tip threshold → say + stay

Stack on amount conditions:

1. `companion_say` — `"Big tip from $username — holding position!"`
2. `companion_stay`

### Fabric CommandOutcome fallback

```json
{
  "type": "command",
  "command": "azscci companion_greet $username",
  "executeAsSelf": true
}
```

---

## Limitations

| Topic | Reality |
|-------|---------|
| Companion must be out | No-op + toast if none within ~96 blocks |
| Dedicated servers | IMCOutcome is client-side in CCI; we forward when the streamer client receives it |
| Spawn / items / effects | Use CCI outcomes — not our IMC subjects |
| Skin / customize | Az's Companions only (Mojang username skins) |
| Auto-import configs | Example snippets only; build events in the CCI Editor |
| Standalone jars | No bridge — need the matching CCI edition |

---

## Quick reference

```
modId:   azscompanions
subjects:
  companion_say | companion_greet | companion_wave
  companion_follow | companion_sit | companion_stay
message: free text / $variables  (use "" for modes)
deps:    CCI 1.13.0 + iChunUtil 1.0.3 (same loader)
```

Jar examples: `cci-examples/` inside `azscompanions-*-cci-*.jar`.
