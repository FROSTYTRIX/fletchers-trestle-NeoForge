#!/usr/bin/env python3
"""
Generates the Fletcher's Trestle showcase datapack: staged scenes for screenshots.

Built for a superflat world (the default "Classic Flat": you stand at y = -60).
Everything is placed at fixed coordinates around the world spawn, because nails
store their garland partners as absolute positions.

    python3 tools/showcase/build_showcase.py

Then copy tools/showcase/fletchers_showcase into <world>/datapacks/ and, in game:

    /reload
    /function showcase:build        builds every scene (takes a couple of seconds)
    /function showcase:cam/workshop  jump to a camera spot (see cam/ for the list)
    /function showcase:kit          the weapons, quiver and arrows, for in-hand shots
    /function showcase:clear        wipes the area and the staged mobs
"""

import pathlib
import shutil

OUT = pathlib.Path(__file__).resolve().parent / "fletchers_showcase"
NS = "fletcherstrestle"
BASE_Y = -60          # first air layer on a Classic Flat world
TAG = "ft_show"       # every staged entity carries it, so clear can find them

# Dye colours, as garlands store them.
COLOUR = {
    "white": 0xF9FFFE, "orange": 0xF9801D, "magenta": 0xC74EBD, "light_blue": 0x3AB3DA,
    "yellow": 0xFED83D, "lime": 0x80C71F, "pink": 0xF38BAA, "red": 0xB02E26,
    "blue": 0x3C44AA, "purple": 0x8932B8, "green": 0x5E7C16, "cyan": 0x169C9C,
}
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
         "pale_oak", "crimson", "warped"]
NAMES = {"dark_oak": "Dark Oak", "pale_oak": "Pale Oak"}


def nice(wood):
    return NAMES.get(wood, wood.capitalize())


# ---------------------------------------------------------------- items

def bow(limb, riser="iron", string="spider", tuning=0.95, second=None, enchants=None, crossbow=False,
        attachment=None, charged=False):
    """SNBT for a finished modular bow or crossbow."""
    asm = f'limb:"{limb}",riser:"{riser}",string:"{string}",tuning:{tuning}f'
    if second:
        asm += f',second_limb:"{second}"'
    comps = [f'"{NS}:bow_assembly":{{{asm}}}', f'"{NS}:crafted_by":"FROSTYTRIX"']
    if enchants:
        levels = ",".join(f'"{k}":{v}' for k, v in enchants.items())
        comps.append(f'"minecraft:enchantments":{{levels:{{{levels}}}}}')
    if attachment:
        comps.append(f'"{NS}:crossbow_attachment":"{NS}:{attachment}"')
    if charged:
        comps.append('"minecraft:charged_projectiles":[{id:"minecraft:arrow",count:1}]')
    item = "modular_crossbow" if crossbow else "modular_bow"
    return f'{{id:"{NS}:{item}",count:1,components:{{{",".join(comps)}}}}}'


def arrow(head, shaft, fletching, count=1):
    return (f'{{id:"{NS}:modular_arrow",count:{count},components:{{"{NS}:arrow_assembly":'
            f'{{head:"{head}",shaft:"{shaft}",fletching:"{fletching}"}}}}}}')


def garland(*colours):
    ints = ",".join(str(COLOUR[c]) for c in colours)
    return f'{{id:"{NS}:garland",count:1,components:{{"{NS}:garland_colours":{{colours:[{ints}]}}}}}}'


def give_cmd(snbt):
    """Turns item SNBT into a /give argument: id[component=value,...] count."""
    # Only used for the kit, where the SNBT shapes are known.
    inner = snbt[1:-1]
    item_id = inner.split('id:"', 1)[1].split('"', 1)[0]
    count = inner.split("count:", 1)[1].split(",", 1)[0]
    comps = inner.split("components:{", 1)[1][:-1]
    # components are "k":v pairs separated at depth 0
    parts, depth, cur, in_str = [], 0, "", False
    for ch in comps:
        if ch == '"':
            in_str = not in_str
        if not in_str:
            if ch in "{[":
                depth += 1
            elif ch in "}]":
                depth -= 1
            elif ch == "," and depth == 0:
                parts.append(cur)
                cur = ""
                continue
        cur += ch
    parts.append(cur)
    args = ",".join(p.replace('":', '=', 1).lstrip('"') for p in parts)
    return f"{item_id}[{args}] {count}"


# ---------------------------------------------------------------- builder

class Fn:
    def __init__(self):
        self.lines = []

    def c(self, line):
        self.lines.append(line)

    def comment(self, text):
        self.lines.append(f"# {text}")

    @staticmethod
    def p(x, y, z):
        return f"{x} {y + BASE_Y} {z}"

    def fill(self, x1, y1, z1, x2, y2, z2, block, mode=""):
        self.c(f"fill {self.p(x1, y1, z1)} {self.p(x2, y2, z2)} {block}{(' ' + mode) if mode else ''}")

    def set(self, x, y, z, block):
        self.c(f"setblock {self.p(x, y, z)} {block}")

    def summon(self, entity, x, y, z, nbt):
        tags = f'Tags:["{TAG}"]'
        nbt = f"{{{tags},{nbt}}}" if nbt else f"{{{tags}}}"
        self.c(f"summon {entity} {x} {y + BASE_Y} {z} {nbt}")


def pos_nbt(x, y, z):
    return f"[I;{x},{y + BASE_Y},{z}]"


class Garlands:
    """Collects every span first, then places each nail once with all of its spans:
    placing a nail twice would wipe the garlands already tied to it."""

    def __init__(self):
        self.state = {}
        self.spans = {}
        self.incoming = {}

    def nail(self, x, y, z, state):
        self.state[(x, y, z)] = state
        self.spans.setdefault((x, y, z), [])
        self.incoming.setdefault((x, y, z), [])

    def span(self, a, b, colours):
        self.spans[a].append(f'{{Target:{pos_nbt(*b)},Garland:{garland(*colours)}}}')
        self.incoming[b].append(f'{{Pos:{pos_nbt(*a)}}}')

    def place(self, fn):
        for pos, state in self.state.items():
            assert len(self.spans[pos]) + len(self.incoming[pos]) <= 4, f"nail {pos} holds too many"
            fn.set(*pos, f'{NS}:nail[{state}]{{Spans:[{",".join(self.spans[pos])}],'
                         f'Incoming:[{",".join(self.incoming[pos])}]}}')


def rack(fn, x, y, z, facing, item):
    fn.set(x, y, z, f'{NS}:weapon_rack[facing={facing}]{{DisplayedItem:{item}}}')


def sign(fn, x, y, z, facing, text, wood="spruce"):
    fn.set(x, y, z, f"{wood}_wall_sign[facing={facing}]{{front_text:{{color:\"black\",messages:['\"{text}\"','\"\"','\"\"','\"\"']}}}}")


def tree_canopy(fn, x1, z1, x2, z2, y, leaves):
    fn.fill(x1, y, z1, x2, y + 1, z2, f"{leaves}[persistent=true]")
    fn.fill(x1 + 1, y + 2, z1 + 1, x2 - 1, y + 2, z2 - 1, f"{leaves}[persistent=true]")


# ---------------------------------------------------------------- scenes

def workshop(fn):
    fn.comment("The Workshop: the whole pipeline under one roof, open to the south for the camera.")
    fn.fill(-1, -1, -1, 17, -1, 20, "grass_block")
    fn.fill(0, -1, 0, 16, -1, 10, "spruce_planks")
    fn.fill(0, -1, 10, 16, -1, 10, "stripped_spruce_log[axis=x]")
    # back and side walls, log-framed
    fn.fill(0, 0, 0, 16, 5, 0, "spruce_planks")
    fn.fill(0, 0, 0, 0, 5, 8, "spruce_planks")
    fn.fill(16, 0, 0, 16, 5, 8, "spruce_planks")
    for x in (0, 4, 8, 12, 16):
        fn.fill(x, 0, 0, x, 5, 0, "stripped_spruce_log[axis=y]")
    for z in (4, 8):
        fn.fill(0, 0, z, 0, 5, z, "stripped_spruce_log[axis=y]")
        fn.fill(16, 0, z, 16, 5, z, "stripped_spruce_log[axis=y]")
    fn.fill(1, 2, 0, 3, 3, 0, "glass_pane")
    fn.fill(13, 2, 0, 15, 3, 0, "glass_pane")
    # front pillars, beam and roof
    fn.fill(0, 0, 10, 0, 5, 10, "stripped_spruce_log[axis=y]")
    fn.fill(16, 0, 10, 16, 5, 10, "stripped_spruce_log[axis=y]")
    fn.fill(0, 5, 10, 16, 5, 10, "spruce_log[axis=x]")
    fn.fill(-1, 6, -1, 17, 6, 11, "dark_oak_slab[type=bottom]")
    for x in (4, 12):
        fn.set(x, 4, 10, "lantern[hanging=true]")
    fn.set(8, 4, 5, "lantern[hanging=true]")
    fn.fill(8, 5, 5, 8, 5, 5, "spruce_planks")
    # a garland along the front beam
    g = Garlands()
    for x in (1, 8, 15):
        g.nail(x, 5, 11, "face=wall,facing=south")
    g.span((1, 5, 11), (8, 5, 11), ("red", "yellow", "red", "yellow", "red", "yellow", "red"))
    g.span((8, 5, 11), (15, 5, 11), ("blue", "white", "blue", "white", "blue", "white", "blue"))
    g.place(fn)

    fn.comment("Stations")
    fn.set(3, 0, 3, "campfire[lit=true,signal_fire=false,facing=south]")
    fn.set(3, 1, 3, f'{NS}:steam_box{{fluid:{{Fluid:{{id:"minecraft:water",amount:4000}}}}}}')
    fn.set(6, 0, 3, f'{NS}:shaving_horse[facing=south]{{inventory:{{Items:[{{Slot:0,id:"minecraft:birch_log",count:1}}],Size:1}},currentShaves:2}}')
    fn.set(9, 0, 3, "fletching_table")
    fn.set(12, 0, 3, f'{NS}:dipping_vat{{Fluid:{{Fluid:{{id:"minecraft:water",amount:3000}}}}}}')
    fn.set(14, 0, 6, f"{NS}:crossbow_bench")
    fn.set(1, 0, 1, "barrel[facing=up]")
    fn.set(2, 0, 1, "chest[facing=south]")
    fn.set(1, 1, 1, "barrel[facing=up]")
    fn.set(15, 2, 6, f"{NS}:archery_target[facing=west]")
    # rope hanging from the roof slab, coiled at the bottom
    fn.fill(15, 1, 1, 15, 5, 1, f"{NS}:rope[bottom=false,persistent=true]")
    fn.set(15, 1, 1, f"{NS}:rope[bottom=true,persistent=true]")
    fn.fill(5, 0, 6, 11, 0, 8, f"{NS}:red_linen_carpet")
    fn.fill(6, 0, 7, 10, 0, 7, f"{NS}:white_linen_carpet")

    fn.comment("Weapons on the back wall")
    rack(fn, 2, 3, 1, "south", bow("oak", "wood", "spider", 0.82))
    rack(fn, 5, 3, 1, "south", bow("cherry", "copper", "flax", 0.9))
    rack(fn, 8, 3, 1, "south", bow("dark_oak", "iron", "high_tension", 1.0, second="birch",
                                    enchants={f"{NS}:tinkers_mark": 3}))
    rack(fn, 11, 3, 1, "south", bow("crimson", "iron", "high_tension", 0.97, crossbow=True,
                                     attachment="scope", charged=True))
    rack(fn, 14, 3, 1, "south", bow("jungle", "copper", "spider", 0.88))

    fn.comment("Flax growing out front, and a dummy to test on")
    fn.fill(1, -1, 13, 7, -1, 17, "farmland[moisture=7]")
    fn.set(4, -1, 15, "water")
    for x in range(1, 8):
        for z in range(13, 18):
            if (x, z) == (4, 15):
                continue
            fn.set(x, 0, z, f"{NS}:flax_crop[age=7]")
            fn.set(x, 1, z, f"{NS}:flax_crop[age=8]")
    fn.summon(f"{NS}:heavy_dummy", 12.5, 0, 16.5, "Rotation:[180f,0f]")


def armory(fn):
    fn.comment("The Armory: every wood on one wall, composites and crossbows above.")
    fn.fill(20, -1, -1, 50, -1, 10, "polished_andesite")
    fn.fill(20, -1, 1, 50, -1, 3, "polished_deepslate")
    fn.fill(21, 0, 4, 49, 0, 5, f"{NS}:green_linen_carpet")
    fn.fill(20, 0, 0, 50, 7, 0, "stone_bricks")
    fn.fill(20, 0, 0, 50, 0, 0, "polished_deepslate")
    fn.fill(20, 7, 0, 50, 7, 0, "polished_deepslate")
    for x in range(20, 51, 6):
        fn.fill(x, 0, 0, x, 7, 0, "stripped_dark_oak_log[axis=y]")
    fn.fill(20, 8, 0, 50, 8, 1, "dark_oak_slab[type=bottom]")

    risers = ["wood", "copper", "iron"]
    strings = {"wood": ["spider", "flax"], "copper": ["spider", "high_tension"], "iron": ["high_tension", "flax"]}
    for i, wood in enumerate(WOODS):
        x = 22 + i * 2
        riser = risers[i % 3]
        string = strings[riser][i % 2]
        rack(fn, x, 2, 1, "south", bow(wood, riser, string, round(0.8 + (i % 4) * 0.05, 2)))
        sign(fn, x, 1, 1, "south", nice(wood), "dark_oak")
    upper = [
        bow("crimson", "iron", "high_tension", 1.0, second="warped", enchants={f"{NS}:follow_through": 3}),
        bow("dark_oak", "iron", "flax", 0.94, second="birch"),
        bow("spruce", "wood", "spider", 0.9, crossbow=True, attachment="magazine", charged=True),
        bow("jungle", "copper", "spider", 0.9, second="mangrove"),
        bow("oak", "iron", "high_tension", 0.99, crossbow=True, attachment="scope"),
        bow("cherry", "wood", "flax", 0.86, second="pale_oak", enchants={f"{NS}:photosynthesis": 2}),
        bow("mangrove", "copper", "high_tension", 0.92, crossbow=True, attachment="bayonet"),
    ]
    xs = [23, 27, 29, 33, 35, 39, 41]
    for x, item in zip(xs, upper):
        rack(fn, x, 5, 1, "south", item)
    for x in range(21, 50, 4):
        fn.set(x, 7, 1, "lantern[hanging=true]")


def archery_range(fn):
    fn.comment("The Range: a shooting bunker behind arrow slits, targets down the lane, an eagle on watch.")
    fn.fill(-1, -1, 24, 16, -1, 64, "grass_block")
    fn.fill(5, -1, 26, 9, -1, 62, "dirt_path")
    fn.fill(-1, 0, 24, -1, 0, 64, "oak_fence")
    fn.fill(16, 0, 24, 16, 0, 64, "oak_fence")
    # bunker
    fn.fill(0, -1, 56, 15, -1, 63, "spruce_planks")
    fn.fill(0, 0, 56, 15, 3, 56, "stone_bricks")
    fn.fill(0, 4, 56, 15, 4, 63, "stone_brick_slab[type=bottom]")
    fn.fill(0, 0, 57, 0, 3, 63, "stone_bricks")
    fn.fill(15, 0, 57, 15, 3, 63, "stone_bricks")
    for x in (3, 7, 11):
        fn.set(x, 1, 56, f'{NS}:arrow_slit[facing=north]{{Mimic:{{Name:"minecraft:stone_bricks"}}}}')
    fn.set(7, 3, 60, "lantern[hanging=true]")
    fn.set(2, 0, 62, "barrel[facing=up]")
    fn.set(13, 0, 62, f"{NS}:crossbow_bench")
    # targets on hay, facing the bunker
    for x, z in ((3, 46), (8, 38), (12, 30)):
        fn.fill(x, 0, z - 1, x, 2, z - 1, "hay_block")
        fn.set(x, 2, z, f"{NS}:archery_target[facing=south]")
    fn.summon(f"{NS}:heavy_dummy", 5.5, 0, 42.5, "Rotation:[0f,0f]")
    fn.summon(f"{NS}:heavy_dummy", 10.5, 0, 34.5, "Rotation:[0f,0f]")
    # the eagle's corner: a perch by the bunker, a nest on a dead tree up the lane
    fn.set(2, 0, 53, f"{NS}:eagle_perch[facing=south]")
    fn.summon(f"{NS}:eagle", 2.5, 14 / 16, 53.5,
              f"EagleState:1,PerchX:2,PerchY:{BASE_Y},PerchZ:53,NoAI:1b,PersistenceRequired:1b,Rotation:[200f,0f]")
    fn.fill(13, 0, 27, 13, 6, 27, "stripped_spruce_log[axis=y]")
    fn.fill(12, 5, 27, 14, 5, 27, "spruce_fence")
    fn.set(13, 7, 27, f"{NS}:eagle_nest")


def armed_mobs(fn):
    fn.comment("Armed mobs: under a canopy, so the skeletons don't burn. Each carries its own wood.")
    fn.fill(22, -1, 18, 46, -1, 36, "grass_block")
    fn.fill(25, -1, 22, 43, -1, 30, "podzol")
    fn.fill(28, -1, 25, 40, -1, 27, "coarse_dirt")
    for x, z, log in ((24, 21, "oak_log"), (44, 21, "birch_log"), (24, 31, "birch_log"), (44, 31, "oak_log"),
                      (34, 20, "dark_oak_log")):
        fn.fill(x, 0, z, x, 5, z, f"{log}[axis=y]")
    tree_canopy(fn, 22, 19, 46, 33, 5, "oak_leaves")
    fn.fill(26, 5, 22, 42, 5, 30, "birch_leaves[persistent=true]")
    fn.set(28, 0, 23, "mossy_cobblestone")
    fn.set(40, 0, 23, "mossy_cobblestone_wall")
    mobs = [
        ("skeleton", 29.5, bow("crimson", "copper", "spider", 0.6, second="oak")),
        ("stray", 32.5, bow("spruce", "wood", "flax", 0.55)),
        ("bogged", 35.5, bow("oak", "iron", "high_tension", 0.7, second="mangrove")),
        ("pillager", 38.5, bow("dark_oak", "wood", "spider", 0.65, crossbow=True, charged=True)),
    ]
    for entity, x, item in mobs:
        fn.summon(f"minecraft:{entity}", x, 0, 26.5,
                  f"NoAI:1b,PersistenceRequired:1b,Silent:1b,Rotation:[0f,0f],"
                  f"HandItems:[{item},{{}}],HandDropChances:[0f,0f],"
                  f"NeoForgeData:{{\"{NS}:armory_rolled\":1b}}")


def market(fn):
    fn.comment("The Linen Market: dyed linen stalls, bunting across the street.")
    fn.fill(50, -1, -1, 78, -1, 22, "grass_block")
    fn.fill(50, -1, 9, 78, -1, 12, "dirt_path")
    stalls = [(52, "red", "white"), (61, "blue", "yellow"), (70, "green", "orange")]
    for x0, a, b in stalls:
        fn.fill(x0, -1, 2, x0 + 6, -1, 7, "spruce_planks")
        for x, z in ((x0, 2), (x0 + 6, 2), (x0, 7), (x0 + 6, 7)):
            fn.fill(x, 0, z, x, 3, z, "spruce_fence")
        # a striped linen awning: sloped eaves front and back, a ridge on top
        for x in range(x0 - 1, x0 + 8):
            colour = a if (x - x0) % 2 == 0 else b
            fn.set(x, 4, 1, f"{NS}:{colour}_linen_stairs[facing=south,half=bottom]")
            fn.fill(x, 4, 2, x, 4, 7, f"{NS}:{colour}_linen")
            fn.set(x, 4, 8, f"{NS}:{colour}_linen_stairs[facing=north,half=bottom]")
            fn.fill(x, 5, 4, x, 5, 5, f"{NS}:{colour}_linen_slab[type=bottom]")
        fn.fill(x0 + 1, 0, 3, x0 + 5, 0, 3, "barrel[facing=up]")
        fn.fill(x0 + 1, 1, 3, x0 + 5, 1, 3, f"{NS}:{a}_linen_carpet")
    # the bowyer's stall: weapons for sale on its back wall
    fn.fill(61, 1, 2, 67, 3, 2, "spruce_planks")
    rack(fn, 62, 2, 3, "south", bow("birch", "wood", "flax", 0.7))
    rack(fn, 64, 2, 3, "south", bow("acacia", "copper", "spider", 0.75))
    rack(fn, 66, 2, 3, "south", bow("spruce", "wood", "spider", 0.72, crossbow=True))
    # a dye-merchant's stall: linen in every colour
    for i, colour in enumerate(["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink"]):
        fn.set(71 + (i % 5), 1 + i // 5, 3, f"{NS}:{colour}_linen")
    # bunting on log posts either side of the street: along both edges, and across it
    g = Garlands()
    for x in (51, 63, 75):
        for z in (9, 13):
            fn.fill(x, 0, z, x, 4, z, "stripped_spruce_log[axis=y]")
            g.nail(x, 5, z, "face=floor,facing=north")
    rainbow = ("red", "orange", "yellow", "lime", "light_blue", "blue", "purple")
    g.span((51, 5, 9), (63, 5, 9), rainbow)
    g.span((63, 5, 9), (75, 5, 9), ("pink", "white", "pink", "white", "pink", "white", "pink"))
    g.span((51, 5, 13), (63, 5, 13), ("yellow", "cyan", "yellow", "cyan", "yellow", "cyan", "yellow"))
    g.span((63, 5, 13), (75, 5, 13), rainbow)
    for x in (51, 63, 75):
        g.span((x, 5, 9), (x, 5, 13), ("red", "white", "blue", "red", "white", "blue", "red"))
    g.place(fn)


WARDEN_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
                "crimson", "warped", "bamboo"]


def quiver(*arrows):
    slots = ",".join(f"{{slot:{i},item:{a}}}" for i, a in enumerate(arrows))
    return f'{{id:"{NS}:iron_quiver",count:1,components:{{"minecraft:container":[{slots}]}}}}'


def emplacement(fn, x, y, z, facing, crossbow=None, ammo=None):
    contents = f"Crossbow:{crossbow or '{}'},Quiver:{ammo or '{}'}"
    fn.set(x, y, z, f"{NS}:emplacement[facing={facing}]{{{contents}}}")


def warden(fn, x, y, z, wood, yaw, still=True):
    ai = "NoAI:1b," if still else ""
    fn.summon(f"{NS}:garrison_golem", x, y, z, f'{ai}PersistenceRequired:1b,Wood:"{wood}",Rotation:[{yaw}f,0f]')


def garrison(fn):
    fn.comment("The Garrison: a curtain wall with three manned emplacements, a field in front for the enemy.")
    fn.fill(-1, -1, 68, 31, -1, 100, "grass_block")
    fn.fill(2, -1, 85, 28, -1, 99, "coarse_dirt")
    fn.fill(4, -1, 87, 26, -1, 97, "gravel")
    # the wall: three blocks thick, four high, crenellated along the front
    fn.fill(2, 0, 82, 28, 3, 84, "stone_bricks")
    fn.fill(2, 0, 82, 28, 0, 82, "mossy_stone_bricks")
    for x in range(2, 29):
        if x % 2 == 0 and x not in (6, 15, 24):
            fn.set(x, 4, 82, "stone_brick_wall")
    for x in (2, 28):
        fn.fill(x, 0, 81, x, 6, 85, "stone_bricks")
        fn.fill(x, 7, 81, x, 7, 85, "stone_brick_slab[type=bottom]")
    # stairs up the back, each step on stone
    for i in range(4):
        z = 88 - i
        if i:
            fn.fill(13, 0, z, 17, i - 1, z, "stone_bricks")
        fn.fill(13, i, z, 17, i, z, "stone_brick_stairs[facing=north,half=bottom]")
    # three posts, each with a different attachment, quivers full
    arrows = (arrow("broadhead", "spruce", "red_feather", 64), arrow("bodkin_point", "dark_oak", "feather", 64))
    posts = [
        (6, bow("spruce", "iron", "high_tension", 0.95, crossbow=True, attachment="scope"), "oak"),
        (15, bow("dark_oak", "iron", "spider", 0.97, crossbow=True, attachment="magazine", charged=True), "dark_oak"),
        (24, bow("cherry", "copper", "flax", 0.9, crossbow=True), "cherry"),
    ]
    for x, crossbow, wood in posts:
        emplacement(fn, x, 4, 82, "north", crossbow, quiver(*arrows))
        warden(fn, x + 0.5, 4, 83.6, wood, 180)
    # off duty, below the wall
    warden(fn, 7.5, 0, 92.5, "birch", 200)
    warden(fn, 22.5, 0, 91.5, "warped", 150)
    fn.set(5, 0, 94, "barrel[facing=up]")
    fn.set(6, 0, 95, "barrel[facing=up]")
    fn.set(24, 0, 95, f"{NS}:crossbow_bench")
    for x, z in ((10, 90), (20, 90)):
        fn.fill(x, 0, z, x, 2, z, "spruce_fence")
        fn.set(x, 3, z, "lantern")
    sign(fn, 15, 2, 85, "south", "The Garrison", "spruce")


def lineup(fn):
    fn.comment("Eleven Bolt Wardens, one per wood, on parade.")
    fn.fill(33, -1, 70, 57, -1, 92, "grass_block")
    fn.fill(34, -1, 77, 56, -1, 83, "polished_andesite")
    fn.fill(34, -1, 79, 56, -1, 79, "polished_deepslate")
    for i, wood in enumerate(WARDEN_WOODS):
        x = 35 + i * 2
        warden(fn, x + 0.5, 0, 79.5, wood, 0)
        fn.set(x, 0, 81, f"spruce_sign[rotation=0]{{front_text:{{color:\"black\",messages:['\"{nice(wood)}\"','\"\"','\"\"','\"\"']}}}}")
    for x in (34, 56):
        fn.fill(x, 0, 77, x, 3, 77, "stripped_spruce_log[axis=y]")
        fn.set(x, 4, 77, "lantern")


def watchtower(fn):
    fn.comment("A Ranger's Watchtower from its template, among spruces, an eagle on its nest.")
    fn.fill(58, -1, 68, 80, -1, 104, "grass_block")
    fn.c(f"place template {NS}:ranger_watchtower 63 {BASE_Y - 1} 80")
    for x, z in ((59, 72), (77, 74), (60, 96), (58, 86), (80, 84)):
        fn.c(f"place feature minecraft:spruce {x} {BASE_Y} {z}")
    # the nest sits on the peak: template (5, 26, 5)
    fn.summon(f"{NS}:eagle", 68.5, 26 - 1 + 0.25, 85.5, "NoAI:1b,PersistenceRequired:1b,Rotation:[160f,0f]")


# ---------------------------------------------------------------- functions

AREA = (-3, -2, -3, 82, 31, 106)   # x1, y1, z1, x2, y2, z2 (local)

CAMERAS = {
    "workshop": (8.5, 1.5, 24.5, 180, 4),
    "workshop_inside": (12.5, 1.0, 9.0, 140, 10),
    "steam_box": (5.5, 1.2, 6.5, 145, 12),
    "armory": (32.5, 1.6, 11.5, 180, 4),
    "range": (7.5, 9.0, 70.0, 180, 28),
    "range_aerial": (-8.5, 14.0, 44.5, -90, 32),
    "eagle": (3.5, 1.0, 50.5, 18, 20),
    "armed_mobs": (34.0, 1.4, 33.5, 180, 3),
    "market": (63.5, 2.0, 21.5, 180, 6),
    "garrison": (23.0, 2.6, 73.5, 36, -4),
    "garrison_crew": (17.6, 6.1, 87.2, 156, 16),
    "garrison_front": (16.5, 4.4, 78.6, 10, 8),
    "emplacement": (12.8, 4.6, 79.8, -45, 20),
    "lineup": (45.5, 0.6, 89.0, 180, -2),
    "watchtower": (82.0, 2.0, 104.5, 141, -18),
    "watchtower_top": (74.5, 19.6, 92.5, 139, 12),
}


def clear_fn():
    fn = Fn()
    fn.comment("Wipes the showcase area back to flat grass and removes the staged mobs.")
    fn.c(f"kill @e[tag={TAG}]")
    x1, y1, z1, x2, y2, z2 = AREA
    for x in range(x1, x2 + 1, 16):
        hx = min(x + 15, x2)
        for z in range(z1, z2 + 1, 32):
            hz = min(z + 31, z2)
            fn.fill(x, 0, z, hx, y2, hz, "air")
            fn.fill(x, -1, z, hx, -1, hz, "grass_block")
    fn.c("kill @e[type=item,distance=..200]")
    return fn


def write(path, fn_or_lines):
    lines = fn_or_lines.lines if isinstance(fn_or_lines, Fn) else fn_or_lines
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n")


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    (OUT).mkdir(parents=True)
    (OUT / "pack.mcmeta").write_text(
        '{\n  "pack": {\n    "pack_format": 48,\n'
        '    "description": "Fletcher\'s Trestle showcase scenes. /function showcase:build"\n  }\n}\n')
    fns = OUT / "data" / "showcase" / "function"

    scenes = {"workshop": workshop, "armory": armory, "range": archery_range,
              "armed_mobs": armed_mobs, "market": market, "garrison": garrison, "lineup": lineup,
              "watchtower": watchtower}
    for name, build in scenes.items():
        fn = Fn()
        build(fn)
        write(fns / "scene" / f"{name}.mcfunction", fn)

    cx, cz = 40, 52
    write(fns / "build.mcfunction", [
        "# Loads the area, then builds every scene a moment later (chunks need a tick to load).",
        f"forceload add {AREA[0]} {AREA[2]} {AREA[3]} {AREA[5]}",
        f"tp @s {cx} {BASE_Y + 30} {cz}",
        "schedule function showcase:internal/build 40t",
        'tellraw @s {"text":"Building the Fletcher\'s Trestle showcase...","color":"gold"}',
    ])
    write(fns / "internal" / "build.mcfunction", [
        "function showcase:clear",
        "function showcase:setup",
        *[f"function showcase:scene/{name}" for name in scenes],
        'tellraw @a {"text":"Showcase built. Camera spots: /function showcase:cam/<name>","color":"green"}',
        'tellraw @a ["",' + ",".join(
            f'{{"text":"[{name}] ","color":"aqua","clickEvent":{{"action":"run_command","value":"/function showcase:cam/{name}"}}}}'
            for name in CAMERAS) + "]",
    ])
    write(fns / "clear.mcfunction", clear_fn())
    write(fns / "setup.mcfunction", [
        "# Still, clear, golden light: no day cycle, no weather, no mob spawns.",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule doMobSpawning false",
        "gamerule doFireTick false",
        "weather clear",
        "time set 12300",
    ])
    write(fns / "garrison_attack.mcfunction", [
        "# Wakes the garrison's crews and sends zombies across the field in front of the wall.",
        f"execute as @e[type={NS}:garrison_golem,tag={TAG},x=0,y={BASE_Y},z=80,dx=30,dy=10,dz=10] run data merge entity @s {{NoAI:0b}}",
        "gamerule doMobSpawning false",
        # Helmets, so the low sun doesn't set them alight before the crews can.
        *[f"summon minecraft:zombie {x} {BASE_Y} {z} {{Tags:[\"{TAG}\"],PersistenceRequired:1b,"
          f"ArmorItems:[{{}},{{}},{{}},{{id:\"minecraft:leather_helmet\",count:1}}],ArmorDropChances:[0f,0f,0f,0f]}}"
          for x, z in ((6.5, 71.5), (11.5, 73.5), (15.5, 70.5), (20.5, 72.5), (25.5, 71.5))],
    ])
    write(fns / "time" / "noon.mcfunction", ["time set 6000"])
    write(fns / "time" / "golden.mcfunction", ["time set 12300"])
    write(fns / "time" / "dawn.mcfunction", ["time set 23300"])
    write(fns / "time" / "night.mcfunction", ["time set 18000"])
    write(fns / "time" / "storm.mcfunction", ["weather thunder"])

    for name, (x, y, z, yaw, pitch) in CAMERAS.items():
        write(fns / "cam" / f"{name}.mcfunction",
              [f"gamemode spectator @s", f"tp @s {x} {y + BASE_Y} {z} {yaw} {pitch}"])

    kit = [
        bow("crimson", "iron", "high_tension", 1.0, second="warped", enchants={f"{NS}:tinkers_mark": 5}),
        bow("cherry", "copper", "flax", 0.93),
        bow("dark_oak", "iron", "high_tension", 0.97),
        bow("oak", "iron", "high_tension", 1.0, crossbow=True, attachment="scope", charged=True),
        bow("spruce", "copper", "spider", 0.9, crossbow=True, attachment="magazine"),
        arrow("broadhead", "oak", "red_feather", 32),
        arrow("bodkin_point", "dark_oak", "blue_feather", 32),
        arrow("glass_vial", "cherry", "light_gray_feather", 16),
        arrow("resonance_tip", "warped", "vex", 16),
        garland("red", "yellow", "red", "yellow", "red", "yellow", "red"),
    ]
    quiver = (f'{NS}:iron_quiver[minecraft:container=[{{slot:0,item:{arrow("broadhead", "oak", "red_feather", 64)}}},'
              f'{{slot:1,item:{arrow("barbed_tip", "spruce", "green_feather", 64)}}},'
              f'{{slot:2,item:{arrow("weighted_blunt", "birch", "feather", 64)}}}],'
              f'{NS}:quiver_selected_slot=0] 1')
    write(fns / "kit.mcfunction", ["# Weapons, a filled quiver and arrows, for in-hand screenshots.",
                                   *[f"give @s {give_cmd(i)}" for i in kit],
                                   f"give @s {quiver}",
                                   f"give @s {NS}:fletcher_guide 1",
                                   f"give @s {NS}:emplacement 3",
                                   f"give @s {NS}:mechanical_trigger 2",
                                   f"give @s {NS}:garrison_golem_spawn_egg 4"])

    total = sum(len(p.read_text().splitlines()) for p in OUT.rglob("*.mcfunction"))
    print(f"Wrote {OUT} ({total} lines across {len(list(OUT.rglob('*.mcfunction')))} functions)")


if __name__ == "__main__":
    main()
