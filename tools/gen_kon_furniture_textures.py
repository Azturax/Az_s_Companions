from PIL import Image, ImageDraw
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "common" / "src" / "main" / "resources" / "assets" / "koncompanions"
tex_block = root / "textures" / "block"
tex_item = root / "textures" / "item"
tex_block.mkdir(parents=True, exist_ok=True)
tex_item.mkdir(parents=True, exist_ok=True)

WHITE = (248, 248, 252, 255)
PINK = (255, 170, 200, 255)
BLUE = (120, 170, 230, 255)
DARK = (80, 90, 120, 255)
CREAM = (255, 240, 245, 255)

bed = Image.new("RGBA", (16, 16), WHITE)
d = ImageDraw.Draw(bed)
d.rectangle([0, 8, 15, 15], fill=BLUE)
d.rectangle([1, 9, 14, 14], fill=PINK)
d.rectangle([4, 2, 11, 7], fill=CREAM)
d.rectangle([5, 3, 10, 6], fill=PINK)
d.line([0, 7, 15, 7], fill=DARK)
bed.save(tex_block / "kon_bed.png")
bed.save(tex_item / "kon_bed.png")

bed_top = Image.new("RGBA", (16, 16), BLUE)
d = ImageDraw.Draw(bed_top)
d.rectangle([0, 0, 15, 15], fill=BLUE)
d.rectangle([1, 1, 14, 10], fill=PINK)
d.rectangle([3, 11, 12, 15], fill=CREAM)
d.rectangle([5, 12, 10, 14], fill=PINK)
bed_top.save(tex_block / "kon_bed_top.png")

pillow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(pillow)
d.rounded_rectangle([1, 3, 14, 12], radius=3, fill=CREAM, outline=PINK)
d.ellipse([5, 5, 10, 10], fill=PINK)
pillow.save(tex_block / "kon_daki_pillow.png")
pillow.save(tex_item / "kon_daki_pillow.png")

blanket = Image.new("RGBA", (16, 16), BLUE)
d = ImageDraw.Draw(blanket)
for y in range(0, 16, 2):
    d.line([0, y, 15, y], fill=PINK)
d.rectangle([0, 0, 15, 15], outline=WHITE)
blanket.save(tex_block / "kon_blanket.png")
blanket.save(tex_item / "kon_blanket.png")
print("textures ok")
