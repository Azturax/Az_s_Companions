# Activity / context skins

Player-form companions can wear separate outfits for **Sleeping**, **Bathing**, and **Adventuring**. These are configured in Companion Customization → **Activity** (top tab).

## Player form only

Context skins apply **only when form is Player**. Mob forms keep their normal model/textures; Activity settings still save but do not swap skins until you switch back to Player.

## Priority

When rendering in player form:

1. Active context custom skin (if set for the current activity)
2. Else the companion’s normal custom skin (`SkinPath` / Name-tab Mojang skin)
3. Else base/default (Kon / owner skin)

Custom always wins over base. While a context URL/local skin is still downloading, the custom skin is shown (not the default).

## When each context applies

| Context | In-game trigger |
|---------|-----------------|
| Sleeping | Companion is sleeping in a bed |
| Bathing | Companion is in water (and not sleeping) |
| Adventuring | Owner is exploring / moving (same idle tracker as follow) and not sleeping/bathing |

Sleeping overrides bathing; bathing overrides adventuring.

## How to set

1. Hold charm + Shift + right-click the companion → **Customize**
2. Top-right: open **Activity**
3. For Sleeping / Bathing / Adventuring:
   - **Local:** PNG under `config/azscompanions/skins/` — enter `name.png` or `local:name.png`
   - **URL:** `https://…` (stored as `url:https://…`)
4. **Apply** (or **Clear**), then **Done**

## Multiplayer

Paths are stored on the companion entity (NBT + synced data). Each client loads local files from its own game directory; URL skins download per client.
