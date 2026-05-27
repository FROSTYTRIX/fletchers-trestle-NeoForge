# Fletcher's Trestle — 1.6.0

A big update. The headline is **modpack compatibility** — pack makers
can now add their own bow / arrow materials purely from JSON. Beyond
that: a whole new **eagle companion** system, **potion-tipped modular
arrows**, a lot of new material behaviors that finish out the
component identity, and assorted polish + fixes.

---

## 🪶 Eagles — companion system (WIP)

A new feathered companion that can fetch your shot arrows, hunt with
you, and roost in nests. Full ecosystem ships in this release:

- **Tame** an eagle by feeding it raw meat. Tamed eagles bond to you
  and follow.
- **Smooth flight** with proper soar / dive animations, plus a
  spyglass-driven hunt lock-on — look at a target through a spyglass
  while your eagle is nearby and it dives at the target.
- **Fetch mode** — the eagle picks up arrows you've shot (up to 16 at
  a time, mixed types) and returns them to your inventory. Toggleable
  per-eagle. Has a busy-state "unstuck" guard so a confused eagle
  always recovers.
- **Eagle Whistle** item for remote fetch/recall control. Bind a
  whistle to a specific eagle, or use it as an "all eagles in range"
  caller.
- **Eagle Perch** block and **Eagle Nest** block — the nest auto-fills
  with eggs over time, supports egg-hatching, and has a full nesting
  cycle.
- **Mountain biome worldgen** — wild nests generate on exposed ridges
  in mountain-tagged biomes; wild eagles patrol them.

> ⚠️ The eagle model is still being finalised, so **natural spawning
> is disabled by default** for this release. The spawn egg still works
> and is labeled `[WIP] Eagle Spawn Egg` in the creative menu so
> there's no surprise about its placeholder state. To enable wild
> spawning anyway, flip `eagles.natural_spawning = true` in
> `config/fletcherstrestle-server.toml` — everything else (perch,
> nest, fetch, whistle, hunt) is fully active.

---

## 🧪 Glass-Vial Potion Arrows

A new arrow head + a new way to use the dipping vat:

- Craft a modular arrow with a **glass bottle** in the head slot →
  Glass-Vial arrow (empty payload, low damage).
- Dip the empty glass-vial arrow in the Dipping Vat while any potion
  fills the tank → the arrow is now tipped with that potion.
- On hit, the vial shatters and **splashes** the potion onto whatever
  it struck — full effect at the impact point, weakening with
  distance like a vanilla splash potion.
- **JEI now indexes per-potion entries** for the dipping recipe so
  searching for "Jump Boost Arrow", "Strength Arrow", etc. surfaces
  the recipe directly.
- Tooltip polish across the board so dipped arrows clearly show their
  potion contents and stack with matching variants.

---

## ✨ Modpack Compatibility (data-driven materials)

The entire bow / arrow material system is now driven by JSON
datapack registries. Every built-in material — every wood limb, every
riser, every string, every arrow head/shaft/fletching — ships as a
JSON file inside the mod, identical to what a modpack would write.
Behavior is unchanged for the built-ins; the win is the extension
surface.

- **Six new datapack registries** at
  `data/<pack>/fletcherstrestle/{bow_limb,bow_riser,bow_string,arrow_head,arrow_shaft,arrow_fletching}/<id>.json`.
- **Ingredient field** accepts `{"item": "..."}`, tag references
  (`{"tag": "c:ingots/steel"}`), or a list of either — any modded
  item can map onto a fletcher's-trestle material without code.
- **20-effect vocabulary** covers everything the built-ins do: bleed,
  armor-pierce, executioner, backstab, distance-damage, heal-shooter,
  target-pull, teleport-swap, drop-on-hit, mid-flight boost,
  magnetism, bounce, ignite, no-gravity, persistent flag, shooter
  buff, pierce, status effect, damage multiplier, scripted callback.
- **KubeJS support** via the `scripted_callback` effect type — KubeJS
  startup scripts can register Java-side callbacks under a string id
  and reference them from material JSONs.
- **Translation keys** — material display names use
  `material.<namespace>.<path>` so modpack lang files automatically
  localise names in tooltips and JEI.
- **Texture overrides** — each def has an optional `texture` field;
  if absent, the renderer falls back to the conventional path **in
  the material's own namespace** (so `mypack:steel` head pulls its
  PNG from `mypack:textures/...` automatically).
- **Full reference docs** in `MODPACK_INTEGRATION.md` at the repo
  root with end-to-end examples and the complete effect vocabulary.

For existing players: nothing changes. For modpack makers: the
surface is documented and stable.

---

## 🎯 New material identities

The "component identity pass" filled in long-missing per-material
behaviors so every choice in the fletching menu now has a real
gameplay reason. New/Fixed on-hit / on-flight effects:

- **Barbed Tip** head — yanks the hit target toward the shooter on
  impact.
- **Vex** fletching — phases through one block of cover, once per
  arrow.
- **Pale Oak** shaft — backstab bonus damage (+40% when hitting from
  behind).
- **Mangrove** shaft — applies Slowness III for 1 second on hit.
- **Cherry** shaft — heals the shooter ½ heart and spawns
  cherry-leaves particles on hit ("Petal Burst").
- **Warped** shaft — chance to swap positions with the target on hit
  ("Translocation").
- **Crimson** shaft — executioner bonus (+50% damage to targets
  below half health).
- **Bound** fletching — 25% chance to drop the arrow as an item on
  hit so you can pick it back up.
- **Dark Oak** shaft — built-in Piercing I.
- **Spruce** limb — built-in Punch I.
- **Copper** riser — conductive (calls down a lightning bolt on hit
  during a thunderstorm).
- **Crimson** limb — ignites the fired arrow.
- **Warped** limb — fires gravity-less arrows.
- **Acacia** limb — gives the shooter Speed II for 30 ticks on
  release.
- **Acacia** shaft — mid-flight speed boost at tick 10.
- **Serrated** fletching — subtle magnetism, gently homes toward the
  nearest target within 5 blocks.

---

## 📖 Fletcher's Guide

The in-game guide (the temporary item) now links straight to the project wiki for the
full crafting tree and material reference, rather than trying to fit
everything inside the book itself.

---

## 🏹 Bug fixes

- **Bow draw timing is finally consistent across all three visual
  channels.** The FOV zoom, the texture pull-stages (`_pulling_0/1/2`)
  AND the held-item Z-stretch now all complete at the same instant —
  matched to the per-limb draw time. Birch limbs (10-tick draw) fully
  zoom + stretch + render fully drawn at tick 10. Dark oak (35-tick
  draw) waits the full 35 ticks. Previously the three channels
  desynced for any limb whose draw time wasn't exactly 20.
- **Modular arrow entity rendering** — fixed a regression where some
  arrows showed multiple head + fletching layers at once mid-flight,
  including the trailing-rope head sometimes appearing on every
  modular arrow.
- **Trailing-rope head rendering** — fixed Z-fighting on the visible
  rope mesh.
---

## 🧑‍💻 For developers / pack makers

`MODPACK_INTEGRATION.md` at the repo root is the contract for adding
new materials. Three escalating extension tiers:

1. **JSON only** — the common path; covers ~95% of pack needs.
2. **JSON + KubeJS** — `scripted_callback` effect type lets scripts
   attach arbitrary on-hit / on-tick / on-release logic.
3. **Java companion mod** — register a new `MaterialEffectType`
   against the live `fletcherstrestle:material_effect_type` registry
   to grow the vocabulary itself.

A handful of head behaviors stay keyed off their built-in ids
(glass-vial splash, resonance echo, weighted hook, trailing rope, vex
phase) — they need a richer arrow-state API to externalise cleanly.
Documented as reserved id keywords in the integration MD.
