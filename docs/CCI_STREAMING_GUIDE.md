# CCI Streaming Guide — Az's Companions (CCI edition)

Practical setup for streamers using **iChun Content Creator Integration (CCI)** with the Az's Companions **CCI edition** jar.

Official CCI docs: [content-creator-integration.readthedocs.io](https://content-creator-integration.readthedocs.io/)  
Issues / docs repo: [iChun/ContentCreatorIntegration-IssuesAndDocumentation](https://github.com/iChun/ContentCreatorIntegration-IssuesAndDocumentation)

---

## What you can do **today**

These six IMC subjects are implemented in `neoforge-cci` and `fabric-cci` and work when the companion is summoned and within ~96 blocks of you:

| IMC `subject` | Aliases | What happens | Uses `message`? |
|---------------|---------|--------------|-----------------|
| `companion_say` | `say` | Companion chat-lines the exact text | Yes (default `"Hello!"`) |
| `companion_greet` | `greet` | Companion says thanks + name | Yes — usually `$username` |
| `companion_wave` | `wave` | Companion says hello + optional name | Yes — optional name |
| `companion_follow` | `follow` | Sets companion mode to **FOLLOW** (clears task queue) | Ignored |
| `companion_sit` | `sit` | Sets companion mode to **SIT** | Ignored |
| `companion_stay` | `stay` | Sets companion mode to **STAY** | Ignored |

**Not implemented** by Az's Companions: spawn mobs, channel-point inventory gifts, skin changes, summon/despawn companion, dance/emotes beyond the lines above, or any CCI socket beyond listening for `IMCOutcome`.

**Mob spawning** is a **CCI-native** feature (typically `CommandOutcome` + `/summon`), not part of our bridge. You can run a summon outcome *alongside* a companion say/greet in the same Config Event.

---

## Which jars to install

Install **one** Az's Companions jar per loader — never standalone + CCI together:

| Jar | Role |
|-----|------|
| `azscompanions-neoforge-cci-0.1.0.jar` | NeoForge **CCI edition** |
| `azscompanions-neoforge-0.1.0.jar` | NeoForge standalone — **no** bridge |
| `azscompanions-fabric-cci-0.1.0.jar` | Fabric **CCI edition** |
| `azscompanions-fabric-0.1.0.jar` | Fabric standalone — **no** bridge |

Plus for Minecraft **1.21.1** (matching loader):

| Mod | NeoForge | Fabric |
|-----|----------|--------|
| CCI **1.13.0** | [AySbAgcO](https://modrinth.com/mod/content-creator-integration/version/AySbAgcO) | [PERd6IT9](https://modrinth.com/mod/content-creator-integration/version/PERd6IT9) |
| iChunUtil **1.0.3** | [OvIyyNh4](https://modrinth.com/mod/ichunutil/version/OvIyyNh4) | [gfAOoiwe](https://modrinth.com/mod/ichunutil/version/gfAOoiwe) |

Fabric note: iChunUtil’s Fabric `sendIMCMessage` is a no-op; the Fabric CCI jar bridges the same IMCOutcome subjects via a client mixin, and also registers `/azscci <subject> [message]` for CommandOutcome.

Do **not** use CCI jars built for 1.21.3+ with this project.

---

## How the companion hooks into CCI

1. In CCI you attach an **IMCOutcome** (`type: "imc"`) to a stream Config Event.
2. CCI sends a **client-side** InterModComms runtime message to `modId = azscompanions`.
3. Our bridge polls IMC, maps `subject` → action, and forwards a packet to the server.
4. The server finds your nearest owned/trusted companion and applies the action.
5. Feedback: companion chat line and/or action-bar toast (CCI toast API when available).

```
Stream event (bits / sub / tip / channel points / …)
    → CCI Config Event (conditions + outcomes)
        → IMCOutcome { modId, subject, message }
            → Az's Companions IMC bridge
                → companion react on server
```

IMCOutcome fields ([CCI docs](https://content-creator-integration.readthedocs.io/en/latest/components/config/outcome/IMCOutcome/)):

| Field | Required | Value for us |
|-------|----------|--------------|
| `type` | yes | `"imc"` |
| `modId` | yes | `"azscompanions"` |
| `subject` | yes | one of the subjects in the table above |
| `message` | yes | text or CCI variables (`$username`, etc.) — may be `""` for mode actions |

---

## Setup in the CCI Editor (short path)

1. Launch with CCI + iChunUtil + **CCI edition** jar.
2. Open the **CCI Editor** → connect a socket (Twitch Chat / EventSub, Streamlabs, StreamElements, YouTube Chat, etc. — see [Sockets](https://content-creator-integration.readthedocs.io/en/latest/gettingstarted/socketdifferences/)).
3. Summon your companion (Companion Charm) so actions have a target.
4. Create or edit a **Config Event** for the stream event type you care about (e.g. cheer, subscription, tip, or `message` with a channel-point condition).
5. Add an outcome of type **IMC** / `imc`.
6. Set `modId` = `azscompanions`, pick a `subject`, set `message`.
7. Save. Trigger a test event (or use CCI’s debug tools) and confirm the companion reacts.

Variable names differ slightly by socket. Use the **Event Viewer** in the CCI Editor to copy real names (often `$username` / `$display-name`). Channel points: match `custom-reward-id` — see [Capturing Twitch Channel Point Rewards](https://content-creator-integration.readthedocs.io/en/latest/howto/twitchchannelpointreward/).

Example outcome JSON also ships inside the jar under `cci-examples/` (not auto-loaded).

---

## Example setups

### 1) New follower / sub → greet

**Idea:** Viewer name is thanked by the companion.

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_greet",
  "message": "$username"
}
```

Result line: `Thanks for the support, <name>!`

Attach this outcome to your socket’s follow / subscription Config Event (exact event layer name depends on the socket — check Event Viewer).

---

### 2) Bits / tip → custom say

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_say",
  "message": "Thanks for the bits, $username!"
}
```

Or for tips/donations, same subject with a tip-oriented line, e.g. `"Thanks for the tip, $username!"`.

---

### 3) Channel points → wave

Create a channel-point reward, capture its `custom-reward-id` via Event Viewer, put a `VariableCondition` on a `message` Config Event, then:

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_wave",
  "message": "$username"
}
```

Result: `Hello, <name>!` (or `Hello there!` if message empty).

---

### 4) Channel points → follow / sit / stay

Mode actions ignore `message`:

**Follow me**

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_follow",
  "message": ""
}
```

**Sit**

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_sit",
  "message": ""
}
```

**Stay**

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_stay",
  "message": ""
}
```

Typical streamer pattern: three channel-point rewards (“Companion Follow / Sit / Stay”), each Config Event matching its `custom-reward-id`.

---

### 5) Sub + companion reaction + spawn a mob (CCI native)

Az's Companions does **not** spawn mobs. Use CCI’s **CommandOutcome** in the **same** Config Event as the IMC greet:

**Outcome A — companion greets**

```json
{
  "type": "imc",
  "modId": "azscompanions",
  "subject": "companion_greet",
  "message": "$username"
}
```

**Outcome B — CCI summons a friendly mob** (you must be allowed to run commands / be op’d as CCI expects for server commands):

```json
{
  "type": "command",
  "command": "summon minecraft:wolf ~ ~ ~ {Owner:$mc_uuid}",
  "executeAsSelf": true
}
```

Notes:

- Exact `CommandOutcome` fields and whether you use `executeAsSelf` are defined by [CCI CommandOutcome](https://content-creator-integration.readthedocs.io/en/latest/components/config/outcome/CommandOutcome/). Adjust the summon NBT to what your version accepts.
- For safer spawn positions, CCI recommends `TwoHighSpaceCondition` before summoning ([FAQ](https://content-creator-integration.readthedocs.io/en/latest/howto/faq/)).
- For gift bombs / many spawns, use CCI queues / `RepeatOutcome` so you do not flood the world.

---

### 6) Tip / bits threshold → say + stay

Use CCI amount conditions (`SpecificAmountCondition` / `RangedAmountCondition`) on the cheer/tip event, then stack outcomes:

1. `companion_say` — `"Big tip from $username — holding position!"`
2. `companion_stay`

---

## Limitations (accurate)

| Topic | Reality today |
|-------|----------------|
| Companion must be out | Actions no-op with a toast if no owned/trusted companion within ~96 blocks |
| Dedicated servers | IMCOutcome is **client-side** in CCI; we forward to the server when the streamer client receives it. The companion still needs to exist on that world |
| Spawn / items / effects | Use CCI outcomes (`command`, `inventory`, `health`, …) — not our IMC subjects |
| Skin / customize | Still Az's Companions only (Mojang username skins). CCI has no character API |
| Auto-import configs | We ship example snippets only; build Config Events in the CCI Editor |
| Fabric / standalone NeoForge jar | No IMC bridge — need the **CCI edition** jar |
| Aliases | Short subjects (`say`, `greet`, …) work, but prefer `companion_*` in shared configs |

---

## Quick reference — IMC keys

```
modId:   azscompanions
subjects:
  companion_say | companion_greet | companion_wave
  companion_follow | companion_sit | companion_stay
message: free text / $variables  (required field; use "" for modes)
```

Jar examples: `cci-examples/imc-companion-*.json` inside `azscompanions-neoforge-cci-*.jar`.
