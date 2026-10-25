#!/usr/bin/env python3
"""
Builds the Garrison's art: the Bolt Warden's geometry and textures, and the
Emplacement's block models and textures.

The warden's boxes are defined once, in WARDEN below. From them the script
packs the texture layout, paints one texture per wood, three crack overlays and
the glow layer, and writes the matching createBodyLayer() into
GarrisonGolemModel.java between its GENERATED markers. Change a box here, not
in the Java.

The emplacement's elements are defined in EMPLACEMENT_* below and written as
block models: the static stand, the swivel yoke and the cradle (both turned by
the block-entity renderer), and an item model that has all three.

Colours are tone ramps picked from the vanilla stripped logs, so a warden
matches the logs it was built from. No vanilla pixels are copied.

    python3 tools/garrison/make_garrison_art.py
    python3 tools/garrison/make_garrison_art.py --preview build/garrison_preview
"""

import argparse
import json
import math
import pathlib
import random
import re

from PIL import Image, ImageDraw

REPO = pathlib.Path(__file__).resolve().parent.parent.parent
ASSETS = REPO / "src/main/resources/assets/fletcherstrestle"
MODEL_JAVA = REPO / "src/main/java/net/frostytrix/fletcherstrestle/entity/client/GarrisonGolemModel.java"
TEX_W, TEX_H = 64, 64
CLEAR = (0, 0, 0, 0)

# ---------------------------------------------------------------- palettes

# Six side tones (dark to light) and four end-grain tones per wood, read off
# the vanilla stripped log textures.
WOODS = {
    'oak': ([(147, 117, 68), (163, 130, 73), (176, 142, 85), (185, 149, 88), (192, 157, 98), (194, 161, 102)],
            [(125, 103, 57), (150, 116, 65), (175, 143, 85), (184, 148, 95)]),
    'spruce': ([(97, 81, 45), (103, 86, 49), (112, 89, 52), (121, 91, 55), (126, 94, 56), (134, 98, 56)],
               [(66, 54, 29), (97, 75, 46), (122, 90, 52), (130, 97, 58)]),
    'birch': ([(177, 152, 96), (186, 168, 116), (196, 172, 114), (203, 181, 122), (206, 188, 128), (211, 196, 135)],
              [(167, 142, 86), (174, 159, 118), (200, 183, 122), (215, 193, 133)]),
    'jungle': ([(151, 124, 69), (155, 128, 73), (174, 128, 84), (180, 134, 90), (187, 141, 97), (191, 145, 101)],
               [(151, 106, 68), (158, 131, 76), (170, 121, 84), (184, 135, 100)]),
    'acacia': ([(150, 88, 55), (169, 88, 58), (178, 91, 59), (172, 96, 63), (181, 97, 62), (196, 99, 64)],
               [(153, 80, 43), (153, 92, 59), (173, 93, 50), (186, 99, 55)]),
    'dark_oak': ([(59, 48, 32), (69, 54, 35), (72, 57, 36), (74, 58, 36), (80, 61, 38), (82, 63, 39)],
                 [(53, 43, 28), (58, 36, 17), (73, 47, 23), (79, 50, 24)]),
    'mangrove': ([(109, 44, 45), (117, 49, 49), (122, 55, 49), (122, 55, 49), (122, 61, 47), (128, 69, 51)],
                 [(93, 28, 30), (111, 42, 45), (111, 46, 45), (119, 57, 52)]),
    'cherry': ([(205, 133, 128), (210, 137, 141), (214, 143, 147), (217, 148, 153), (220, 152, 158), (223, 156, 164)],
               [(201, 125, 121), (221, 157, 151), (230, 181, 173), (231, 190, 180)]),
    'crimson': ([(120, 44, 73), (128, 56, 83), (137, 57, 91), (137, 57, 91), (150, 62, 98), (160, 68, 104)],
                [(92, 48, 66), (128, 56, 83), (126, 58, 86), (148, 63, 97)]),
    'warped': ([(46, 133, 120), (49, 141, 140), (59, 153, 151), (59, 153, 151), (68, 161, 159), (61, 169, 166)],
               [(46, 95, 81), (57, 131, 130), (49, 141, 140), (59, 153, 151)]),
    'bamboo': ([(144, 126, 58), (153, 135, 65), (200, 177, 77), (194, 181, 78), (217, 194, 94), (227, 204, 106)],
               [(117, 100, 37), (163, 145, 62), (217, 194, 94), (227, 204, 106)]),
}

IRON = [(34, 34, 38), (54, 54, 60), (78, 78, 86), (104, 104, 112), (138, 138, 146), (180, 180, 188)]
BRASS = [(70, 44, 16), (108, 72, 26), (150, 106, 38), (192, 146, 56), (224, 188, 94), (246, 222, 150)]
SOCKET = (26, 18, 12)
HOLLOW = (30, 22, 14)
CRACK = (44, 30, 18)
EYE_GLOW = (255, 178, 70)
HEART_GLOW = (150, 78, 22)


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, v))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def scale(c, k):
    return tuple(clamp(int(round(v * k))) for v in c)


def saturate(c, k):
    g = sum(c) / 3.0
    return tuple(clamp(int(round(g + (v - g) * k))) for v in c)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


class Wood:
    """A wood's tones. ramp[3..8] are the side tones; 0..2 are darker, 9 lighter."""

    def __init__(self, side, top):
        side = sorted(side, key=lum)
        lo, hi = side[0], side[-1]
        base = [mix(mix(lo, hi, i / 5.0), side[i], 0.5) for i in range(6)]
        darks = [saturate(scale(lo, k), 1.12) for k in (0.40, 0.56, 0.74)]
        self.ramp = darks + base + [scale(hi, 1.07)]
        self.top = sorted(top, key=lum)

    def tone(self, i):
        return self.ramp[clamp(i, 0, len(self.ramp) - 1)]


def h(*values):
    """A stable hash of some integers, in [0, 1)."""
    x = 0x9E3779B9
    for v in values:
        x ^= (int(v) & 0xFFFFFFFF) + 0x9E3779B9 + ((x << 6) & 0xFFFFFFFF) + (x >> 2)
        x &= 0xFFFFFFFF
    x ^= x >> 16
    x = (x * 0x45D9F3B) & 0xFFFFFFFF
    x ^= x >> 16
    x = (x * 0x45D9F3B) & 0xFFFFFFFF
    x ^= x >> 16
    return x / 4294967296.0


# ---------------------------------------------------------------- warden geometry

class Part:
    def __init__(self, name, parent, pivot, rot=(0.0, 0.0, 0.0)):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.boxes = []

    def abs_pivot(self, parts):
        if self.parent is None:
            return self.pivot
        p = parts[self.parent].abs_pivot(parts)
        return tuple(p[i] + self.pivot[i] for i in range(3))


class Box:
    def __init__(self, name, origin, size, mat, grow=(0.0, 0.0, 0.0), axis='y', tone=0):
        self.name, self.origin, self.size, self.mat = name, origin, size, mat
        self.grow, self.axis, self.tone = grow, axis, tone
        self.uv = None
        self.part = None

    def footprint(self):
        w, hh, d = self.size
        return 2 * d + 2 * w, d + hh


def warden():
    """The Bolt Warden: a wooden toy soldier with a clockwork heart. Model space, y down, feet at 24."""
    parts = {}

    def part(name, parent, pivot, rot=(0.0, 0.0, 0.0), boxes=()):
        p = Part(name, parent, pivot, rot)
        for b in boxes:
            b.part = p
            p.boxes.append(b)
        parts[name] = p
        return p

    part('body', None, (0, 14, 0), boxes=[
        Box('torso_core', (-3, -8, -3), (6, 8, 6), 'wood'),
        Box('torso_wide', (-4, -8, -2), (8, 8, 4), 'wood', grow=(0, -0.02, 0)),
        Box('belt_core', (-3, -2, -3), (6, 1, 6), 'iron', grow=(0.3, 0.3, 0.3)),
        Box('belt_wide', (-4, -2, -2), (8, 1, 4), 'iron', grow=(0.3, 0.28, 0.3)),
    ])
    part('heart', 'body', (0, -5, -2.6), boxes=[
        Box('spoke_h', (-2, -0.5, -0.5), (4, 1, 1), 'brass', grow=(0, 0, -0.25)),
        Box('spoke_v', (-0.5, -2, -0.5), (1, 4, 1), 'brass', grow=(0, 0, -0.27)),
    ])
    part('key', 'body', (0, -4.5, 3), boxes=[
        Box('key_shaft', (-0.5, -0.5, 0), (1, 1, 2), 'iron'),
    ])
    part('key_bow', 'key', (0, 0, 2), boxes=[
        Box('key_lobe_left', (-3.5, -1.5, -0.5), (3, 3, 1), 'brass'),
        Box('key_lobe_right', (0.5, -1.5, -0.5), (3, 3, 1), 'brass'),
        Box('key_bridge', (-0.5, -0.5, -0.5), (1, 1, 1), 'brass'),
    ])
    part('yoke', 'body', (0, -8, 0), boxes=[
        Box('beam', (-8, -3, -2), (16, 3, 4), 'wood', axis='x'),
    ])
    part('head', 'yoke', (0, -3, 0), boxes=[
        Box('skull', (-3, -6, -3), (6, 6, 6), 'wood'),
        Box('nose', (-1, -3, -4), (2, 1, 1), 'wood', tone=1),
    ])
    part('hat', 'head', (0, -6, 0), boxes=[
        Box('crown', (-3, -4, -3), (6, 4, 6), 'wood', grow=(0.3, 0.3, 0.3), tone=-3),
        Box('pompom', (-1, -6.3, -1), (2, 2, 2), 'brass'),
    ])
    part('visor', 'head', (0, -6, -3.3), rot=(0.2, 0.0, 0.0), boxes=[
        Box('peak', (-3, -0.5, -2.5), (6, 1, 3), 'wood', grow=(0.3, -0.25, 0), tone=-4),
    ])
    for side, x in (('right', -6), ('left', 6)):
        part(side + '_arm', 'yoke', (x, 0, 0), boxes=[
            Box(side + '_upper_arm', (-1.5, -1, -1.5), (3, 5, 3), 'wood'),
        ])
        part(side + '_forearm', side + '_arm', (0, 4, 0), boxes=[
            Box(side + '_forearm', (-1, 0, -1), (2, 3, 2), 'wood'),
            Box(side + '_hand', (-1.5, 3, -1.5), (3, 2, 3), 'wood', tone=1),
        ])
    for side, x in (('right', -2), ('left', 2)):
        part(side + '_leg', None, (x, 14, 0), boxes=[
            Box(side + '_thigh', (-1.5, 0, -1.5), (3, 4, 3), 'wood'),
        ])
        part(side + '_shin', side + '_leg', (0, 4, 0), boxes=[
            Box(side + '_shin', (-1, 0, -1), (2, 4, 2), 'wood', tone=-1),
            Box(side + '_boot', (-1.5, 4, -2.5), (3, 2, 4), 'wood', tone=-3),
        ])
    return parts


def all_boxes(parts):
    return [b for p in parts.values() for b in p.boxes]


def pack(boxes):
    """Shelf-packs the boxes' texture footprints, tallest first."""
    order = sorted(boxes, key=lambda b: (-b.footprint()[1], -b.footprint()[0], b.name))
    x = y = shelf = 0
    for b in order:
        w, hh = b.footprint()
        if x + w > TEX_W:
            x, y, shelf = 0, y + shelf, 0
        b.uv = (x, y)
        x += w
        shelf = max(shelf, hh)
    if y + shelf > TEX_H:
        raise SystemExit(f"texture layout needs {y + shelf} rows, more than {TEX_H}")


def face_rects(b):
    u, v = b.uv
    w, hh, d = b.size
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'west': (u, v + d, d, hh),
        'north': (u + d, v + d, w, hh),
        'east': (u + d + w, v + d, d, hh),
        'south': (u + 2 * d + w, v + d, w, hh),
    }


def face_point(face, a, r, size):
    """The box-local point of face coordinate (a, r); texel centres are at +0.5."""
    w, hh, d = size
    if face == 'north':
        return (a, r, 0)
    if face == 'south':
        return (w - a, r, d)
    if face == 'west':
        return (0, r, d - a)
    if face == 'east':
        return (w, r, a)
    if face == 'top':
        return (a, 0, d - r)
    return (a, hh, d - r)


def is_end(axis, face):
    return face in {'y': ('top', 'bottom'), 'x': ('west', 'east'), 'z': ('north', 'south')}[axis]


# ---------------------------------------------------------------- warden painting

def wood_texel(wood, b, face, c, r, fw, fh, seed):
    w, hh, d = b.size
    lp = face_point(face, c + 0.5, r + 0.5, b.size)
    if is_end(b.axis, face):
        if b.axis == 'y':
            a, q, ha, hq = lp[0] - w / 2, lp[2] - d / 2, w / 2, d / 2
        else:
            a, q, ha, hq = lp[2] - d / 2, lp[1] - hh / 2, d / 2, hh / 2
        if c in (0, fw - 1) or r in (0, fh - 1) or fw <= 2 or fh <= 2:
            col = wood.tone(6 + b.tone)
            return col if h(seed, c, r, 3) > 0.25 else wood.tone(5 + b.tone)
        rn = math.sqrt((a / max(ha, 0.5)) ** 2 + (q / max(hq, 0.5)) ** 2)
        if rn < 0.3:
            col = wood.top[0]
        else:
            ring = int(rn * 3.0 + h(seed, c, r) * 0.35)
            col = wood.top[2] if ring % 2 else wood.top[1]
            if ring % 2 == 0 and h(seed, c, r, 9) > 0.8:
                col = wood.top[3]
        return scale(col, 0.88 ** -b.tone) if b.tone < 0 else col
    if b.axis == 'y':
        ang = math.atan2(lp[2] - d / 2, lp[0] - w / 2)
        along, per = lp[1], 2 * (w + d)
    else:
        ang = math.atan2(lp[2] - d / 2, lp[1] - hh / 2)
        along, per = lp[0], 2 * (hh + d)
    stripe = int(math.floor((ang + math.pi) / (2 * math.pi) * per))
    rnd = h(seed, stripe)
    base = 4 if rnd < 0.18 else 5 if rnd < 0.55 else 6 if rnd < 0.86 else 7
    seg = int(math.floor((along + h(seed, stripe, 1) * 5) / 3))
    q = h(seed, stripe, seg)
    if q < 0.18:
        base -= 1
    elif q > 0.87:
        base += 1
    if h(seed, stripe // 2, int(along // 4), 7) < 0.03:
        base = 2
    if b.axis == 'y' and face not in ('top', 'bottom'):
        if r == 0:
            base += 1
        elif r == fh - 1:
            base -= 1
        if (c == 0 or c == fw - 1) and h(seed, c, r, 5) < 0.55:
            base -= 1
    elif b.axis == 'x':
        if face in ('north', 'south') and r == 0:
            base += 1
        if face in ('north', 'south') and r == fh - 1:
            base -= 1
    return wood.tone(base + b.tone)


def metal_texel(ramp, b, face, c, r, fw, fh, seed):
    i = 3
    if face == 'top':
        i = 4
    elif face == 'bottom':
        i = 2
    elif r == 0 and fh > 1:
        i = 4
    elif r == fh - 1 and fh > 1:
        i = 2
    if h(seed, c, r, 11) < 0.15:
        i -= 1
    return ramp[clamp(i, 0, 5)]


def decorate(b, face, c, r, fw, fh, base, wood):
    """Painted details. Returns a colour, CLEAR for a hole, or None to keep the base."""
    n = b.name
    if n == 'torso_core' and face == 'north':
        corner = (r in (1, 4)) and (c in (1, 4))
        if 1 <= r <= 4 and 1 <= c <= 4 and not corner:
            return CLEAR
        ring = (r in (0, 5) and 1 <= c <= 4) or (c in (0, 5) and 1 <= r <= 4) or corner
        if ring:
            # Lit from the top left: bright there, deep in the bottom right.
            if corner:
                return BRASS[4] if (r, c) == (1, 1) else BRASS[2] if (r, c) == (4, 4) else BRASS[3]
            if r == 0 or c == 0:
                return BRASS[5] if (r + c) % 2 else BRASS[4]
            return BRASS[3] if (r + c) % 2 else BRASS[2]
    if n == 'torso_wide' and face == 'north' and 1 <= r <= 4 and 2 <= c <= 5:
        ir, ic = r - 1, c - 2
        if ir in (0, 3) and ic in (0, 3):
            return HOLLOW
        if ir in (0, 3) or ic in (0, 3):
            return IRON[2] if (ir + ic) % 2 else IRON[1]
        return HOLLOW
    if n == 'spoke_h' and face == 'north':
        return BRASS[5] if c in (0, 3) else BRASS[4]
    if n == 'spoke_v' and face == 'north':
        return BRASS[5] if r in (0, 3) else BRASS[4]
    if n in ('spoke_h', 'spoke_v'):
        return BRASS[3]
    if n == 'torso_core' and face == 'south' and 2 <= r <= 4 and 2 <= c <= 3:
        return BRASS[3] if r == 2 else BRASS[2] if r == 4 else BRASS[1]
    if n == 'belt_core' and face == 'north' and c in (2, 3):
        return BRASS[5] if c == 2 else BRASS[4]
    if n == 'belt_core' and face in ('north', 'south', 'west', 'east') and c % 3 == 1:
        return IRON[4]
    if n == 'beam' and face in ('north', 'south') and r == 1 and c in (2, 13):
        return BRASS[4]
    if n == 'beam' and face in ('north', 'south') and r == 2 and c in (2, 13):
        return BRASS[1]
    if n == 'skull' and face == 'north':
        # A carved face: brows, deep-set eyes, a button nose (its own box, row 3)
        # over a handlebar moustache, and the shadow of the hat's peak on top.
        if r == 0:
            return wood.tone(3)
        if r == 1 and c in (1, 4):
            return wood.tone(2)
        if r == 2 and c in (1, 4):
            return SOCKET
        if (r == 3 and c in (0, 5)) or (r == 4 and 1 <= c <= 4):
            return wood.tone(1) if c in (2, 3) else wood.tone(0)
        if r == 5 and c in (2, 3):
            return wood.tone(4)
        if r == 5:
            return wood.tone(5)
    if n == 'skull' and face == 'west' and c == 3 and r >= 1:
        return IRON[2] if r % 2 else IRON[3]
    if n == 'skull' and face == 'east' and c == 2 and r >= 1:
        return IRON[2] if r % 2 else IRON[3]
    if n == 'skull' and face == 'bottom' and r == 3:
        return IRON[2]
    if n == 'crown' and face in ('north', 'south', 'west', 'east'):
        if r == fh - 1:
            return IRON[3] if c % 2 else IRON[2]
        if r == 0:
            return wood.tone(7 + b.tone)
    if n == 'crown' and face == 'north':
        if r == 1 and c in (1, 4):
            return BRASS[2]
        if r == 1 and c in (2, 3):
            return BRASS[5] if c == 2 else BRASS[4]
        if r == 2 and c in (2, 3):
            return BRASS[3]
    if n == 'pompom':
        return BRASS[5] if face == 'top' else BRASS[4] if r == 0 else BRASS[3]
    if n == 'nose' and face == 'north':
        return wood.tone(8) if r == 0 else wood.tone(6)
    if n.endswith('_hand') and face in ('north', 'south', 'west', 'east') and r == 0:
        return IRON[3] if c % 2 else IRON[2]
    if n.endswith('_forearm') and face in ('north', 'south', 'west', 'east') and r == 0:
        return BRASS[3] if c % 2 else BRASS[2]
    if n.endswith('_thigh') and face in ('north', 'south', 'west', 'east') and r == fh - 1:
        return IRON[3] if c % 2 else IRON[2]
    if n.endswith('_thigh') and face == 'north' and r == fh - 2 and c == 1:
        return IRON[4]
    if n.endswith('_boot'):
        if face == 'bottom' or (face != 'top' and r == fh - 1):
            return wood.tone(0)
        if face == 'north' and r == 0:
            return wood.tone(4)
    if n.startswith('key_lobe') and face in ('north', 'south') and c == 1 and r == 1:
        return CLEAR
    return None


def paint_warden(parts, wood_name, seed=7):
    wood = Wood(*WOODS[wood_name])
    img = Image.new('RGBA', (TEX_W, TEX_H), CLEAR)
    px = img.load()
    mask = {}
    for bi, b in enumerate(sorted(all_boxes(parts), key=lambda x: x.name)):
        for face, (u0, v0, fw, fh) in face_rects(b).items():
            for r in range(fh):
                for c in range(fw):
                    bseed = h(seed, bi) * 100000
                    if b.mat == 'wood':
                        col = wood_texel(wood, b, face, c, r, fw, fh, bseed)
                    elif b.mat == 'iron':
                        col = metal_texel(IRON, b, face, c, r, fw, fh, bseed)
                    else:
                        col = metal_texel(BRASS, b, face, c, r, fw, fh, bseed)
                    deco = decorate(b, face, c, r, fw, fh, col, wood)
                    if deco is not None:
                        col = deco
                    if col == CLEAR:
                        px[u0 + c, v0 + r] = CLEAR
                        continue
                    px[u0 + c, v0 + r] = col + (255,)
                    if b.mat == 'wood' and deco is None:
                        mask[(u0 + c, v0 + r)] = (b, face)
    return img, mask


def paint_glow(parts):
    img = Image.new('RGBA', (TEX_W, TEX_H), CLEAR)
    px = img.load()
    for b in all_boxes(parts):
        rects = face_rects(b)
        if b.name == 'skull':
            u0, v0, _, _ = rects['north']
            for c in (1, 4):
                px[u0 + c, v0 + 2] = EYE_GLOW + (255,)
        if b.name == 'torso_wide':
            u0, v0, _, _ = rects['north']
            for c in (3, 4):
                for r in (2, 3):
                    px[u0 + c, v0 + r] = HEART_GLOW + (255,)
    return img


def paint_cracks(parts, mask, seed=31):
    """Three cumulative overlays. Cracks run in from a face's edge, over wood only."""
    rnd = random.Random(seed)
    levels = [Image.new('RGBA', (TEX_W, TEX_H), CLEAR) for _ in range(3)]
    cracks = []
    for b in sorted(all_boxes(parts), key=lambda x: x.name):
        if b.mat != 'wood':
            continue
        for face, (u0, v0, fw, fh) in sorted(face_rects(b).items()):
            area = fw * fh
            if area < 10 or face == 'bottom':
                continue
            count = max(1, int(round(area / 20.0)))
            for _ in range(count):
                cracks.append((b, face, u0, v0, fw, fh))
    rnd.shuffle(cracks)
    for i, (b, face, u0, v0, fw, fh) in enumerate(cracks):
        level = 0 if i < len(cracks) * 0.22 else 1 if i < len(cracks) * 0.55 else 2
        edge = rnd.choice(('top', 'bottom', 'left', 'right'))
        if edge in ('top', 'bottom'):
            x, y = rnd.randrange(fw), 0 if edge == 'top' else fh - 1
            dx, dy = 0, 1 if edge == 'top' else -1
        else:
            x, y = 0 if edge == 'left' else fw - 1, rnd.randrange(fh)
            dx, dy = 1 if edge == 'left' else -1, 0
        length = rnd.randint(2, 3) + level
        lean = rnd.choice((-1, 1))
        pts = []
        for step in range(length):
            pts.append((x, y))
            # Each step goes inward and, more often than not, sideways: a jagged diagonal.
            if rnd.random() < 0.6:
                if dx == 0:
                    x += lean if rnd.random() < 0.8 else -lean
                else:
                    y += lean if rnd.random() < 0.8 else -lean
            x, y = x + dx, y + dy
            if not (0 <= x < fw and 0 <= y < fh):
                break
            if step == 1 and rnd.random() < 0.3:
                bx, by = x - (lean if dx == 0 else 0) * 2, y - (lean if dy == 0 else 0) * 2
                if 0 <= bx < fw and 0 <= by < fh:
                    pts.append((bx, by))
        for (cx, cy) in pts:
            key = (u0 + cx, v0 + cy)
            if key not in mask:
                continue
            for lv in range(level, 3):
                levels[lv].putpixel(key, CRACK + (255,))
    return levels


# ---------------------------------------------------------------- Java geometry

def jf(v):
    s = ('%.3f' % v).rstrip('0')
    if s.endswith('.'):
        s += '0'
    return s + 'F'


def camel(name):
    head, *rest = name.split('_')
    return head + ''.join(x.capitalize() for x in rest)


def java_geometry(parts):
    lines = [
        'MeshDefinition mesh = new MeshDefinition();',
        'PartDefinition root = mesh.getRoot();',
    ]
    for p in parts.values():
        parent = 'root' if p.parent is None else camel(p.parent)
        cubes = 'CubeListBuilder.create()'
        for b in p.boxes:
            o, s, g = b.origin, b.size, b.grow
            args = ', '.join(jf(v) for v in (*o, *s))
            if any(g):
                args += ', new CubeDeformation(%s)' % ', '.join(jf(v) for v in g)
            cubes += '\n        .texOffs(%d, %d).addBox(%s)' % (b.uv[0], b.uv[1], args)
        if any(p.rot):
            pose = 'PartPose.offsetAndRotation(%s)' % ', '.join(jf(v) for v in (*p.pivot, *p.rot))
        else:
            pose = 'PartPose.offset(%s)' % ', '.join(jf(v) for v in p.pivot)
        lines.append('PartDefinition %s = %s.addOrReplaceChild("%s", %s,\n        %s);'
                     % (camel(p.name), parent, p.name, cubes, pose))
    lines.append('return LayerDefinition.create(mesh, %d, %d);' % (TEX_W, TEX_H))
    return lines


def write_java(parts):
    text = MODEL_JAVA.read_text()
    begin = '// GENERATED by tools/garrison/make_garrison_art.py: begin'
    end = '// GENERATED by tools/garrison/make_garrison_art.py: end'
    m = re.search(r'( *)' + re.escape(begin) + r'.*?' + re.escape(end), text, re.S)
    if not m:
        raise SystemExit('GENERATED markers not found in ' + str(MODEL_JAVA))
    indent = m.group(1)
    body = []
    for line in java_geometry(parts):
        for sub in line.split('\n'):
            body.append(indent + sub)
    # Parts declared but never read again would warn; the variables are for the hierarchy.
    new = indent + begin + '\n' + '\n'.join(body) + '\n' + indent + end
    MODEL_JAVA.write_text(text[:m.start()] + new + text[m.end():])


# ---------------------------------------------------------------- emplacement

# Block space: 0..16, y up, front to the north. Textures are 16x16.
EMPLACEMENT_WOOD = Wood(*WOODS['oak'])


def paint_block_textures():
    wood = EMPLACEMENT_WOOD
    out = {}

    grain = Image.new('RGBA', (16, 16))
    for x in range(16):
        rnd = h(5, x)
        base = 4 if rnd < 0.2 else 5 if rnd < 0.55 else 6 if rnd < 0.85 else 7
        for y in range(16):
            t = base
            q = h(5, x, (y + int(h(5, x, 1) * 5)) // 3)
            if q < 0.2:
                t -= 1
            elif q > 0.86:
                t += 1
            if h(5, x // 2, y // 4, 9) < 0.025:
                t = 2
            grain.putpixel((x, y), wood.tone(t - 1) + (255,))
    out['emplacement_wood'] = grain
    out['emplacement_beam'] = grain.transpose(Image.Transpose.TRANSPOSE)

    end = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) * 0.7 + math.hypot(x - 7.5, y - 7.5) * 0.3
            ring = int(d / 1.6 + h(6, x, y) * 0.4)
            col = wood.top[2] if ring % 2 else wood.top[1]
            if d < 1.2:
                col = wood.top[0]
            end.putpixel((x, y), scale(col, 0.93) + (255,))
    out['emplacement_wood_end'] = end

    iron = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            row = y % 4
            i = 4 if row == 0 else 2 if row == 3 else 3
            if row == 1 and x % 4 == 1:
                i = 5
            elif row == 2 and x % 4 == 1:
                i = 1
            elif h(8, x, y) < 0.18:
                i -= 1
            iron.putpixel((x, y), IRON[i] + (255,))
    out['emplacement_iron'] = iron

    # Iron-banded wood. The turntable's sides sample rows 1 and 2: the band, then the wood.
    drum = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            if y in (1, 14):
                col = IRON[5] if x % 3 == 1 else IRON[3]
            elif y in (2, 15):
                col = wood.tone(3 + (1 if h(9, x) > 0.5 else 0))
            else:
                col = grain.getpixel((x, y))[:3]
            drum.putpixel((x, y), col + (255,))
    out['emplacement_drum'] = drum
    return out


def el(frm, to, tex, rot=None, name=''):
    """One block-model element. tex maps face -> texture key, or 'all'."""
    return {'name': name, 'from': list(frm), 'to': list(to), 'tex': tex, 'rotation': rot}


def leg(x_top, z0, west, name):
    """A trestle leg, splayed sideways from under the bar, on a flat iron foot."""
    angle = -22.5 if west else 22.5
    rot = {'angle': angle, 'axis': 'z', 'origin': [x_top, 10.5, z0 + 1]}
    wood = {'north': 'wood', 'south': 'wood', 'west': 'wood', 'east': 'wood', 'up': 'end', 'down': 'end'}
    foot = x_top - 11.5 * math.sin(math.radians(22.5)) * (1 if west else -1)
    iron = {f: 'iron' for f in ('north', 'south', 'west', 'east', 'up', 'down')}
    fx = min(max(foot - 1.5, 0), 13)
    return [
        el((x_top - 1, -1, z0), (x_top + 1, 10.5, z0 + 2), wood, rot, name),
        el((fx, 0, z0 - 0.5), (fx + 3, 1, z0 + 2.5), iron, None, name + ' foot'),
    ]


def emplacement_stand():
    along_x = {'north': 'beam', 'south': 'beam', 'up': 'beam', 'down': 'beam', 'west': 'end', 'east': 'end'}
    along_z = {'west': 'beam', 'east': 'beam', 'up': 'wood', 'down': 'wood', 'north': 'end', 'south': 'end'}
    banded = {'north': 'drum', 'south': 'drum', 'west': 'drum', 'east': 'drum', 'up': 'end', 'down': 'end'}
    iron = {f: 'iron' for f in ('north', 'south', 'west', 'east', 'up', 'down')}
    els = []
    els += leg(6, 3, True, 'front left leg')
    els += leg(6, 11, True, 'back left leg')
    els += leg(10, 3, False, 'front right leg')
    els += leg(10, 11, False, 'back right leg')
    els += [
        el((5, 10, 2), (11, 13, 14), along_z, None, 'trestle bar'),
        el((4.75, 11, 4.25), (11.25, 13.25, 5), iron, None, 'front bar band'),
        el((4.75, 11, 11), (11.25, 13.25, 11.75), iron, None, 'back bar band'),
        el((3, 4, 3.5), (13, 5.5, 4.5), along_x, None, 'front stretcher'),
        el((3, 4, 11.5), (13, 5.5, 12.5), along_x, None, 'back stretcher'),
        el((2, 1.5, 4), (3, 2.5, 12), along_z, None, 'left stretcher'),
        el((13, 1.5, 4), (14, 2.5, 12), along_z, None, 'right stretcher'),
        el((4, 13, 4), (12, 15, 12), banded, None, 'turntable'),
    ]
    return els


def emplacement_yoke():
    upright = {'north': 'wood', 'south': 'wood', 'west': 'wood', 'east': 'wood', 'up': 'end', 'down': 'end'}
    iron = {f: 'iron' for f in ('north', 'south', 'west', 'east', 'up', 'down')}
    return [
        el((4, 15, 4), (12, 16, 12), {'north': 'iron', 'south': 'iron', 'west': 'iron', 'east': 'iron',
                                      'up': 'end', 'down': 'end'}, None, 'swivel plate'),
        el((4, 16, 7), (6, 20, 9), upright, None, 'left fork'),
        el((10, 16, 7), (12, 20, 9), upright, None, 'right fork'),
        el((3.75, 20, 6.75), (6.25, 20.5, 9.25), iron, None, 'left fork cap'),
        el((9.75, 20, 6.75), (12.25, 20.5, 9.25), iron, None, 'right fork cap'),
        # Thinner than the cradle it runs through, so none of their faces share a plane.
        el((3.5, 18.125, 7.625), (12.5, 18.875, 8.375), iron, None, 'trunnion pin'),
    ]


def emplacement_cradle():
    along_z = {'west': 'beam', 'east': 'beam', 'up': 'wood', 'down': 'wood', 'north': 'end', 'south': 'end'}
    iron = {f: 'iron' for f in ('north', 'south', 'west', 'east', 'up', 'down')}
    return [
        el((6.5, 18, 3), (9.5, 19, 13), along_z, None, 'cradle'),
        el((6.25, 17.75, 4.5), (9.75, 19.25, 5.5), iron, None, 'front strap'),
        el((6.25, 17.75, 10.5), (9.75, 19.25, 11.5), iron, None, 'back strap'),
    ]


TEXTURE_KEYS = {
    'wood': 'fletcherstrestle:block/emplacement_wood',
    'beam': 'fletcherstrestle:block/emplacement_beam',
    'end': 'fletcherstrestle:block/emplacement_wood_end',
    'iron': 'fletcherstrestle:block/emplacement_iron',
    'drum': 'fletcherstrestle:block/emplacement_drum',
}


def auto_uv(face, frm, to):
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if face == 'north':
        uv = [16 - x2, 16 - y2, 16 - x1, 16 - y1]
    elif face == 'south':
        uv = [x1, 16 - y2, x2, 16 - y1]
    elif face == 'west':
        uv = [z1, 16 - y2, z2, 16 - y1]
    elif face == 'east':
        uv = [16 - z2, 16 - y2, 16 - z1, 16 - y1]
    elif face == 'up':
        uv = [x1, z1, x2, z2]
    else:
        uv = [x1, 16 - z2, x2, 16 - z1]
    # Elements that reach outside the block would sample past the texture; slide them back in.
    for a, b in ((0, 2), (1, 3)):
        lo, hi = min(uv[a], uv[b]), max(uv[a], uv[b])
        shift = -lo if lo < 0 else 16 - hi if hi > 16 else 0
        uv[a] += shift
        uv[b] += shift
    return [round(v, 3) for v in uv]


def model_json(elements, particle='wood', parent='block/block', display=None):
    used = sorted({t for e in elements for t in e['tex'].values()})
    textures = {k: TEXTURE_KEYS[k] for k in used}
    textures['particle'] = TEXTURE_KEYS[particle]
    out = {'parent': parent, 'textures': textures, 'elements': []}
    if display:
        out['display'] = display
    for e in elements:
        faces = {}
        for face, key in e['tex'].items():
            faces[face] = {'uv': auto_uv(face, e['from'], e['to']), 'texture': '#' + key}
        j = {'name': e['name'], 'from': e['from'], 'to': e['to']}
        if e['rotation']:
            j['rotation'] = e['rotation']
        j['faces'] = faces
        out['elements'].append(j)
    return out


def coplanar_faces(elements):
    """Pairs of unrotated faces in the same plane, facing the same way and overlapping: they z-fight."""
    def faces(e):
        (x1, y1, z1), (x2, y2, z2) = e['from'], e['to']
        out = []
        for f in e['tex']:
            if f in ('north', 'south'):
                out.append((f, z1 if f == 'north' else z2, (x1, x2), (y1, y2)))
            elif f in ('west', 'east'):
                out.append((f, x1 if f == 'west' else x2, (z1, z2), (y1, y2)))
            else:
                out.append((f, y2 if f == 'up' else y1, (x1, x2), (z1, z2)))
        return out
    flat = [e for e in elements if not e['rotation']]
    found = []
    for i, a in enumerate(flat):
        for b in flat[i + 1:]:
            for fa in faces(a):
                for fb in faces(b):
                    if fa[0] != fb[0] or abs(fa[1] - fb[1]) > 1e-6:
                        continue
                    if min(fa[2][1], fb[2][1]) - max(fa[2][0], fb[2][0]) > 1e-6 \
                            and min(fa[3][1], fb[3][1]) - max(fa[3][0], fb[3][0]) > 1e-6:
                        found.append(f"{a['name']} and {b['name']} ({fa[0]} at {fa[1]})")
    return found


def write_emplacement():
    for name, img in paint_block_textures().items():
        img.save(ASSETS / 'textures/block' / (name + '.png'))
    models = ASSETS / 'models/block'
    stand, yoke, cradle = emplacement_stand(), emplacement_yoke(), emplacement_cradle()
    for clash in coplanar_faces(stand + yoke + cradle):
        print('warning: z-fighting faces: ' + clash)
    item_display = {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, -1.25, 0], 'scale': [0.55, 0.55, 0.55]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, -1, 0], 'scale': [0.5, 0.5, 0.5]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    }
    files = {
        'emplacement.json': model_json(stand),
        'emplacement_yoke.json': model_json(yoke),
        'emplacement_cradle.json': model_json(cradle),
        'emplacement_item.json': model_json(stand + yoke + cradle, display=item_display),
    }
    for name, data in files.items():
        (models / name).write_text(json.dumps(data, indent=2) + '\n')
    return stand, yoke, cradle


# ---------------------------------------------------------------- previews

def rot_zyx(v, rx, ry, rz):
    x, y, z = v
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x, y, z)


def part_chain(parts, name, pose):
    chain = []
    p = parts[name]
    while p is not None:
        chain.append(p)
        p = parts[p.parent] if p.parent else None
    return list(reversed(chain))


def to_world(parts, part_name, v, pose):
    """Applies ModelPart's translate-then-rotateZYX from the part up to the root."""
    for p in reversed(part_chain(parts, part_name, pose)):
        rx, ry, rz = p.rot
        dx, dy, dz = pose.get(p.name, (0, 0, 0))
        v = rot_zyx(v, rx + dx, ry + dy, rz + dz)
        v = tuple(v[i] + p.pivot[i] for i in range(3))
    return v


FACE_NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0),
                'top': (0, -1, 0), 'bottom': (0, 1, 0)}


def render_quads(quads, view_yaw, view_pitch, size, px_scale, offset):
    img = Image.new('RGBA', size, (40, 44, 52, 255))
    draw = ImageDraw.Draw(img)
    light = (-0.4, -0.75, -0.5)
    ll = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ll for c in light)
    out = []
    for pts, normal, col in quads:
        pts = [rot_zyx(rot_zyx(p, 0, view_yaw, 0), view_pitch, 0, 0) for p in pts]
        n = rot_zyx(rot_zyx(normal, 0, view_yaw, 0), view_pitch, 0, 0)
        if n[2] > 0.001:
            continue
        shade = 0.55 + 0.45 * max(0.0, -(n[0] * light[0] + n[1] * light[1] + n[2] * light[2]))
        depth = sum(p[2] for p in pts) / 4
        poly = [(offset[0] + p[0] * px_scale, offset[1] + p[1] * px_scale) for p in pts]
        out.append((depth, poly, scale(col[:3], shade)))
    out.sort(key=lambda t: -t[0])
    for _, poly, col in out:
        draw.polygon(poly, fill=col + (255,))
    return img


def warden_quads(parts, tex, pose, overlay=None):
    quads = []
    for p in parts.values():
        for b in p.boxes:
            w, hh, d = b.size
            g = b.grow
            for face, (u0, v0, fw, fh) in face_rects(b).items():
                for r in range(fh):
                    for c in range(fw):
                        col = tex.getpixel((u0 + c, v0 + r))
                        if overlay is not None and overlay.getpixel((u0 + c, v0 + r))[3] > 0:
                            col = overlay.getpixel((u0 + c, v0 + r))
                        if col[3] == 0:
                            continue
                        corners = []
                        for (a, q) in ((c, r), (c + 1, r), (c + 1, r + 1), (c, r + 1)):
                            lp = face_point(face, a, q, b.size)
                            lp = tuple(b.origin[i] - g[i] + lp[i] * ((b.size[i] + 2 * g[i]) / b.size[i])
                                       for i in range(3))
                            corners.append(to_world(parts, p.name, lp, pose))
                        o = to_world(parts, p.name, (0, 0, 0), pose)
                        n = to_world(parts, p.name, FACE_NORMALS[face], pose)
                        normal = tuple(n[i] - o[i] for i in range(3))
                        quads.append((corners, normal, col))
    return quads


def preview_warden(parts, outdir, poses):
    outdir.mkdir(parents=True, exist_ok=True)
    tiles = []
    for wood_name in WOODS:
        tex = Image.open(ASSETS / 'textures/entity/garrison_golem' / (wood_name + '.png')).convert('RGBA')
        front = render_quads(warden_quads(parts, tex, {}), math.radians(-30), math.radians(-12), (300, 520), 13, (150, 140))
        back = render_quads(warden_quads(parts, tex, {}), math.radians(150), math.radians(-12), (300, 520), 13, (150, 140))
        tile = Image.new('RGBA', (600, 520))
        tile.paste(front, (0, 0))
        tile.paste(back, (300, 0))
        tiles.append(tile)
    sheet = Image.new('RGBA', (600 * 4, 520 * 3), (20, 20, 20, 255))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 4) * 600, (i // 4) * 520))
    sheet.save(outdir / 'warden_woods.png')
    oak = Image.open(ASSETS / 'textures/entity/garrison_golem/oak.png').convert('RGBA')
    cracks = Image.open(ASSETS / 'textures/entity/garrison_golem/crackiness_high.png').convert('RGBA')
    big = Image.new('RGBA', (1120, 900))
    big.paste(render_quads(warden_quads(parts, oak, {}), math.radians(-28), math.radians(-10), (560, 900), 24, (280, 260)), (0, 0))
    big.paste(render_quads(warden_quads(parts, oak, {}), math.radians(152), math.radians(-10), (560, 900), 24, (280, 260)), (560, 0))
    big.save(outdir / 'warden_oak.png')
    render_quads(warden_quads(parts, oak, {}, cracks), math.radians(-28), math.radians(-10), (560, 900), 24,
                 (280, 260)).save(outdir / 'warden_cracked.png')
    for name, pose in poses.items():
        img = Image.new('RGBA', (1120, 900))
        img.paste(render_quads(warden_quads(parts, oak, pose), math.radians(-35), math.radians(-10), (560, 900), 24, (280, 260)), (0, 0))
        img.paste(render_quads(warden_quads(parts, oak, pose), math.radians(-100), math.radians(-5), (560, 900), 24, (280, 260)), (560, 0))
        img.save(outdir / ('warden_pose_' + name + '.png'))
    Image.open(ASSETS / 'textures/entity/garrison_golem/oak.png').resize((TEX_W * 8, TEX_H * 8), Image.NEAREST) \
        .save(outdir / 'warden_texture_oak.png')


def element_quads(elements, textures, pivot_yaw=0.0):
    """Block elements as texel quads in a y-down space, to share the warden's renderer."""
    quads = []
    for e in elements:
        (x1, y1, z1), (x2, y2, z2) = e['from'], e['to']
        rot = e['rotation']
        for face, key in e['tex'].items():
            tex = textures[key]
            uv = auto_uv(face, e['from'], e['to'])
            nu = max(1, int(round(abs(uv[2] - uv[0]))))
            nv = max(1, int(round(abs(uv[3] - uv[1]))))

            def point(a, b):
                # a, b in [0, 1] across the face, matching the auto UV layout.
                if face == 'north':
                    return (x2 - a * (x2 - x1), y2 - b * (y2 - y1), z1)
                if face == 'south':
                    return (x1 + a * (x2 - x1), y2 - b * (y2 - y1), z2)
                if face == 'west':
                    return (x1, y2 - b * (y2 - y1), z1 + a * (z2 - z1))
                if face == 'east':
                    return (x2, y2 - b * (y2 - y1), z2 - a * (z2 - z1))
                if face == 'up':
                    return (x1 + a * (x2 - x1), y2, z1 + b * (z2 - z1))
                return (x1 + a * (x2 - x1), y1, z2 - b * (z2 - z1))

            normal = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0),
                      'up': (0, 1, 0), 'down': (0, -1, 0)}[face]
            for j in range(nv):
                for i in range(nu):
                    tu = uv[0] + (uv[2] - uv[0]) * (i + 0.5) / nu
                    tv = uv[1] + (uv[3] - uv[1]) * (j + 0.5) / nv
                    col = tex.getpixel((clamp(int(tu), 0, 15), clamp(int(tv), 0, 15)))
                    pts = [point(i / nu, j / nv), point((i + 1) / nu, j / nv),
                           point((i + 1) / nu, (j + 1) / nv), point(i / nu, (j + 1) / nv)]
                    n = normal
                    if rot:
                        o = rot['origin']
                        ang = math.radians(rot['angle'])
                        ax = rot['axis']

                        def rr(p, origin=o, ang=ang, ax=ax):
                            p = [p[k] - origin[k] for k in range(3)]
                            if ax == 'z':
                                p = [p[0] * math.cos(ang) - p[1] * math.sin(ang),
                                     p[0] * math.sin(ang) + p[1] * math.cos(ang), p[2]]
                            elif ax == 'x':
                                p = [p[0], p[1] * math.cos(ang) - p[2] * math.sin(ang),
                                     p[1] * math.sin(ang) + p[2] * math.cos(ang)]
                            else:
                                p = [p[0] * math.cos(ang) + p[2] * math.sin(ang), p[1],
                                     -p[0] * math.sin(ang) + p[2] * math.cos(ang)]
                            return [p[k] + origin[k] for k in range(3)]
                        pts = [rr(p) for p in pts]
                        n = rr(n, (0, 0, 0))
                    # To the preview's y-down frame, centred on the block.
                    pts = [(p[0] - 8, -(p[1] - 8), p[2] - 8) for p in pts]
                    n = (n[0], -n[1], n[2])
                    if pivot_yaw:
                        pts = [rot_zyx(p, 0, pivot_yaw, 0) for p in pts]
                        n = rot_zyx(n, 0, pivot_yaw, 0)
                    quads.append((pts, n, col))
    return quads


def preview_emplacement(stand, yoke, cradle, outdir):
    textures = {k: Image.open(ASSETS / 'textures/block' / (TEXTURE_KEYS[k].split('/')[-1] + '.png')).convert('RGBA')
                for k in TEXTURE_KEYS}
    quads = element_quads(stand + yoke + cradle, textures)
    a = render_quads(quads, math.radians(-35), math.radians(-25), (520, 560), 20, (260, 330))
    b = render_quads(quads, math.radians(145), math.radians(-25), (520, 560), 20, (260, 330))
    turned = element_quads(stand, textures) + element_quads(yoke + cradle, textures, math.radians(30))
    c = render_quads(turned, math.radians(-90), math.radians(-5), (520, 560), 20, (260, 330))
    sheet = Image.new('RGBA', (1560, 560))
    for i, img in enumerate((a, b, c)):
        sheet.paste(img, (520 * i, 0))
    sheet.save(outdir / 'emplacement.png')


# ---------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', type=pathlib.Path, help='write preview renders to this folder')
    args = ap.parse_args()

    parts = warden()
    pack(all_boxes(parts))
    out = ASSETS / 'textures/entity/garrison_golem'
    out.mkdir(parents=True, exist_ok=True)
    mask = None
    for wood_name in WOODS:
        img, m = paint_warden(parts, wood_name)
        img.save(out / (wood_name + '.png'))
        if wood_name == 'oak':
            mask = m
    paint_glow(parts).save(out / 'glow.png')
    for level, img in zip(('low', 'medium', 'high'), paint_cracks(parts, mask)):
        img.save(out / ('crackiness_' + level + '.png'))
    write_java(parts)
    stand, yoke, cradle = write_emplacement()

    if args.preview:
        poses = {}
        preview_warden(parts, args.preview, poses)
        preview_emplacement(stand, yoke, cradle, args.preview)


if __name__ == '__main__':
    main()
