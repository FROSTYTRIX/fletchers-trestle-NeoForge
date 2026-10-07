#!/usr/bin/env python3
"""Static checks for the showcase datapack: ids, block states, item ids, SNBT balance.

Can't prove a command runs, but catches what usually breaks one: a misspelt block,
a property the block doesn't have, an unknown item, unbalanced brackets.
"""
import glob, json, pathlib, re, sys, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
PACK = ROOT / "tools/showcase/fletchers_showcase"
jar = next(iter(glob.glob(str(ROOT / "build/moddev/artifacts/*client-extra*.jar"))))

states, items = {}, set()
def learn(name, data):
    props = {}
    for key in data.get("variants", {}):
        for kv in filter(None, key.split(",")):
            k, v = kv.split("=")
            props.setdefault(k, set()).add(v)
    for part in data.get("multipart", []):
        conds = part.get("when", {})
        for c in conds.get("OR", [conds]) if "OR" in conds else [conds]:
            for k, v in c.items():
                props.setdefault(k, set()).update(str(v).split("|"))
    states[name] = props

with zipfile.ZipFile(jar) as z:
    for n in z.namelist():
        m = re.fullmatch(r"assets/minecraft/blockstates/([a-z0-9_]+)\.json", n)
        if m: learn("minecraft:" + m.group(1), json.loads(z.read(n)))
        m = re.fullmatch(r"assets/minecraft/(?:models/item|items)/([a-z0-9_]+)\.json", n)
        if m: items.add("minecraft:" + m.group(1))
for f in glob.glob(str(ROOT / "src/*/resources/assets/fletcherstrestle/blockstates/*.json")):
    learn("fletcherstrestle:" + pathlib.Path(f).stem, json.loads(open(f).read()))
for f in glob.glob(str(ROOT / "src/*/resources/assets/fletcherstrestle/models/item/*.json")):
    items.add("fletcherstrestle:" + pathlib.Path(f).stem)
items |= set(states)  # block items
ENTITIES = {"minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:pillager",
            "fletcherstrestle:eagle", "fletcherstrestle:heavy_dummy"}
# Blocks drawn by a block-entity renderer list no properties in their blockstate file.
FACING4 = {"north", "south", "east", "west"}
for wood in ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"):
    states.setdefault(f"minecraft:{wood}_wall_sign", {})["facing"] = FACING4
states.setdefault("minecraft:chest", {}).update({"facing": FACING4, "type": {"single", "left", "right"}})
# Block properties that never affect the model, so blockstate files don't list them.
NON_MODEL = {"persistent", "distance", "waterlogged", "signal_fire", "powered", "bottom"}

errors, warnings = [], []
def block(spec, where):
    m = re.match(r"([a-z0-9_:]+)(\[[^\]]*\])?", spec)
    bid = m.group(1) if ":" in m.group(1) else "minecraft:" + m.group(1)
    if bid not in states:
        errors.append(f"{where}: unknown block {bid}"); return
    for kv in filter(None, (m.group(2) or "[]")[1:-1].split(",")):
        k, v = kv.split("=")
        known = states[bid].get(k)
        if known is None:
            (warnings if k in NON_MODEL else errors).append(f"{where}: {bid} property '{k}' not in its blockstate file")
        elif v not in known:
            errors.append(f"{where}: {bid}[{k}={v}] not one of {sorted(known)}")

def balanced(line, where):
    stack, quote = [], None
    pairs = {"}": "{", "]": "[", ")": "("}
    for ch in line:
        if quote:
            if ch == quote: quote = None
            continue
        if ch in "\"'": quote = ch
        elif ch in "{[(": stack.append(ch)
        elif ch in "}])":
            if not stack or stack.pop() != pairs[ch]:
                errors.append(f"{where}: unbalanced '{ch}'"); return
    if stack or quote: errors.append(f"{where}: unclosed {stack or quote}")

count = 0
for f in sorted(PACK.rglob("*.mcfunction")):
    for i, line in enumerate(f.read_text().splitlines(), 1):
        if not line or line.startswith("#"): continue
        where = f"{f.relative_to(PACK)}:{i}"; count += 1
        balanced(line, where)
        words = line.split(" ")
        if words[0] == "setblock": block(words[4], where)
        elif words[0] == "fill": block(words[7], where)
        elif words[0] == "summon" and words[1] not in ENTITIES: errors.append(f"{where}: unknown entity {words[1]}")
        elif words[0] == "give":
            gid = re.match(r"([a-z0-9_:]+)", words[2]).group(1)
            if gid not in items: errors.append(f"{where}: unknown item {gid}")
        for iid in re.findall(r'id:"([a-z0-9_:]+)"', line):
            if iid not in items and not iid.startswith("minecraft:water"):
                errors.append(f"{where}: unknown item {iid}")
        if words[0] == "function":
            target = PACK / "data" / words[1].split(":")[0] / "function" / (words[1].split(":")[1] + ".mcfunction")
            if not target.exists(): errors.append(f"{where}: missing function {words[1]}")

print(f"checked {count} commands")
for w in sorted(set(warnings)): print("note:", w)
for e in errors: print("ERROR:", e)
sys.exit(1 if errors else 0)
