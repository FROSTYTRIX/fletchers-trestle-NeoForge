# Fletcher's Trestle — Modpack Integration

This mod's bow and arrow material system is **fully data-driven**. Every
limb, riser, string, arrow head, shaft, and fletching is loaded from a
datapack registry — including all the stats and most of the on-hit /
on-flight behaviors. Modpack makers add new materials, swap old ones,
or tune everything by writing JSON. No companion mod required.

This document is the contract: what you can change, where the files go,
what each field does. The same JSONs the built-in materials ship as are
your reference — open
`src/generated/resources/data/fletcherstrestle/fletcherstrestle/` in this
repo to see real examples.

---

## At a glance

A "material" is one of six things:

| Slot           | Datapack registry key                  | Source of truth                              |
|----------------|----------------------------------------|----------------------------------------------|
| Bow limb       | `fletcherstrestle:bow_limb`            | `data/<pack>/fletcherstrestle/bow_limb/<id>.json` |
| Bow riser      | `fletcherstrestle:bow_riser`           | `data/<pack>/fletcherstrestle/bow_riser/<id>.json` |
| Bow string     | `fletcherstrestle:bow_string`          | `data/<pack>/fletcherstrestle/bow_string/<id>.json` |
| Arrow head     | `fletcherstrestle:arrow_head`          | `data/<pack>/fletcherstrestle/arrow_head/<id>.json` |
| Arrow shaft    | `fletcherstrestle:arrow_shaft`         | `data/<pack>/fletcherstrestle/arrow_shaft/<id>.json` |
| Arrow fletching| `fletcherstrestle:arrow_fletching`     | `data/<pack>/fletcherstrestle/arrow_fletching/<id>.json` |

Each JSON has the same shape:

```json
{
  "ingredient": { "item": "minecraft:iron_ingot" },
  "stats": { "...": "..." },
  "texture": "mypack:entity/projectiles/head/steel",
  "effects": [
    { "type": "fletcherstrestle:apply_effect", "...": "..." }
  ]
}
```

- **`ingredient`** — what items the fletching menu accepts for this slot.
  Supports the full vanilla `Ingredient` syntax (single item, item list,
  or tag — see below).
- **`stats`** — per-part-type numeric stats (draw time, damage multiplier,
  durability, etc.). Schema differs per slot; see [Stats schemas](#stats-schemas).
- **`texture`** — optional. Overrides the conventional texture path used
  by the arrow renderer. Falls back to a sensible default if absent.
- **`effects`** — optional list of declarative behaviors. The full
  vocabulary is in the [Effects reference](#effects-reference) below.

The registry-key path of the JSON (e.g. `oak.json` → id `mypack:oak`)
becomes the material's canonical id, used by both the assembly
components and the texture path. It also becomes the suffix of the
material's translation key: `material.<namespace>.<path>`. So
`mypack:steel` looks for `material.mypack.steel` in your lang file.

---

## Ingredient syntax

`ingredient` accepts everything vanilla's `Ingredient` codec does:

```json
"ingredient": { "item": "mymod:steel_ingot" }
```

```json
"ingredient": { "tag": "c:ingots/steel" }
```

```json
"ingredient": [
  { "item": "mymod:steel_ingot" },
  { "item": "mymod:hardened_steel_ingot" }
]
```

Tags are the friendliest option for cross-mod compatibility — `c:` tags
work across most modpacks already.

---

## Stats schemas

Each part type has its own stats schema. Every field is optional; missing
fields fall back to sane defaults.

### `bow_limb`
```json
"stats": {
  "draw_time_ticks": 20.0,
  "damage_multiplier": 1.0,
  "amphibious": false,
  "gives_slow_falling": false
}
```

| Field                 | Default | Meaning                                                                  |
|-----------------------|---------|--------------------------------------------------------------------------|
| `draw_time_ticks`     | required| Ticks needed for a full bow draw. Vanilla bow is 20.                     |
| `damage_multiplier`   | 1.0     | Multiplier applied to base arrow damage.                                 |
| `amphibious`          | false   | Whether shooting works at full strength underwater.                      |
| `gives_slow_falling`  | false   | Whether aiming this bow grants Slow Falling to the player.               |

Optional sub-record `crossbow_overrides` lets a single limb tune its
stats just for the crossbow:

```json
"crossbow_overrides": {
  "stats": { "draw_time_ticks": 30.0 }
}
```

### `bow_riser`
```json
"stats": {
  "max_durability": 250,
  "inaccuracy_multiplier": 1.0
}
```

| Field                   | Default | Meaning                                                       |
|-------------------------|---------|---------------------------------------------------------------|
| `max_durability`        | required| Weapon durability cap when this riser is used.                |
| `inaccuracy_multiplier` | 1.0     | Multiplier on base arrow inaccuracy. 0.2 = laser-precise.     |

### `bow_string`
```json
"stats": {
  "velocity_multiplier": 1.0,
  "durability_cost": 1
}
```

| Field                 | Default | Meaning                                              |
|-----------------------|---------|------------------------------------------------------|
| `velocity_multiplier` | 1.0     | Multiplier on projectile initial speed.              |
| `durability_cost`     | 1       | Durability consumed per shot.                        |

### `arrow_head`
```json
"stats": { "damage_multiplier": 1.0 }
```

### `arrow_shaft`
```json
"stats": {
  "velocity_multiplier": 1.0,
  "gravity_multiplier": 1.0
}
```

| Field                 | Default | Meaning                                                          |
|-----------------------|---------|------------------------------------------------------------------|
| `velocity_multiplier` | 1.0     | Multiplier on initial velocity at spawn.                         |
| `gravity_multiplier`  | 1.0     | Multiplier on arrow gravity; >1 drops faster, <1 floats.         |

### `arrow_fletching`
```json
"stats": { "inaccuracy_multiplier": 1.0 }
```

Sub-1.0 makes the arrow group tighter; above 1.0 spreads it.

---

## Effects reference

Effects let a JSON describe **behavior** in addition to stats. They fire
at different points in the projectile's lifecycle. The same `type`
vocabulary works on any def, but each effect type is opinionated about
*when* it runs — see the **fires on** column.

Effects compose: you can attach as many as you like, in any combination.
Two `apply_effect` entries on the same head will apply two different
status effects on hit.

### On-hit damage modifiers (run BEFORE damage applies)

#### `fletcherstrestle:damage_multiplier`
Scales base damage by a constant. Fires at arrow spawn.
```json
{ "type": "fletcherstrestle:damage_multiplier", "multiplier": 1.25 }
```

#### `fletcherstrestle:damage_multiplier_if_target_below_health`
Bonus damage to wounded targets. **Built-in:** crimson shaft executioner.
```json
{ "type": "fletcherstrestle:damage_multiplier_if_target_below_health",
  "threshold": 0.5, "multiplier": 1.5 }
```

| Field        | Required | Meaning                                                  |
|--------------|----------|----------------------------------------------------------|
| `threshold`  | yes      | Multiplier applies when target HP fraction is below this |
| `multiplier` | yes      | Scale factor applied to base damage                      |

#### `fletcherstrestle:damage_multiplier_on_backstab`
Bonus damage when the arrow arrives from behind. **Built-in:** pale_oak shaft.
```json
{ "type": "fletcherstrestle:damage_multiplier_on_backstab",
  "dot_threshold": 0.5, "multiplier": 1.4,
  "sound": "minecraft:entity.breeze.wind_burst" }
```

| Field           | Default | Meaning                                                                |
|-----------------|---------|------------------------------------------------------------------------|
| `dot_threshold` | 0.5     | dot(target_view, arrow_dir) must exceed this for a hit to count        |
| `multiplier`    | yes     | Scale factor                                                           |
| `sound`         | none    | Optional sound played on successful backstab                           |

#### `fletcherstrestle:damage_multiplier_by_distance`
Bonus damage proportional to distance traveled. **Built-in:** weighted_blunt.
```json
{ "type": "fletcherstrestle:damage_multiplier_by_distance", "per_block": 100 }
```

Adds `1×` base damage per `per_block` blocks of travel (i.e. doubles
damage at 100 blocks, triples at 200, …).

#### `fletcherstrestle:damage_multiplier_if_target_armored`
Bonus damage when target has any armor. **Built-in:** bodkin_point.
```json
{ "type": "fletcherstrestle:damage_multiplier_if_target_armored", "multiplier": 1.25 }
```

#### `fletcherstrestle:pierce_level`
Sets the arrow's pierce level (passes through N entities). **Built-in:** dark_oak.
```json
{ "type": "fletcherstrestle:pierce_level", "level": 1 }
```

### On-hit side effects (run AFTER damage applies)

#### `fletcherstrestle:apply_effect`
Applies a MobEffect to the target. **Built-ins:** broadhead (bleed), mangrove (slowness).
```json
{ "type": "fletcherstrestle:apply_effect",
  "effect": "minecraft:wither",
  "duration": 100,
  "amplifier": 1 }
```

| Field       | Default | Meaning                                       |
|-------------|---------|-----------------------------------------------|
| `effect`    | yes     | Registry id of a MobEffect                    |
| `duration`  | yes     | Effect duration in ticks                      |
| `amplifier` | 0       | 0 = level I, 1 = level II, etc.               |

#### `fletcherstrestle:heal_shooter`
Heals the shooter when the arrow hits. **Built-in:** cherry shaft.
```json
{ "type": "fletcherstrestle:heal_shooter",
  "amount": 2.0,
  "particle": { "type": "minecraft:cherry_leaves" },
  "particle_count": 5 }
```

| Field            | Default | Meaning                                              |
|------------------|---------|------------------------------------------------------|
| `amount`         | yes     | Half-hearts healed (2.0 = 1 heart)                   |
| `particle`       | none    | Optional particle type spawned at target             |
| `particle_count` | 5       | Number of particles                                  |

#### `fletcherstrestle:pull_target_to_shooter`
Yanks the target toward the shooter on impact. **Built-in:** barbed_tip.
```json
{ "type": "fletcherstrestle:pull_target_to_shooter",
  "strength": 0.75, "min_lift": 0.25 }
```

#### `fletcherstrestle:teleport_swap_with_target`
With chance, swaps the shooter's and target's positions. **Built-in:** warped shaft.
```json
{ "type": "fletcherstrestle:teleport_swap_with_target", "chance": 1.0 }
```

#### `fletcherstrestle:drop_self_on_hit`
With chance, drops the arrow as an item instead of consuming it. **Built-in:** bound fletching.
```json
{ "type": "fletcherstrestle:drop_self_on_hit", "chance": 0.25 }
```

### Tick-time effects (run every server tick while in flight)

#### `fletcherstrestle:set_velocity_multiplier_at_tick`
Multiplies arrow velocity at a specific tick of flight. **Built-in:** acacia shaft.
```json
{ "type": "fletcherstrestle:set_velocity_multiplier_at_tick",
  "tick": 10, "multiplier": 1.4 }
```

#### `fletcherstrestle:subtle_homing`
Pulls the arrow toward the nearest non-shooter living entity. **Built-in:** serrated fletching.
```json
{ "type": "fletcherstrestle:subtle_homing",
  "range": 5.0, "strength": 1.0, "grace_ticks": 2 }
```

| Field         | Default | Meaning                                                            |
|---------------|---------|--------------------------------------------------------------------|
| `range`       | 5.0     | Search radius in blocks                                            |
| `strength`    | 1.0     | Velocity-vector add magnitude toward target                        |
| `grace_ticks` | 2       | Skip homing for the first N ticks (lets initial trajectory hold)   |

### Block-hit effects

#### `fletcherstrestle:bounce_on_block`
Chance to ricochet off blocks instead of embedding. **Built-in:** jungle shaft.
```json
{ "type": "fletcherstrestle:bounce_on_block",
  "chance": 0.85, "max_bounces": 3, "retention": 0.3 }
```

| Field         | Default | Meaning                                                     |
|---------------|---------|-------------------------------------------------------------|
| `chance`      | 1.0     | Probability per block hit (0.0 – 1.0)                       |
| `max_bounces` | 3       | Hard cap on bounces per arrow                               |
| `retention`   | 0.3     | Fraction of velocity retained per bounce                    |

### Bow/crossbow on-release effects

These fire when a shot is released. Attach them to a bow limb, riser,
or string def.

#### `fletcherstrestle:ignite_arrow`
Sets the fired arrow on fire. **Built-in:** crimson limb.
```json
{ "type": "fletcherstrestle:ignite_arrow", "seconds": 100 }
```

#### `fletcherstrestle:set_arrow_no_gravity`
Removes gravity from the fired arrow. **Built-in:** warped limb.
```json
{ "type": "fletcherstrestle:set_arrow_no_gravity" }
```

#### `fletcherstrestle:set_arrow_flag`
Stamps a boolean key on the fired arrow's persistent-data NBT. **Built-ins:**
spruce limb (`fletcherstrestle:punch`), copper riser (`fletcherstrestle:conductive`).
```json
{ "type": "fletcherstrestle:set_arrow_flag",
  "key": "mypack:my_custom_flag", "value": true }
```

#### `fletcherstrestle:apply_effect_to_shooter`
Applies a MobEffect to the **shooter** on release. **Built-in:** acacia limb.
```json
{ "type": "fletcherstrestle:apply_effect_to_shooter",
  "effect": "minecraft:speed", "duration": 30, "amplifier": 1 }
```

---

## Textures

By default the arrow renderer looks for entity textures at:

```
assets/<namespace>/textures/entity/projectiles/<part>/<id>.png
```

For example, the built-in flint arrow head is at
`assets/fletcherstrestle/textures/entity/projectiles/head/flint.png`.

A modpack that adds `mypack:steel` as an arrow head should ship its
texture at:

```
assets/mypack/textures/entity/projectiles/head/steel.png
```

If you want to override the conventional path (e.g. share one texture
across multiple materials), set the optional `texture` field in your
material JSON:

```json
"texture": "mypack:entity/projectiles/head/shared_metal"
```

The string is interpreted as a `ResourceLocation`; the `.png` extension
is added automatically.

> **Note:** datapacks ship server-side by default. Textures live in
> *resource packs*. A modpack that adds a material with a custom texture
> needs to ship both — usually combined into one zip.

Item models for inventory display follow the same convention. The
fletching menu icon system reads them from the same path.

---

## Translation

Every material's display name comes from a translation key:

```
material.<namespace>.<path>
```

For `mypack:steel`, ship `assets/mypack/lang/en_us.json`:

```json
{ "material.mypack.steel": "Steel" }
```

The bow / arrow tooltip code automatically resolves this key. If you
don't ship a translation, the in-game tooltip falls back to the raw id.

The built-in materials ship their English names in
`assets/fletcherstrestle/lang/en_us.json` — see that file for the
expected casing and capitalization style.

---

## End-to-end example

Adding a new arrow head: **Steel Spike** — 1.5× damage, +25% on armored
targets, applies Wither II for 4 seconds.

### 1. JSON

`data/mypack/fletcherstrestle/arrow_head/steel_spike.json`:
```json
{
  "ingredient": { "tag": "c:ingots/steel" },
  "stats": { "damage_multiplier": 1.5 },
  "effects": [
    { "type": "fletcherstrestle:damage_multiplier_if_target_armored",
      "multiplier": 1.25 },
    { "type": "fletcherstrestle:apply_effect",
      "effect": "minecraft:wither",
      "duration": 80,
      "amplifier": 1 }
  ]
}
```

### 2. Texture

Drop a 16×16 PNG at:
```
assets/mypack/textures/entity/projectiles/head/steel_spike.png
```

### 3. Translation

`assets/mypack/lang/en_us.json`:
```json
{ "material.mypack.steel_spike": "Steel Spike" }
```

### 4. Done

Steel ingots now show up as a valid head input in the fletching menu.
The crafted arrow has 1.5× damage baseline, +25% when hitting armored
targets, applies Wither II for 4 seconds on hit. Tooltip says "Steel
Spike". Renders with your texture in-flight and in inventory.

No companion mod required.

---

## What's still hardcoded

A handful of head behaviors are too entangled with the arrow's tick
lifecycle to externalise cleanly without a richer state API. These stay
keyed off built-in material ids in Java code:

- `glass_vial` — splashes its stored potion contents on impact.
- `resonance_tip` — delayed echo damage with target-locking.
- `weighted_hook` — sticks to blocks and pulls the shooter.
- `trailing_rope` — drops a chain of rope blocks downward from impact.
- `vex` (fletching) — phases through one block of cover.

A modpack can still **add** new materials with these ids (or override
the built-ins) and they'll use the standard data path, but the
specific stateful behaviors above only fire if the id string matches
the built-in id. A future API extension may expose hooks to lift these
to the effect system too — until then, treat their ids as reserved
keywords.

---

## Extending the vocabulary in Java

If the closed vocabulary above doesn't cover a behavior you need, a
companion mod can register new `MaterialEffectType` entries against the
existing `fletcherstrestle:material_effect_type` registry. Pattern:

```java
public static final DeferredRegister<MaterialEffectType<?>> EFFECT_TYPES =
    DeferredRegister.create(
        net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes.REGISTRY_KEY,
        "mypack");

public static final Supplier<MaterialEffectType<MyCustomEffect>> MY_EFFECT =
    EFFECT_TYPES.register("my_effect",
        () -> new MaterialEffectType<>(MyCustomEffect.CODEC));
```

Call `EFFECT_TYPES.register(modEventBus)` from your mod constructor.

Your `MyCustomEffect` class implements `MaterialEffect`, overrides
whichever lifecycle hooks it cares about (`onArrowSpawn`, `onArrowTick`,
`onPreArrowHit`, `onArrowHit`, `onArrowHitBlock`, `onBowRelease`,
`onProjectileFired`), and exposes a `MapCodec<MyCustomEffect>` for
JSON parsing.

Modpack JSONs reference it as `"type": "mypack:my_effect"`.

---

## Recipes

The fletching menu's recipe is itself data-driven via tags:

| Tag                                | Slot it gates                                       |
|------------------------------------|-----------------------------------------------------|
| `fletcherstrestle:bow_limbs`       | Bow limb slot (pliable / steamed limbs)             |
| `fletcherstrestle:rough_limbs`     | Arrow shaft slot (unsteamed limbs + sticks)         |
| `fletcherstrestle:bow_risers`      | Riser slot                                          |
| `fletcherstrestle:bow_strings`     | Bow string slot                                     |
| `fletcherstrestle:arrow_heads`     | Arrow head slot                                     |
| `fletcherstrestle:arrow_fletching` | Arrow fletching slot                                |

A modpack adding a new material should also extend the matching tag so
the fletching menu accepts the item in that slot. The material's
`ingredient` field controls **which** material id results; the tag
controls **whether the slot accepts the item at all**.

---

## Sanity checklist when adding a material

1. JSON at `data/<pack>/fletcherstrestle/<part>/<id>.json` ✓
2. Texture at `assets/<pack>/textures/entity/projectiles/<part>/<id>.png` ✓
3. Translation key `material.<pack>.<id>` in your lang file ✓
4. Ingredient item exists (single item, item list, or tag) ✓
5. Tag entry for the fletching menu slot ✓

That's it.
