#!/usr/bin/env python3
"""
Procedurally generates every texture used by The Backrooms mod.
No external/copyrighted assets are used - everything here is original,
generated from deterministic noise.

Run:  python3 tools/gen_textures.py
Outputs into src/main/resources/assets/backrooms/
"""
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "backrooms")
BLOCK_DIR = os.path.join(ROOT, "textures", "block")
ENTITY_DIR = os.path.join(ROOT, "textures", "entity")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ENTITY_DIR, exist_ok=True)

RNG = random.Random(0xBAC0A001)


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))


def noise_fill(img, base, jitter=12, seed=0):
    r = random.Random(seed)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            n = r.randint(-jitter, jitter)
            px[x, y] = (
                clamp(base[0] + n + r.randint(-3, 3)),
                clamp(base[1] + n + r.randint(-3, 3)),
                clamp(base[2] + n + r.randint(-3, 3)),
                255,
            )


def blotches(img, colors, count, radius=(1, 4), seed=0, alpha=70):
    r = random.Random(seed)
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    for _ in range(count):
        x = r.randint(0, img.width - 1)
        y = r.randint(0, img.height - 1)
        rad = r.randint(*radius)
        col = r.choice(colors)
        d.ellipse([x - rad, y - rad, x + rad, y + rad], fill=(*col, alpha))
    img.alpha_composite(overlay)


def save(img, name):
    path = os.path.join(BLOCK_DIR, name)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# ---------------------------------------------------------------- wallpaper
def wallpaper(name, base, seed, damp=False):
    img = Image.new("RGBA", (16, 16), base + (255,))
    noise_fill(img, base, jitter=8, seed=seed)
    px = img.load()
    # subtle vertical pinstripes (the classic Level-0 wallpaper look)
    for x in range(16):
        if x % 4 == 2:
            for y in range(16):
                c = px[x, y]
                px[x, y] = (clamp(c[0] - 12), clamp(c[1] - 12), clamp(c[2] - 8), 255)
    blotches(img, [(90, 80, 28), (120, 100, 36)], count=10 if damp else 3,
             radius=(2, 5), seed=seed + 11, alpha=55 if damp else 28)
    if damp:
        blotches(img, [(40, 44, 22), (52, 50, 20)], count=8, radius=(2, 6),
                 seed=seed + 77, alpha=80)
    save(img, name)


wallpaper("wallpaper.png", (184, 162, 56), seed=1)
wallpaper("wallpaper_damp.png", (120, 112, 44), seed=2, damp=True)

# ---------------------------------------------------------------- carpet
def carpet(name):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    r = random.Random(33)
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = r.randint(-16, 16)
            fiber = 10 if r.random() < 0.18 else 0  # occasional lighter fibre
            px[x, y] = (clamp(126 + v + fiber), clamp(106 + v * 0.8 + fiber),
                        clamp(48 + v * 0.5), 255)
    blotches(img, [(70, 58, 26), (60, 50, 22)], count=6, radius=(2, 5),
             seed=44, alpha=60)  # old damp stains
    save(img, name)


carpet("carpet.png")

# ---------------------------------------------------------------- ceiling
def ceiling(name):
    img = Image.new("RGBA", (16, 16), (216, 212, 196, 255))
    noise_fill(img, (216, 212, 196), jitter=6, seed=55)
    px = img.load()
    # acoustic tile pinholes
    r = random.Random(56)
    for _ in range(60):
        x, y = r.randint(1, 14), r.randint(1, 14)
        c = px[x, y]
        px[x, y] = (clamp(c[0] - 22), clamp(c[1] - 22), clamp(c[2] - 20), 255)
    # metal grid frame (tile border)
    frame = (128, 126, 116)
    for x in range(16):
        px[x, 0] = frame + (255,)
        px[x, 15] = frame + (255,)
    for y in range(16):
        px[0, y] = frame + (255,)
        px[15, y] = frame + (255,)
    save(img, name)


ceiling("ceiling_tile.png")

# ---------------------------------------------------------------- fluorescent
def fluorescent(name, lit):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    frame = (96, 102, 100)
    d.rectangle([0, 0, 15, 15], fill=frame)
    if lit:
        # glowing diffuser panel with a soft center
        d.rectangle([2, 2, 13, 13], fill=(236, 246, 236, 255))
        r = random.Random(77)
        px = img.load()
        for y in range(2, 14):
            for x in range(2, 14):
                v = r.randint(-8, 8)
                px[x, y] = (clamp(238 + v), clamp(248 + v), clamp(236 + v), 255)
        # two tube highlights
        d.rectangle([3, 4, 12, 5], fill=(255, 255, 252, 255))
        d.rectangle([3, 10, 12, 11], fill=(255, 255, 252, 255))
    else:
        d.rectangle([2, 2, 13, 13], fill=(112, 116, 110, 255))
        r = random.Random(78)
        px = img.load()
        for y in range(2, 14):
            for x in range(2, 14):
                v = r.randint(-12, 8)
                px[x, y] = (clamp(112 + v), clamp(116 + v), clamp(110 + v), 255)
        # dark dead tubes
        d.rectangle([3, 4, 12, 5], fill=(74, 78, 74, 255))
        d.rectangle([3, 10, 12, 11], fill=(70, 74, 70, 255))
        d.line([4, 3, 11, 12], fill=(60, 64, 60, 255))  # crack
    save(img, name)


fluorescent("fluorescent.png", True)
fluorescent("fluorescent_off.png", False)

# ---------------------------------------------------------------- bacteria
def bacteria_texture():
    S = 64
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    noise_fill(img, (74, 60, 48), jitter=14, seed=909)
    # murky darker blotches
    blotches(img, [(44, 34, 28), (58, 44, 34)], count=70, radius=(2, 7),
             seed=910, alpha=90)
    # pale stretched-skin patches
    blotches(img, [(180, 168, 146), (150, 138, 120)], count=26, radius=(2, 6),
             seed=911, alpha=75)
    # reddish sores
    blotches(img, [(122, 49, 40), (90, 36, 32)], count=22, radius=(1, 3),
             seed=912, alpha=110)
    img.save(os.path.join(ENTITY_DIR, "bacteria.png"))
    print("wrote textures/entity/bacteria.png")


bacteria_texture()

# ---------------------------------------------------------------- mod icon
def icon():
    S = 256
    img = Image.new("RGBA", (S, S), (8, 8, 8, 255))
    d = ImageDraw.Draw(img)
    vp = (128, 132)  # vanishing point
    # back wall
    bw = 70
    d.rectangle([vp[0] - bw // 2, vp[1] - bw // 2, vp[0] + bw // 2,
                 vp[1] + bw // 2], fill=(150, 130, 40))
    # floor / ceiling / walls as trapezoids
    d.polygon([(0, S), (0, 150), (vp[0] - bw // 2, vp[1] + bw // 2),
               (vp[0] + bw // 2, vp[1] + bw // 2), (S, 150), (S, S)],
              fill=(96, 80, 36))
    d.polygon([(0, 0), (0, 150), (vp[0] - bw // 2, vp[1] - bw // 2),
               (vp[0] + bw // 2, vp[1] - bw // 2), (S, 150), (S, 0)],
              fill=(176, 156, 52))
    d.polygon([(0, 0), (0, S), (vp[0] - bw // 2, vp[1] + bw // 2),
               (vp[0] - bw // 2, vp[1] - bw // 2)], fill=(166, 146, 46))
    d.polygon([(S, 0), (S, S), (vp[0] + bw // 2, vp[1] + bw // 2),
               (vp[0] + bw // 2, vp[1] - bw // 2)], fill=(158, 138, 42))

    def interp(a, b, t):
        return a + (b - a) * t

    # fluorescent strips marching down the ceiling
    for i, t in enumerate((0.25, 0.5, 0.72, 0.9)):
        y = interp(150, vp[1] - 16, t)
        w = interp(180, 40, t)
        h = interp(12, 4, t)
        glow = 220 - i * 12
        col = (glow, glow + 12, glow - 4)
        d.rectangle([128 - w / 2, y - h / 2, 128 + w / 2, y + h / 2], fill=col)

    # the Bacteria silhouette, tall and lanky, far down the hall
    cx = vp[0]
    feet = vp[1] + 30
    head_top = feet - 64
    d.ellipse([cx - 7, head_top, cx + 7, head_top + 16], fill=(12, 10, 8))
    d.polygon([(cx - 7, head_top + 12), (cx + 7, head_top + 12),
               (cx + 9, feet - 18), (cx - 9, feet - 18)], fill=(12, 10, 8))
    # long arms
    d.line([cx - 6, head_top + 16, cx - 14, feet - 6], fill=(12, 10, 8), width=5)
    d.line([cx + 6, head_top + 16, cx + 14, feet - 6], fill=(12, 10, 8), width=5)
    # legs
    d.line([cx - 4, feet - 20, cx - 8, feet], fill=(12, 10, 8), width=5)
    d.line([cx + 4, feet - 20, cx + 8, feet], fill=(12, 10, 8), width=5)

    # vignette
    vig = Image.new("L", (S, S), 0)
    vd = ImageDraw.Draw(vig)
    vd.ellipse([-40, -40, S + 40, S + 40], fill=255)
    import PIL.ImageFilter as F
    vig = vig.filter(F.GaussianBlur(60))
    black = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    img = Image.composite(img, black, vig)
    img.save(os.path.join(ROOT, "icon.png"))
    print("wrote icon.png")


icon()
print("done")
