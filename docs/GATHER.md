# Gather & deposit

NeoForge **1.21.1** material gather goals with optional chest multi-select. Fabric has **deposit selection** (menu + `/deposit`); full `collect_material` gather remains NeoForge-first.

## End-to-end (NeoForge)

1. **Select deposit chests** (optional but recommended)
   - Menu: companion → **Commands** → **Deposit chests…**  
     or `/deposit` / `/az deposit`
   - Walk freely; **right-click** chests/barrels/etc. to toggle (multi-select). Selected blocks outline while mode is on.
   - **Esc**, **Deposit done**, or `/deposit done` exits mode (highlights hide; selection stays).
   - `/deposit clear` or **Clear deposit chests** wipes the set.

2. **Start gather**
   - Menu: **Gather…** → item id + count + Chests/Look  
     or `/az gather <item> <count> [chest|look|nearest]`
   - Companion mines/collects, auto-swaps tools, holds/places torches in the dark, deposits into **nearest of your selected chests** (else nearest allowed container).

3. **Status / cancel**
   - Menu: **Gather status** / **Cancel gather**  
     or `/az gather status` / `/az gather cancel`

## Commands menu scrollbar

Commands list is scrollable; the **scrollbar thumb is click-draggable** (also mouse wheel). Same drag behavior on persona onboarding and customize right-panel scrollbars.

## Tools & crafting

If a block needs a tool the companion lacks, they **ask in chat** and try to **craft** a stone tool from the dynamic recipe catalog (refreshed on server start with the item gather catalog). Put ingredients in their inventory or hand them a tool.

## Slash aliases

| Command | Effect |
|---------|--------|
| `/deposit` | Start selection mode |
| `/deposit done` | Exit mode, keep chests |
| `/deposit clear` | Clear selection |
| `/az gather <item> <count> [mode]` | Assign gather |
| `/az gather status` / `cancel` | Status / cancel |
| `/ask` · `/az ask` | Server text AI only (no AI Mode) |
