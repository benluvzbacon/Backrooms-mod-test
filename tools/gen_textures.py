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
import zlib

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "backrooms")
BLOCK_DIR = os.path.join(ROOT, "textures", "block")
ENTITY_DIR = os.path.join(ROOT, "textures", "entity")
ITEM_DIR = os.path.join(ROOT, "textures", "item")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ENTITY_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

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

# ---------------------------------------------------------------- pool tile
def pool_tile_texture(name):
    img = Image.new("RGBA", (16, 16), (226, 232, 230, 255))
    noise_fill(img, (228, 234, 232), jitter=5, seed=101)
    px = img.load()
    grout = (176, 188, 190, 255)
    # small square ceramic tiles every 4 px with grout lines
    for i in range(16):
        if i % 4 == 3:
            for j in range(16):
                px[i, j] = grout
                px[j, i] = grout
    # faint glossy highlight on each tile
    r = random.Random(102)
    for tx in range(4):
        for ty in range(4):
            hx = tx * 4 + r.randint(0, 1)
            hy = ty * 4 + r.randint(0, 1)
            c = px[hx, hy]
            px[hx, hy] = (clamp(c[0] + 18), clamp(c[1] + 18), clamp(c[2] + 16), 255)
    save(img, name)


pool_tile_texture("pool_tile.png")

# ---------------------------------------------------------------- pool portal
def pool_portal_texture(name):
    img = Image.new("RGBA", (16, 16), (20, 120, 150, 255))
    px = img.load()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            angle = math.atan2(dy, dx)
            # concentric wavy bands of teal and pale cyan
            wave = math.sin(dist * 1.7 + angle * 2.0)
            v = (wave + 1.0) * 0.5
            rr = clamp(20 + v * 70 + max(0.0, 1.0 - dist / 9.0) * 90)
            gg = clamp(120 + v * 90 + max(0.0, 1.0 - dist / 9.0) * 90)
            bb = clamp(150 + v * 80 + max(0.0, 1.0 - dist / 9.0) * 70)
            px[x, y] = (rr, gg, bb, 255)
    # a few sparkle pixels
    r = random.Random(103)
    for _ in range(14):
        x, y = r.randint(1, 14), r.randint(1, 14)
        px[x, y] = (220, 250, 255, 255)
    save(img, name)


pool_portal_texture("pool_portal.png")

# ---------------------------------------------------------------- almond water
def almond_water_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    glass = (178, 206, 214, 255)
    glass_dark = (138, 168, 178, 255)
    liquid = (240, 232, 204, 255)
    liquid_shade = (222, 212, 178, 255)
    cap = (120, 122, 130, 255)
    # bottle cap
    d.rectangle([7, 1, 8, 2], fill=cap)
    # neck (glass)
    d.rectangle([7, 3, 8, 6], fill=glass)
    # shoulders and body
    d.rectangle([6, 7, 9, 7], fill=glass)
    d.rectangle([5, 8, 10, 13], fill=glass)
    d.rectangle([5, 14, 10, 14], fill=glass_dark)
    # almond-coloured liquid inside
    d.rectangle([6, 9, 9, 13], fill=liquid)
    d.rectangle([6, 12, 9, 13], fill=liquid_shade)
    px = img.load()
    # glass highlight
    px[6, 8] = (220, 240, 246, 255)
    px[6, 10] = (220, 240, 246, 255)
    path = os.path.join(ITEM_DIR, "almond_water.png")
    img.save(path)
    print("wrote textures/item/almond_water.png")


almond_water_texture()

# --------------------------------------------------------------- still life
# The Still Life: a gaunt mannequin in a tricorn hat, black beard, yellow
# blood-streaked vest over teal sleeves, black belt with gold buckle, grey sash.
def sl_faces(u, v, w, h, d):
    """Minecraft box UV rects: name -> (x0, y0, w, h) on the texture sheet."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }


def sl_paint_rect(img, rect, base, jitter, seed, blotch=None):
    r = random.Random(seed)
    px = img.load()
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            n = r.randint(-jitter, jitter)
            px[x, y] = (clamp(base[0] + n), clamp(base[1] + n),
                        clamp(base[2] + n), 255)
    if blotch:
        colors, count, radius, alpha, bseed = blotch
        sub = Image.new("RGBA", img.size, (0, 0, 0, 0))
        sd = ImageDraw.Draw(sub)
        br = random.Random(bseed)
        for _ in range(count):
            x = br.randint(x0, x0 + w - 1)
            y = br.randint(y0, y0 + h - 1)
            rad = br.randint(*radius)
            col = br.choice(colors)
            sd.ellipse([x - rad, y - rad, x + rad, y + rad], fill=(*col, alpha))
        img.alpha_composite(sub)


def still_life_texture():
    S = 128
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    SKIN = (172, 164, 148)      # pale weathered-mannequin skin
    SOCKET = (58, 50, 42)       # sunken eye sockets
    BROW = (48, 40, 32)         # heavy carved brows
    EYE_WHITE = (232, 230, 220)
    PUPIL = (26, 22, 18)
    TEAL = (127, 182, 164)      # long sleeves
    CUFF = (96, 146, 130)
    HAND = (148, 132, 108)      # grimy gnarled hands
    GRIME = (92, 80, 60)
    VEST = (202, 184, 92)       # yellow waistcoat
    COAT = (178, 158, 74)       # darker coat-skirt fabric
    COAT_SHADE = (150, 132, 60)
    TRIM = (214, 175, 64)       # gold trim / buttons
    TRIM_SHADE = (150, 118, 40)
    BLOOD = (106, 26, 24)       # dried blood streaks
    BELT = (24, 22, 20)
    SASH = (130, 122, 142)      # grey-lavender waist sash
    SASH_SHADE = (96, 90, 108)
    SHIRT = (202, 196, 180)
    SHADOW = (140, 126, 66)     # under-beard neck shadow
    TROUSERS = (44, 47, 56)
    BOOT = (26, 26, 30)
    HAT = (30, 26, 20)
    HAT_TRIM = (52, 44, 32)
    BEARD = (22, 20, 18)
    BEARD_GREY = (86, 82, 76)

    # ---- head 8x8x8 at (0,0), weathered all over
    for name, rect in sl_faces(0, 0, 8, 8, 8).items():
        sl_paint_rect(img, rect, SKIN, 7, 1000 + zlib.crc32(name.encode()) % 900,
                      blotch=([(120, 110, 94), (102, 90, 70)], 8, (1, 2), 40, 1050))
    # The stare (FRONT rect 8..16, 8..16): dark sunken sockets, wide whites,
    # small pupils, heavy brows. The beard covers rows fy+3..fy+7.
    fx, fy = 8, 8
    d.rectangle([fx + 0, fy + 1, fx + 3, fy + 2], fill=SOCKET)
    d.rectangle([fx + 4, fy + 1, fx + 7, fy + 2], fill=SOCKET)
    d.rectangle([fx + 1, fy + 1, fx + 2, fy + 2], fill=EYE_WHITE)
    d.rectangle([fx + 5, fy + 1, fx + 6, fy + 2], fill=EYE_WHITE)
    d.point((fx + 2, fy + 2), fill=PUPIL)
    d.point((fx + 5, fy + 2), fill=PUPIL)
    d.line([fx + 0, fy + 0, fx + 3, fy + 0], fill=BROW)
    d.line([fx + 4, fy + 0, fx + 7, fy + 0], fill=BROW)
    # a thin carved mouth line (the beard hides most of it)
    d.line([fx + 2, fy + 6, fx + 5, fy + 6], fill=(96, 84, 70))

    # ---- body 6x18x4 at (0,16): yellow waistcoat
    body = sl_faces(0, 16, 6, 18, 4)
    for name, rect in body.items():
        sl_paint_rect(img, rect, VEST, 9, 1100 + zlib.crc32(name.encode()) % 900,
                      blotch=([(158, 140, 64)], 10, (1, 3), 45, 1190))
    # dried-blood streaks running down the front (and back)
    for face_name, seed_off in (("front", 0), ("back", 50)):
        x0, y0, w, h = body[face_name]
        br = random.Random(1200 + seed_off)
        for _ in range(7):
            sx = x0 + br.randint(0, w - 1)
            sy = y0 + br.randint(0, 4)
            length = br.randint(3, 9)
            for t in range(length):
                xx = sx + (1 if br.random() < 0.3 else 0)
                yy = sy + t
                if x0 <= xx < x0 + w and y0 <= yy < y0 + h:
                    img.putpixel((xx, yy), (*BLOOD, 255))

    # shirt collar + under-beard shadow at the neck (front face rows 0..2)
    fx0, fy0, fw, fh = body["front"]
    for xx in range(fx0, fx0 + fw):
        img.putpixel((xx, fy0), (*SHADOW, 255))
    for xx in range(fx0 + 1, fx0 + 5):
        img.putpixel((xx, fy0 + 1), (*SHIRT, 255))
    for xx in (fx0 + 2, fx0 + 3):
        img.putpixel((xx, fy0 + 2), (*SHIRT, 255))
    # brass buttons down the waistcoat front (bright + shaded halves)
    for by in (fy0 + 3, fy0 + 5, fy0 + 7):
        img.putpixel((fx0 + 2, by), (*TRIM, 255))
        img.putpixel((fx0 + 3, by), (*TRIM_SHADE, 255))
    # black belt around the waist (all four sides) + gold buckle on front.
    # Rows 12+ hide behind the coat skirts, so the belt sits at rows 9..11.
    for face_name in ("front", "back", "left", "right"):
        x0, y0, w, h = body[face_name]
        for yy in range(y0 + 9, y0 + 12):
            for xx in range(x0, x0 + w):
                img.putpixel((xx, yy), (*BELT, 255))
    for xx in range(fx0 + 1, fx0 + 5):
        for yy in range(fy0 + 9, fy0 + 12):
            img.putpixel((xx, yy), (*TRIM, 255))
    for xx in (fx0 + 2, fx0 + 3):
        img.putpixel((xx, fy0 + 10), (*BELT, 255))  # buckle opening

    # ---- arms 3x18x3: teal sleeves, grimy hands at the cuffs
    for au in (24, 38):
        arms = sl_faces(au, 16, 3, 18, 3)
        for name, rect in arms.items():
            sl_paint_rect(img, rect, TEAL, 8, 1300 + au + zlib.crc32(name.encode()) % 900)
        # darker cuff band and grimy hands on the bottom rows of each face
        for name in ("front", "back", "left", "right"):
            x0, y0, w, h = arms[name]
            for xx in range(x0, x0 + w):
                img.putpixel((xx, y0 + h - 5), (*CUFF, 255))
                for yy in range(y0 + h - 2, y0 + h):
                    img.putpixel((xx, yy), (*HAND, 255))
        gr = random.Random(1350 + au)
        for name in ("front", "back", "left", "right"):
            x0, y0, w, h = arms[name]
            for _ in range(6):
                img.putpixel((x0 + gr.randint(0, w - 1), y0 + h - 1 - gr.randint(0, 1)),
                             (*GRIME, 255))

    # ---- legs 3x14x3: dark trousers with black boots
    for lu in (0, 12):
        legs = sl_faces(lu, 40, 3, 14, 3)
        for name, rect in legs.items():
            sl_paint_rect(img, rect, TROUSERS, 7, 1400 + lu + zlib.crc32(name.encode()) % 900)
        for name in ("front", "back", "left", "right"):
            x0, y0, w, h = legs[name]
            for yy in range(y0 + h - 3, y0 + h):
                for xx in range(x0, x0 + w):
                    img.putpixel((xx, yy), (*BOOT, 255))

    # ---- beard 7x5x2 at (26,40): big black beard on jaw and chin
    beard = sl_faces(26, 40, 7, 5, 2)
    for name, rect in beard.items():
        sl_paint_rect(img, rect, BEARD, 6, 1500 + zlib.crc32(name.encode()) % 900)
    # long grey-streaked strands + stray hairs on the front
    bx, by, bw, bh = beard["front"]
    br = random.Random(1550)
    for _ in range(7):
        sx = bx + br.randint(0, bw - 1)
        for yy in range(by, by + bh):
            if br.random() < 0.85:
                img.putpixel((sx, yy), (*BEARD_GREY, 255))
    for _ in range(16):
        img.putpixel((bx + br.randint(0, bw - 1), by + br.randint(0, bh - 1)),
                     (70, 66, 60, 255))

    # ---- coat skirts: front 6x8x1 at (0,62), back at (16,62), sides 1x8x4 at (0,72)
    skirt_f = sl_faces(0, 62, 6, 8, 1)
    for name, rect in skirt_f.items():
        sl_paint_rect(img, rect, COAT, 7, 1900 + zlib.crc32(name.encode()) % 900)
    kx0, ky0, kw, kh = skirt_f["front"]
    # pocket flaps with gold trim under the belt line
    for px in (kx0, kx0 + kw - 2):
        for xx in range(px, px + 2):
            img.putpixel((xx, ky0), (*COAT_SHADE, 255))
            img.putpixel((xx, ky0 + 1), (*TRIM, 255))
    # fold shading at the panel edges
    for yy in range(ky0, ky0 + kh):
        img.putpixel((kx0, yy), (*COAT_SHADE, 255))
        img.putpixel((kx0 + kw - 1, yy), (*COAT_SHADE, 255))
    # sash knot + hanging ends over the coat front
    for xx in range(kx0 + 2, kx0 + 4):
        for yy in range(ky0 + 2, ky0 + 4):
            img.putpixel((xx, yy), (*SASH, 255))
    img.putpixel((kx0 + 2, ky0 + 2), (*SASH_SHADE, 255))
    img.putpixel((kx0 + 3, ky0 + 3), (*SASH_SHADE, 255))
    for yy in range(ky0 + 4, ky0 + 7):
        for xx in range(kx0 + 2, kx0 + 4):
            img.putpixel((xx, yy), (*SASH, 255))
        img.putpixel((kx0 + 3, yy), (*SASH_SHADE, 255))
    img.putpixel((kx0 + 2, ky0 + 6), (*SASH_SHADE, 255))
    for xx in range(kx0, kx0 + kw):  # gold hem
        img.putpixel((xx, ky0 + kh - 1), (*TRIM, 255))

    skirt_b = sl_faces(16, 62, 6, 8, 1)
    for name, rect in skirt_b.items():
        sl_paint_rect(img, rect, COAT, 7, 1950 + zlib.crc32(name.encode()) % 900)
    qx0, qy0, qw, qh = skirt_b["back"]
    for yy in range(qy0, qy0 + qh - 1):  # centre pleat shading
        img.putpixel((qx0 + qw // 2, yy), (*COAT_SHADE, 255))
    for xx in range(qx0, qx0 + qw):  # gold hem
        img.putpixel((xx, qy0 + qh - 1), (*TRIM, 255))

    skirt_s = sl_faces(0, 72, 1, 8, 4)
    for name, rect in skirt_s.items():
        sl_paint_rect(img, rect, COAT, 7, 1980 + zlib.crc32(name.encode()) % 900)
    # sash wrap band + gold hem on every tall face (shared by both side panels)
    for name in ("front", "back", "left", "right"):
        x0, y0, w, h = skirt_s[name]
        for yy in range(y0 + 1, y0 + 4):
            for xx in range(x0, x0 + w):
                img.putpixel((xx, yy), (*SASH, 255))
        for xx in range(x0, x0 + w):
            img.putpixel((xx, y0 + 3), (*SASH_SHADE, 255))
            img.putpixel((xx, y0 + h - 1), (*TRIM, 255))

    # ---- tricorn hat: crown 7x6x7 at (44,40)
    crown = sl_faces(44, 40, 7, 6, 7)
    for name, rect in crown.items():
        sl_paint_rect(img, rect, HAT, 5, 1600 + zlib.crc32(name.encode()) % 900)
    # wide flat brim 13x1x13 at (44,64)
    brim = sl_faces(44, 64, 13, 1, 13)
    for name, rect in brim.items():
        sl_paint_rect(img, rect, HAT, 5, 1700 + zlib.crc32(name.encode()) % 900)
    # lighter trim along the brim edge on the top face
    tx, ty, tw, th = brim["top"]
    for xx in range(tx, tx + tw):
        img.putpixel((xx, ty), (HAT_TRIM[0], HAT_TRIM[1], HAT_TRIM[2], 255))
        img.putpixel((xx, ty + th - 1), (HAT_TRIM[0], HAT_TRIM[1], HAT_TRIM[2], 255))
    for yy in range(ty, ty + th):
        img.putpixel((tx, yy), (HAT_TRIM[0], HAT_TRIM[1], HAT_TRIM[2], 255))
        img.putpixel((tx + tw - 1, yy), (HAT_TRIM[0], HAT_TRIM[1], HAT_TRIM[2], 255))
    # three upturned brim flaps 11x1x7 sharing UV at (44,92)
    flap = sl_faces(44, 92, 11, 1, 7)
    for name, rect in flap.items():
        sl_paint_rect(img, rect, HAT, 5, 1800 + zlib.crc32(name.encode()) % 900)
    fx2, fy2, fw2, fh2 = flap["top"]
    for xx in range(fx2, fx2 + fw2):
        img.putpixel((xx, fy2), (HAT_TRIM[0], HAT_TRIM[1], HAT_TRIM[2], 255))

    img.save(os.path.join(ENTITY_DIR, "still_life.png"))
    print("wrote textures/entity/still_life.png")


still_life_texture()

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

    # the Still Life silhouette, tall and lanky, far down the hall
    cx = vp[0]
    feet = vp[1] + 30
    head_top = feet - 64
    # tricorn hat: wide brim with three upturned corners
    d.polygon([(cx - 13, head_top + 2), (cx, head_top - 9),
               (cx + 13, head_top + 2), (cx + 8, head_top + 4),
               (cx - 8, head_top + 4)], fill=(8, 7, 6))
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
