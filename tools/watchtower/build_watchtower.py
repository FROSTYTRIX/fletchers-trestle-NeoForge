#!/usr/bin/env python3
"""
Builds the Ranger's Watchtower and writes it as a structure template,
data/fletcherstrestle/structure/ranger_watchtower.nbt.

An abandoned rangers' post: a stone base with arrow slits and a bunk room, a
timber trestle tower above it climbed by a ladder up its middle post, and a
lookout platform under a hipped roof. On the platform stand three
emplacements (one still holding a worn crossbow) and the unwound body of a
Bolt Warden, a T of stripped spruce logs; the chest beside it always holds a
Mechanical Trigger to wind it back up. An eagle has nested on the peak.

The build lives in build() below, block by block. Like any structure
template it can also be loaded in game with a Structure Block, changed and
saved back over the .nbt; after that, edit the .nbt rather than rerun this.

    python3 tools/watchtower/build_watchtower.py
    python3 tools/watchtower/build_watchtower.py --preview build/watchtower_preview
"""

import argparse
import gzip
import math
import pathlib
import struct
import sys

REPO = pathlib.Path(__file__).resolve().parent.parent.parent
OUT = REPO / 'src/main/resources/data/fletcherstrestle/structure/ranger_watchtower.nbt'
DATA_VERSION = 3955  # 1.21.1

SIZE = (11, 28, 14)

# ---------------------------------------------------------------- NBT


class Byte(int):
    pass


class Long(int):
    pass


class Double(float):
    pass


def _payload(value):
    """(tag id, encoded payload) for a Python value."""
    if isinstance(value, Byte):
        return 1, struct.pack('>b', value)
    if isinstance(value, Long):
        return 4, struct.pack('>q', value)
    if isinstance(value, bool):
        return 1, struct.pack('>b', int(value))
    if isinstance(value, int):
        return 3, struct.pack('>i', value)
    if isinstance(value, (Double, float)):
        return 6, struct.pack('>d', value)
    if isinstance(value, str):
        raw = value.encode('utf-8')
        return 8, struct.pack('>H', len(raw)) + raw
    if isinstance(value, list):
        if not value:
            return 9, struct.pack('>bi', 0, 0)
        parts = [_payload(v) for v in value]
        return 9, struct.pack('>bi', parts[0][0], len(parts)) + b''.join(p[1] for p in parts)
    if isinstance(value, dict):
        out = b''
        for k, v in value.items():
            tag, body = _payload(v)
            key = k.encode('utf-8')
            out += struct.pack('>bH', tag, len(key)) + key + body
        return 10, out + b'\x00'
    raise TypeError(type(value))


def write_nbt(path, root):
    tag, body = _payload(root)
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(path, 'wb') as f:
        f.write(struct.pack('>bH', tag, 0) + body)


# ---------------------------------------------------------------- template


class Template:
    def __init__(self, size):
        self.size = size
        self.blocks = {}

    def set(self, x, y, z, name, props=None, nbt=None):
        assert 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2], (x, y, z)
        if ':' not in name:
            name = 'minecraft:' + name
        self.blocks[(x, y, z)] = (name, dict(props or {}), nbt)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, name, props)

    def air(self, x0, y0, z0, x1, y1, z1):
        """Clears a volume (trees, grass) wherever nothing else was placed."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, 'air')

    def to_nbt(self):
        palette, index, blocks = [], {}, []
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            name, props, nbt = self.blocks[pos]
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = {'Name': name}
                if props:
                    entry['Properties'] = {k: str(v).lower() for k, v in props.items()}
                palette.append(entry)
            block = {'pos': list(pos), 'state': index[key]}
            if nbt is not None:
                block['nbt'] = nbt
            blocks.append(block)
        return {'DataVersion': DATA_VERSION, 'size': list(self.size), 'palette': palette, 'blocks': blocks,
                'entities': []}


# ---------------------------------------------------------------- the build

SOLID = ('stone_bricks', 'cobblestone', 'mossy', 'planks', '_log', '_wood', 'hay_block', 'barrel')


def is_solid(entry):
    return entry is not None and any(s in entry[0] for s in SOLID)


def stairs(facing, half='bottom', shape='straight'):
    return {'facing': facing, 'half': half, 'shape': shape, 'waterlogged': False}


def roof_ring(t, y, lo, hi, block):
    """One course of a hipped roof: stairs rising toward the middle, outer corners at the corners."""
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            if lo < x < hi and lo < z < hi:
                continue
            if (x, z) == (lo, lo):
                p = stairs('south', shape='outer_left')
            elif (x, z) == (hi, lo):
                p = stairs('south', shape='outer_right')
            elif (x, z) == (lo, hi):
                p = stairs('north', shape='outer_right')
            elif (x, z) == (hi, hi):
                p = stairs('north', shape='outer_left')
            elif z == lo:
                p = stairs('south')
            elif z == hi:
                p = stairs('north')
            elif x == lo:
                p = stairs('east')
            else:
                p = stairs('west')
            t.set(x, y, z, block, p)


def emplacement(t, x, y, z, facing, crossbow=None):
    nbt = {'id': 'fletcherstrestle:emplacement_be', 'Crossbow': crossbow or {}, 'Quiver': {}}
    t.set(x, y, z, 'fletcherstrestle:emplacement', {'facing': facing}, nbt)


def build():
    t = Template(SIZE)
    # The tower stands on x 2..8, z 2..8, centred on (5, 5); its yard is to the south.
    lo, hi, mid = 2, 8, 5
    # Levels: the ground (vanilla sets a template's bottom layer level with the terrain), the bunk
    # room's floor, the top of the trestle, the platform, the roof's eaves.
    ground, floor, frame_top, deck, eaves = 0, 1, 17, 18, 22

    # ---- stone base: footings in the ground, a sloped plinth on it, three courses of wall, a capping course.
    t.fill(lo, 0, lo, hi, 0, hi, 'cobblestone')
    for y in range(floor, 6):
        for x in range(lo, hi + 1):
            for z in range(lo, hi + 1):
                if not (x in (lo, hi) or z in (lo, hi)):
                    continue
                corner = x in (lo, hi) and z in (lo, hi)
                t.set(x, y, z, 'stone_bricks' if corner or y == 5 else 'cobblestone')
    roof_ring(t, floor, lo - 1, hi + 1, 'stone_brick_stairs')
    t.fill(lo + 1, floor, lo + 1, hi - 1, floor, hi - 1, 'spruce_planks')
    t.fill(lo + 1, 5, lo + 1, hi - 1, 5, hi - 1, 'spruce_planks')
    # A door on the south side, and arrow slits dressed as the wall around them.
    door = {'facing': 'north', 'hinge': 'left', 'open': False, 'powered': False}
    t.set(mid, 2, hi, 'spruce_door', dict(door, half='lower'))
    t.set(mid, 3, hi, 'spruce_door', dict(door, half='upper'))
    slit = {'id': 'fletcherstrestle:arrow_slit_be', 'Mimic': {'Name': 'minecraft:cobblestone'}}
    t.set(mid, 3, lo, 'fletcherstrestle:arrow_slit', {'facing': 'north'}, slit)
    t.set(lo, 3, mid, 'fletcherstrestle:arrow_slit', {'facing': 'west'}, slit)
    t.set(hi, 3, mid, 'fletcherstrestle:arrow_slit', {'facing': 'east'}, slit)

    # ---- the middle post, from the bunk room to the platform, with the ladder on its south face.
    for y in range(2, deck):
        t.set(mid, y, mid, 'stripped_spruce_log', {'axis': 'y'})
    for y in range(2, deck):
        t.set(mid, y, mid + 1, 'ladder', {'facing': 'south', 'waterlogged': False})

    # ---- the bunk room.
    t.set(3, 2, 3, 'fletching_table')
    t.set(7, 2, 3, 'red_bed', {'facing': 'north', 'part': 'head', 'occupied': False})
    t.set(7, 2, 4, 'red_bed', {'facing': 'north', 'part': 'foot', 'occupied': False})
    t.set(7, 2, 7, 'barrel', {'facing': 'up', 'open': False},
          {'id': 'minecraft:barrel', 'LootTable': 'minecraft:chests/village/village_taiga_house'})
    t.set(3, 2, 7, 'hay_block', {'axis': 'y'})
    t.set(3, 2, 6, 'hay_block', {'axis': 'z'})
    t.set(3, 4, mid, 'lantern', {'hanging': True, 'waterlogged': False})
    t.set(7, 4, mid, 'lantern', {'hanging': True, 'waterlogged': False})

    # ---- the trestle: four posts, girts in three bays, the top one under the platform.
    for x, z in ((3, 3), (7, 3), (3, 7), (7, 7)):
        for y in range(6, frame_top + 1):
            t.set(x, y, z, 'spruce_log', {'axis': 'y'})
    for y in (9, 13, frame_top):
        for x in range(4, 7):
            t.set(x, y, 3, 'spruce_log', {'axis': 'x'})
            t.set(x, y, 7, 'spruce_log', {'axis': 'x'})
        for z in range(4, 7):
            t.set(3, y, z, 'spruce_log', {'axis': 'z'})
            t.set(7, y, z, 'spruce_log', {'axis': 'z'})
    # Corbels under the platform's overhang.
    for i in range(3, 8):
        t.set(i, frame_top, lo, 'spruce_stairs', stairs('south', 'top'))
        t.set(i, frame_top, hi, 'spruce_stairs', stairs('north', 'top'))
        t.set(lo, frame_top, i, 'spruce_stairs', stairs('east', 'top'))
        t.set(hi, frame_top, i, 'spruce_stairs', stairs('west', 'top'))

    # ---- the platform.
    walk = deck + 1
    t.fill(lo, deck, lo, hi, deck, hi, 'spruce_planks')
    # A trapdoor over the ladder: open, it climbs like the ladder below it; shut, a warden can't fall through.
    t.set(mid, deck, mid + 1, 'spruce_trapdoor', {'facing': 'south', 'half': 'bottom', 'open': False,
                                                  'powered': False, 'waterlogged': False})
    for x, z in ((lo, lo), (hi, lo), (lo, hi), (hi, hi)):
        for y in range(walk, eaves):
            t.set(x, y, z, 'spruce_log', {'axis': 'y'})
    worn = {'id': 'minecraft:crossbow', 'count': 1, 'components': {'minecraft:damage': 330}}
    emplacement(t, mid, walk, lo, 'north', worn)
    emplacement(t, lo, walk, mid, 'west')
    emplacement(t, hi, walk, mid, 'east')
    for i in range(lo + 1, hi):
        for x, z in ((i, lo), (i, hi), (lo, i), (hi, i)):
            if t.get(x, walk, z) is None:
                t.set(x, walk, z, 'spruce_fence')
    # The warden that was: a T of stripped spruce, waiting for its trigger.
    t.set(mid, walk, mid, 'stripped_spruce_log', {'axis': 'y'})
    for x in (mid - 1, mid, mid + 1):
        t.set(x, walk + 1, mid, 'stripped_spruce_log', {'axis': 'x'})
    t.set(7, walk, 7, 'chest', {'facing': 'north', 'type': 'single', 'waterlogged': False},
          {'id': 'minecraft:chest', 'LootTable': 'fletcherstrestle:chests/ranger_watchtower'})
    t.set(3, walk, 7, 'barrel', {'facing': 'up', 'open': False},
          {'id': 'minecraft:barrel', 'LootTable': 'minecraft:chests/village/village_fletcher'})
    t.set(3, walk, 3, 'hay_block', {'axis': 'y'})

    # ---- the roof: a tie beam with a lantern, four courses of dark oak, a nest on the peak.
    for x in range(lo, hi + 1):
        t.set(x, eaves, mid, 'spruce_log', {'axis': 'x'})
    t.set(mid, eaves - 1, mid, 'lantern', {'hanging': True, 'waterlogged': False})
    for k in range(4):
        roof_ring(t, eaves + k, 1 + k, 9 - k, 'dark_oak_stairs')
    t.set(mid, eaves + 3, mid, 'dark_oak_planks')
    t.set(mid, eaves + 4, mid, 'fletcherstrestle:eagle_nest', {'egg_count': 0})

    # ---- the yard, at ground level: a path from the door, a woodpile, a practice butt of hay
    # with a target hung on its face.
    for x in range(lo, hi + 1):
        for z in range(10, 13):
            t.set(x, ground, z, 'grass_block', {'snowy': False})
    for z in range(9, 13):
        t.set(mid, ground, z, 'dirt_path')
    t.set(4, ground, 11, 'coarse_dirt')
    t.set(7, ground, 10, 'coarse_dirt')
    for x, y, z in ((2, 1, 11), (2, 1, 12), (3, 1, 11), (3, 1, 12), (2, 2, 11), (2, 2, 12)):
        t.set(x, y, z, 'spruce_log', {'axis': 'z'})
    for x, y in ((7, 1), (8, 1), (8, 2)):
        t.set(x, y, 12, 'hay_block', {'axis': 'y'})
    # The target is a board on the back of its block: it hangs on the hay behind it, to the south.
    t.set(8, 2, 11, 'fletcherstrestle:archery_target', {'facing': 'north'})

    # Vines creeping up the shady side of the base.
    for x, y, z, side in ((3, 2, lo - 1, 'south'), (3, 3, lo - 1, 'south'), (4, 2, lo - 1, 'south'),
                          (7, 2, lo - 1, 'south'), (7, 3, lo - 1, 'south'), (7, 4, lo - 1, 'south'),
                          (hi + 1, 2, 3, 'west'), (hi + 1, 3, 3, 'west'), (hi + 1, 2, 7, 'west')):
        props = {'north': False, 'south': False, 'east': False, 'west': False, 'up': False}
        props[side] = True
        t.set(x, y, z, 'vine', props)

    # Clear the volume around it so it doesn't come out full of leaves.
    t.air(1, floor, 1, 9, SIZE[1] - 1, 13)

    # Fences connect to each other and to solid blocks beside them.
    for (x, y, z), (name, props, nbt) in list(t.blocks.items()):
        if not name.endswith('_fence'):
            continue
        for direction, (dx, dz) in (('north', (0, -1)), ('south', (0, 1)), ('west', (-1, 0)), ('east', (1, 0))):
            other = t.get(x + dx, y, z + dz)
            props[direction] = other is not None and (other[0].endswith('_fence') or is_solid(other))
        props['waterlogged'] = False
    return t


# ---------------------------------------------------------------- preview

COLOURS = {
    'cobblestone': (122, 122, 122), 'stone_bricks': (150, 150, 150), 'spruce_planks': (114, 84, 48),
    'spruce_log': (70, 52, 30), 'stripped_spruce_log': (150, 114, 66), 'spruce_fence': (104, 76, 44),
    'spruce_stairs': (114, 84, 48), 'dark_oak_stairs': (66, 43, 20), 'dark_oak_planks': (66, 43, 20),
    'ladder': (150, 112, 60), 'spruce_door': (100, 74, 44), 'fletching_table': (180, 160, 110),
    'red_bed': (160, 40, 40), 'barrel': (120, 86, 50), 'hay_block': (190, 160, 40), 'lantern': (240, 200, 110),
    'chest': (160, 110, 40), 'grass_block': (100, 150, 60), 'dirt_path': (150, 120, 70),
    'coarse_dirt': (110, 80, 55), 'emplacement': (90, 70, 50), 'arrow_slit': (122, 122, 122),
    'archery_target': (200, 200, 200), 'eagle_nest': (120, 90, 50), 'stone_brick_stairs': (140, 140, 140),
    'vine': (60, 110, 40), 'spruce_trapdoor': (104, 76, 44),
}


def preview(t, outdir):
    sys.path.insert(0, str(REPO / 'tools/garrison'))
    import make_garrison_art as art
    from PIL import Image
    quads = []
    faces = {
        'top': ((0, 1, 0), [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)]),
        'bottom': ((0, -1, 0), [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)]),
        'north': ((0, 0, -1), [(0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)]),
        'south': ((0, 0, 1), [(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)]),
        'west': ((-1, 0, 0), [(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)]),
        'east': ((1, 0, 0), [(1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)]),
    }
    for (x, y, z), (name, props, _) in t.blocks.items():
        short = name.split(':')[1]
        if short == 'air':
            continue
        col = COLOURS.get(short, (200, 0, 200))
        # Rough shapes: thin posts for fences and lanterns, low boxes for stairs' bottoms and nests.
        x0, x1, y0, y1, z0, z1 = 0, 1, 0, 1, 0, 1
        if short.endswith('_fence'):
            x0, x1, z0, z1 = 0.375, 0.625, 0.375, 0.625
        elif short in ('lantern', 'emplacement'):
            x0, x1, z0, z1 = 0.25, 0.75, 0.25, 0.75
        elif short == 'eagle_nest':
            y1 = 0.3
        elif short == 'vine':
            z0, z1 = (0.9, 1.0) if props.get('south') else (0.0, 0.1)
            if props.get('west'):
                x0, x1, z0, z1 = 0.0, 0.1, 0, 1
        elif short == 'ladder':
            z0 = 0.0 if props.get('facing') == 'south' else 0.9
            z1 = z0 + 0.1
        for fname, (normal, corners) in faces.items():
            n = (x + normal[0], y + normal[1], z + normal[2])
            if t.get(*n) is not None and is_solid(t.get(*n)) and short not in ('lantern', 'ladder', 'emplacement', 'vine') \
                    and not short.endswith('_fence'):
                continue
            pts = []
            for cx, cy, cz in corners:
                px = x + (x0 if cx == 0 else x1)
                py = y + (y0 if cy == 0 else y1)
                pz = z + (z0 if cz == 0 else z1)
                # To the renderer's frame: y down, centred.
                pts.append((px - 5.5, -(py - 13), pz - 7))
            shade = 1.0 if fname == 'top' else 0.8 if fname in ('north', 'south') else 0.65
            quads.append((pts, (normal[0], -normal[1], normal[2]), tuple(int(c * shade) for c in col) + (255,)))
    outdir.mkdir(parents=True, exist_ok=True)
    views = [(-35, 28), (145, 28), (55, 12), (-125, 40)]
    sheet = Image.new('RGBA', (560 * 2, 860 * 2))
    for i, (yaw, pitch) in enumerate(views):
        img = art.render_quads(quads, math.radians(yaw), math.radians(pitch), (560, 860), 24, (280, 430))
        sheet.paste(img, ((i % 2) * 560, (i // 2) * 860))
    sheet.save(outdir / 'watchtower.png')
    # A cutaway: everything above the bunk room's ceiling hidden.
    cut = [(pts, n, c) for pts, n, c in quads if min(13 - p[1] for p in pts) < 5.0]
    cut = [([(p[0], p[1] - 10, p[2]) for p in pts], n, c) for pts, n, c in cut]
    art.render_quads(cut, math.radians(-35), math.radians(50), (620, 560), 34, (310, 280)).save(
        outdir / 'watchtower_bunk_room.png')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', type=pathlib.Path)
    args = ap.parse_args()
    t = build()
    write_nbt(OUT, t.to_nbt())
    solid = sum(1 for v in t.blocks.values() if v[0] != 'minecraft:air')
    print('%s: %d blocks placed, %d cleared' % (OUT.relative_to(REPO), solid, len(t.blocks) - solid))
    if args.preview:
        preview(t, args.preview)


if __name__ == '__main__':
    main()
