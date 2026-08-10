"""Generate azscompanions companion_inventory.png aligned to menu slot coords."""
from PIL import Image

W, H = 194, 220
TEX = 256
img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
px = img.load()

C6 = (198, 198, 198, 255)
DARK = (55, 55, 55, 255)
LIGHT = (255, 255, 255, 255)
MID = (139, 139, 139, 255)


def fill_rect(x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < TEX and 0 <= y < TEX:
                px[x, y] = color


def draw_panel(x0, y0, x1, y1):
    fill_rect(x0, y0, x1, y1, C6)
    for x in range(x0, x1):
        px[x, y1 - 1] = DARK
        if y1 - 2 >= y0:
            px[x, y1 - 2] = MID
    for y in range(y0, y1):
        px[x1 - 1, y] = DARK
        if x1 - 2 >= x0:
            px[x1 - 2, y] = MID
    for x in range(x0, x1 - 1):
        px[x, y0] = LIGHT
    for y in range(y0, y1 - 1):
        px[x0, y] = LIGHT
    fill_rect(x0 + 2, y0 + 2, x1 - 2, y0 + 3, MID)
    fill_rect(x0 + 2, y0 + 2, x0 + 3, y1 - 2, MID)
    fill_rect(x0 + 3, y0 + 3, x1 - 3, y1 - 3, C6)


def draw_slot(sx, sy):
    # Vanilla-like 18x18 slot well
    fill_rect(sx, sy, sx + 18, sy + 18, DARK)
    fill_rect(sx + 1, sy + 1, sx + 17, sy + 17, MID)
    for x in range(sx, sx + 18):
        px[x, sy] = DARK
        px[x, sy + 17] = LIGHT
    for y in range(sy, sy + 18):
        px[sx, sy + y - sy] = DARK
        px[sx + 17, sy + y - sy] = LIGHT
    px[sx + 17, sy] = MID
    px[sx, sy + 17] = MID
    px[sx + 17, sy + 17] = LIGHT
    fill_rect(sx + 1, sy + 1, sx + 17, sy + 17, (8, 8, 8, 255))


COMP_BOTTOM = 112
draw_panel(0, 0, W, COMP_BOTTOM)

ARMOR_X, STORAGE_X, STORAGE_Y = 8, 26, 18
HOTBAR_Y = 76

for i in range(5):
    draw_slot(ARMOR_X - 1, STORAGE_Y - 1 + i * 18)

for row in range(3):
    for col in range(9):
        draw_slot(STORAGE_X - 1 + col * 18, STORAGE_Y - 1 + row * 18)

for col in range(9):
    draw_slot(STORAGE_X - 1 + col * 18, HOTBAR_Y - 1)

PLAYER_INV_Y = COMP_BOTTOM + 12 + 11  # 135
player_x = STORAGE_X - 8  # 18
player_y = PLAYER_INV_Y - 12  # 123
player_w, player_h = 176, 96
draw_panel(player_x, player_y, player_x + player_w, player_y + player_h)

for row in range(3):
    for col in range(9):
        draw_slot(STORAGE_X - 1 + col * 18, PLAYER_INV_Y - 1 + row * 18)
for col in range(9):
    draw_slot(STORAGE_X - 1 + col * 18, PLAYER_INV_Y + 58 - 1)

# Transparent gap + side voids
for y in range(COMP_BOTTOM, player_y):
    for x in range(W):
        px[x, y] = (0, 0, 0, 0)
for y in range(player_y + player_h, H):
    for x in range(W):
        px[x, y] = (0, 0, 0, 0)
for y in range(player_y, player_y + player_h):
    for x in range(0, player_x):
        px[x, y] = (0, 0, 0, 0)
    for x in range(player_x + player_w, W):
        px[x, y] = (0, 0, 0, 0)

out = r"common\src\main\resources\assets\azscompanions\textures\gui\companion_inventory.png"
import os

os.makedirs(os.path.dirname(out), exist_ok=True)
img.save(out, "PNG")
print("wrote", out, img.size, "PLAYER_INV_Y", PLAYER_INV_Y)
