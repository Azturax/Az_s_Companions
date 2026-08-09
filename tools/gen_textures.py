from PIL import Image
import os
import math

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
COMMON = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "koncompanions", "textures")
NEO = os.path.join(ROOT, "neoforge", "src", "main", "resources", "assets", "koncompanions", "textures")

SKIN = (245, 214, 198, 255)
SKIN_SH = (225, 185, 168, 255)
SKIN_DK = (205, 160, 145, 255)
HAIR = (60, 52, 58, 255)
HAIR_HL = (95, 82, 90, 255)
HAIR_DK = (35, 30, 34, 255)
WHITE = (248, 250, 252, 255)
WHITE_SH = (220, 228, 236, 255)
CYAN = (110, 205, 220, 255)
CYAN_DK = (70, 160, 180, 255)
PINK = (240, 150, 180, 255)
PINK_DK = (210, 110, 145, 255)
EYE = (70, 170, 190, 255)
LIP = (230, 140, 155, 255)
TRANS = (0, 0, 0, 0)


def shade(c, f):
    return (
        max(0, min(255, int(c[0] * f))),
        max(0, min(255, int(c[1] * f))),
        max(0, min(255, int(c[2] * f))),
        c[3] if len(c) > 3 else 255,
    )


def fill_rect(img, x, y, w, h, color):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if 0 <= xx < img.width and 0 <= yy < img.height:
                img.putpixel((xx, yy), color)


def fill_rect_vshade(img, x, y, w, h, top, bot):
    for yy in range(y, y + h):
        t = (yy - y) / max(1, h - 1)
        c = tuple(int(top[i] * (1 - t) + bot[i] * t) for i in range(4))
        for xx in range(x, x + w):
            if 0 <= xx < img.width and 0 <= yy < img.height:
                img.putpixel((xx, yy), c)


def make_kon_skin():
    img = Image.new("RGBA", (64, 64), TRANS)
    fill_rect(img, 8, 0, 8, 8, HAIR)
    fill_rect(img, 16, 0, 8, 8, HAIR_DK)
    fill_rect_vshade(img, 8, 8, 8, 8, SKIN, SKIN_SH)
    fill_rect(img, 8, 8, 8, 2, HAIR)
    img.putpixel((9, 9), HAIR_HL)
    img.putpixel((10, 9), HAIR)
    img.putpixel((13, 9), HAIR)
    img.putpixel((14, 9), HAIR_HL)
    img.putpixel((10, 11), (255, 255, 255, 255))
    img.putpixel((11, 11), EYE)
    img.putpixel((13, 11), (255, 255, 255, 255))
    img.putpixel((14, 11), EYE)
    img.putpixel((11, 12), CYAN_DK)
    img.putpixel((14, 12), CYAN_DK)
    img.putpixel((10, 13), PINK)
    img.putpixel((15, 13), PINK)
    img.putpixel((12, 14), LIP)
    img.putpixel((13, 14), LIP)
    fill_rect_vshade(img, 0, 8, 8, 8, SKIN_SH, SKIN_DK)
    fill_rect(img, 0, 8, 8, 2, HAIR)
    fill_rect_vshade(img, 16, 8, 8, 8, SKIN_SH, SKIN_DK)
    fill_rect(img, 16, 8, 8, 2, HAIR)
    fill_rect_vshade(img, 24, 8, 8, 8, SKIN_SH, SKIN_DK)
    fill_rect(img, 24, 8, 8, 3, HAIR)
    fill_rect(img, 32, 0, 8, 8, HAIR_HL)
    fill_rect(img, 40, 0, 8, 8, HAIR)
    fill_rect_vshade(img, 32, 8, 8, 8, HAIR, HAIR_DK)
    fill_rect_vshade(img, 40, 8, 8, 8, HAIR_HL, HAIR)
    fill_rect_vshade(img, 48, 8, 8, 8, HAIR, HAIR_DK)
    fill_rect_vshade(img, 56, 8, 8, 8, HAIR_DK, HAIR)

    fill_rect_vshade(img, 20, 20, 8, 12, WHITE, WHITE_SH)
    fill_rect(img, 20, 20, 8, 2, CYAN)
    fill_rect(img, 20, 26, 8, 2, PINK)
    fill_rect(img, 20, 28, 8, 1, PINK_DK)
    for y in range(22, 26):
        img.putpixel((20, y), WHITE_SH)
        img.putpixel((27, y), WHITE_SH)
    fill_rect_vshade(img, 32, 20, 8, 12, WHITE_SH, shade(WHITE_SH, 0.9))
    fill_rect(img, 32, 26, 8, 2, PINK_DK)
    fill_rect(img, 16, 20, 4, 12, WHITE_SH)
    fill_rect(img, 28, 20, 4, 12, WHITE_SH)
    fill_rect(img, 16, 26, 4, 2, PINK)
    fill_rect(img, 28, 26, 4, 2, PINK)
    fill_rect(img, 20, 16, 8, 4, WHITE)
    fill_rect(img, 28, 16, 8, 4, WHITE_SH)

    fill_rect_vshade(img, 44, 20, 4, 12, WHITE, WHITE_SH)
    fill_rect(img, 44, 20, 4, 2, CYAN)
    fill_rect(img, 44, 30, 4, 2, SKIN)
    fill_rect(img, 40, 20, 4, 12, WHITE_SH)
    fill_rect(img, 48, 20, 4, 12, WHITE_SH)
    fill_rect(img, 52, 20, 4, 12, shade(WHITE_SH, 0.92))
    fill_rect(img, 44, 16, 4, 4, WHITE)
    fill_rect(img, 48, 16, 4, 4, WHITE_SH)

    fill_rect_vshade(img, 36, 52, 4, 12, WHITE, WHITE_SH)
    fill_rect(img, 36, 52, 4, 2, CYAN)
    fill_rect(img, 36, 62, 4, 2, SKIN)
    fill_rect(img, 32, 52, 4, 12, WHITE_SH)
    fill_rect(img, 40, 52, 4, 12, WHITE_SH)
    fill_rect(img, 44, 52, 4, 12, shade(WHITE_SH, 0.92))
    fill_rect(img, 36, 48, 4, 4, WHITE)
    fill_rect(img, 40, 48, 4, 4, WHITE_SH)

    fill_rect_vshade(img, 4, 20, 4, 12, WHITE, WHITE_SH)
    fill_rect(img, 4, 30, 4, 2, PINK)
    fill_rect(img, 0, 20, 4, 12, WHITE_SH)
    fill_rect(img, 8, 20, 4, 12, WHITE_SH)
    fill_rect(img, 12, 20, 4, 12, shade(WHITE_SH, 0.9))
    fill_rect(img, 4, 16, 4, 4, WHITE)
    fill_rect(img, 8, 16, 4, 4, WHITE_SH)

    fill_rect_vshade(img, 20, 52, 4, 12, WHITE, WHITE_SH)
    fill_rect(img, 20, 62, 4, 2, PINK)
    fill_rect(img, 16, 52, 4, 12, WHITE_SH)
    fill_rect(img, 24, 52, 4, 12, WHITE_SH)
    fill_rect(img, 28, 52, 4, 12, shade(WHITE_SH, 0.9))
    fill_rect(img, 20, 48, 4, 4, WHITE)
    fill_rect(img, 24, 48, 4, 4, WHITE_SH)
    return img


def make_item_icon(kind):
    img = Image.new("RGBA", (16, 16), TRANS)
    if kind == "charm":
        gold = (232, 190, 70, 255)
        gold_dk = (180, 130, 40, 255)
        gold_hl = (255, 230, 140, 255)
        fill_rect(img, 4, 3, 8, 8, gold)
        fill_rect(img, 5, 4, 6, 6, gold_hl)
        fill_rect(img, 6, 5, 4, 4, CYAN)
        fill_rect(img, 7, 6, 2, 2, CYAN_DK)
        for x in range(4, 12):
            img.putpixel((x, 3), gold_hl)
            img.putpixel((x, 10), gold_dk)
        fill_rect(img, 3, 11, 3, 3, PINK)
        fill_rect(img, 10, 11, 3, 3, PINK)
        fill_rect(img, 6, 11, 4, 2, PINK_DK)
        img.putpixel((7, 2), gold)
        img.putpixel((8, 1), gold_hl)
        img.putpixel((8, 2), gold)
    else:
        paper = (240, 236, 220, 255)
        ink = (70, 90, 110, 255)
        fill_rect_vshade(img, 3, 2, 10, 12, paper, shade(paper, 0.92))
        fill_rect(img, 5, 4, 6, 1, ink)
        fill_rect(img, 5, 6, 5, 1, ink)
        fill_rect(img, 5, 8, 6, 1, ink)
        fill_rect(img, 9, 10, 3, 3, PINK)
        img.putpixel((10, 11), PINK_DK)
    return img


def make_clothing(base, accent):
    img = Image.new("RGBA", (16, 16), TRANS)
    fill_rect_vshade(img, 3, 2, 10, 12, base, shade(base, 0.85))
    fill_rect(img, 4, 3, 8, 2, accent)
    fill_rect(img, 5, 8, 6, 1, shade(accent, 0.9))
    for y in range(5, 13):
        img.putpixel((3, y), shade(base, 0.75))
        img.putpixel((12, y), shade(base, 0.9))
    return img


def make_block_side(wood, trim):
    img = Image.new("RGBA", (16, 16), wood)
    for y in range(16):
        for x in range(16):
            f = 0.9 + 0.1 * math.sin(x * 0.8) * math.cos(y * 0.5)
            img.putpixel((x, y), shade(wood, f))
    for i in range(16):
        img.putpixel((i, 0), shade(trim, 1.05))
        img.putpixel((i, 15), shade(trim, 0.75))
        img.putpixel((0, i), shade(trim, 0.95))
        img.putpixel((15, i), shade(trim, 0.8))
    return img


def make_wardrobe():
    side = make_block_side((120, 85, 55, 255), (180, 160, 130, 255))
    for y in range(2, 14):
        for x in range(2, 7):
            side.putpixel((x, y), shade((200, 185, 160, 255), 0.95 + 0.05 * ((x + y) % 3 == 0)))
        for x in range(9, 14):
            side.putpixel((x, y), shade((195, 180, 155, 255), 0.93 + 0.05 * ((x + y) % 3 == 0)))
    side.putpixel((6, 8), CYAN)
    side.putpixel((9, 8), PINK)
    return side


def make_wardrobe_top():
    img = Image.new("RGBA", (16, 16), (130, 95, 60, 255))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade((150, 110, 70, 255), 0.88 + 0.12 * ((x + y) % 4) / 3))
    return img


def make_sewing_table():
    img = make_block_side((110, 90, 70, 255), (160, 150, 140, 255))
    fill_rect(img, 3, 3, 10, 7, (230, 225, 215, 255))
    fill_rect(img, 4, 4, 8, 1, PINK)
    fill_rect(img, 5, 6, 6, 1, CYAN_DK)
    return img


def make_sewing_top():
    img = Image.new("RGBA", (16, 16), (235, 230, 220, 255))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), shade((235, 230, 220, 255), 0.92 + 0.08 * ((x * 3 + y) % 5) / 4))
    fill_rect(img, 2, 2, 12, 2, PINK)
    fill_rect(img, 6, 6, 4, 4, CYAN)
    return img


def make_outfit_layer():
    img = Image.new("RGBA", (64, 64), TRANS)
    fill_rect(img, 20, 20, 8, 12, (255, 255, 255, 160))
    fill_rect(img, 20, 26, 8, 2, (255, 180, 200, 180))
    fill_rect(img, 44, 20, 4, 12, (255, 255, 255, 140))
    fill_rect(img, 36, 52, 4, 12, (255, 255, 255, 140))
    fill_rect(img, 4, 20, 4, 12, (255, 255, 255, 120))
    fill_rect(img, 20, 52, 4, 12, (255, 255, 255, 120))
    return img


def main():
    skin = make_kon_skin()
    for base in (COMMON, NEO):
        p = os.path.join(base, "entity", "companion")
        os.makedirs(p, exist_ok=True)
        skin.save(os.path.join(p, "kon.png"))
        make_outfit_layer().save(os.path.join(p, "kon_outfit.png"))

    items = {
        "companion_charm": make_item_icon("charm"),
        "companion_contract": make_item_icon("contract"),
        "shrine_maiden_top": make_clothing(WHITE, CYAN),
        "pink_sash": make_clothing(PINK, PINK_DK),
        "white_stockings": make_clothing(WHITE, WHITE_SH),
        "red_pompom_shoes": make_clothing((220, 80, 90, 255), PINK),
        "travel_cloak": make_clothing(CYAN_DK, CYAN),
        "work_apron": make_clothing((210, 200, 180, 255), (160, 140, 110, 255)),
        "adventurer_scarf": make_clothing(PINK, CYAN),
        "soft_gloves": make_clothing(WHITE_SH, SKIN),
        "fox_ear_ribbon": make_clothing(PINK, HAIR),
        "rain_cape": make_clothing((100, 140, 180, 255), CYAN),
    }
    item_dir = os.path.join(COMMON, "item")
    os.makedirs(item_dir, exist_ok=True)
    for name, im in items.items():
        im.save(os.path.join(item_dir, name + ".png"))
    neo_item = os.path.join(NEO, "item")
    os.makedirs(neo_item, exist_ok=True)
    items["companion_charm"].save(os.path.join(neo_item, "companion_charm.png"))
    items["companion_contract"].save(os.path.join(neo_item, "companion_contract.png"))

    block_dir = os.path.join(COMMON, "block")
    os.makedirs(block_dir, exist_ok=True)
    make_wardrobe().save(os.path.join(block_dir, "wardrobe.png"))
    make_wardrobe_top().save(os.path.join(block_dir, "wardrobe_top.png"))
    make_sewing_table().save(os.path.join(block_dir, "sewing_table.png"))
    make_sewing_top().save(os.path.join(block_dir, "sewing_table_top.png"))

    old = os.path.join(item_dir, "companion_selector.png")
    if os.path.exists(old):
        os.remove(old)
    old_neo = os.path.join(neo_item, "companion_selector.png")
    if os.path.exists(old_neo):
        os.remove(old_neo)
    print("OK textures")


if __name__ == "__main__":
    main()
