# Building the structure NBTs

The worldgen JSON, biome tags and loot tables are already wired in the
mod. The only thing left is the actual building, saved as a **structure
NBT**. This is done in-game with a **Structure Block** — no modding tools
needed.

> ⚠️ Until the `.nbt` exists, the structure references a missing template
> and Minecraft will log errors when it tries to generate. Add the NBT
> before testing worldgen.

---

## Fletcher's Camp — `fletcherstrestle:fletchers_camp`

A small abandoned woodworking camp. Suggested footprint: **~9 × 9**, one
to two blocks tall plus the floor.

### What to build

- A **floor** of dirt / coarse dirt / path blocks on the bottom layer.
- The three stations so players see them in the wild:
  **Shaving Horse**, **Steam Box**, **Fletching Table**.
- A **campfire**, a couple of **logs / stripped logs** as seats or piles,
  maybe a tent of wool/leaves or a fence-and-trapdoor lean-to.
- One **chest** with the camp loot (set up below).
- Keep it grounded — put the build's lowest blocks on the bottom layer of
  the structure bounds so it sits flush on terrain (`beard_thin` terrain
  adaptation blends a small foundation underneath).

### Set the chest's loot table

Place the chest, point at it, and run:

```
/data merge block ~ ~ ~ {LootTable:"fletcherstrestle:chests/fletchers_camp"}
```

(aim at the chest; `~ ~ ~` works if you stand on it, otherwise use its
coordinates). The Structure Block saves this tag, so the chest fills from
the loot table on world generation — don't put items in it by hand.

### Save it

1. Place a **Structure Block** and set it to **Save** mode.
2. **Structure Name:** `fletcherstrestle:fletchers_camp`
3. Set the **size/offset** so the bounding box encloses the whole build
   (the corner of the box is the structure origin; keep the floor at the
   bottom of the box).
4. Toggle **"Include entities"** off (unless you intentionally placed
   item frames / armor stands).
5. Click **SAVE**. The file is written to:
   `<world>/generated/fletcherstrestle/structures/fletchers_camp.nbt`

### Drop it into the mod

Copy that file to:

```
src/main/resources/data/fletcherstrestle/structure/fletchers_camp.nbt
```

(note: the folder is `structure`, singular). That's it — the structure
will now generate in forest / taiga / plains / meadow biomes.

### Test

- `/place structure fletcherstrestle:fletchers_camp` — drops one at your
  feet (great for checking the build + loot without exploring).
- `/locate structure fletcherstrestle:fletchers_camp` — find the nearest
  naturally-generated one.

### Tuning (all already wired, edit if you want)

- **Rarity:** `worldgen/structure_set/fletchers_camp.json` →
  `spacing` / `separation` (bigger = rarer; villages are 34 / 8).
- **Biomes:** `tags/worldgen/biome/has_structure/fletchers_camp.json`.
- **Loot:** `loot_table/chests/fletchers_camp.json`.
