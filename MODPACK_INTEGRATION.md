# Fletcher's Trestle — Modpack Integration

This mod's bow and arrow material system is **fully data-driven**. Every
limb, riser, string, arrow head, shaft, and fletching is loaded from a
datapack registry, including all the stats and every on-hit / on-flight
behavior. No material id is special-cased in code. Modpack makers add new materials, swap old ones,
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

> **Tie-break:** if two registered defs accept the same item (e.g. a
> modpack adds a `mypack:bronze` head and another datapack later adds
> `otherpack:bronze` whose ingredient also lists `c:ingots/bronze`),
> the resolver returns the **first match by registry-iteration order**.
> Within a single datapack the order is alphabetical by id. If you
> need deterministic precedence, narrow your ingredient (use a more
> specific tag or list explicit items) so only one def matches the
> stack.

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
  "gives_slow_falling": false,
  "agility": false,
  "photosynthetic": true
}
```

| Field                 | Default | Meaning                                                                  |
|-----------------------|---------|--------------------------------------------------------------------------|
| `draw_time_ticks`     | required| Ticks needed for a full bow draw. Vanilla bow is 20.                     |
| `damage_multiplier`   | 1.0     | Multiplier applied to base arrow damage.                                 |
| `amphibious`          | false   | Whether shooting works at full strength underwater.                      |
| `gives_slow_falling`  | false   | Whether aiming this bow grants Slow Falling to the player.               |
| `agility`             | false   | The archer walks at full speed while drawing.                            |
| `photosynthetic`      | true    | The wood can feed on sunlight, which the Photosynthesis enchantment needs. Crimson and warped set it false. |

A **composite** (two different woods) averages draw time and damage, keeps
every trait either wood has, and is only `photosynthetic` if both woods are.

Optional sub-record `crossbow_overrides` lets a single limb tune its
stats just for the crossbow:

```json
"crossbow_overrides": {
  "stats": { "draw_time_ticks": 30.0 }
}
```

> Only **stats** can be overridden per-weapon; the limb's `effects`
> list is shared between the bow and the crossbow. If you need
> different on-fire behavior between the two, ship two separate limb
> defs with different ingredients.

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
| `metal`                 | false   | A metal riser: it can carry a string that `requires_metal_riser`, and it rules out Photosynthesis. |

### `bow_string`
```json
"stats": {
  "velocity_multiplier": 1.0,
  "durability_cost": 1,
  "requires_metal_riser": false,
  "overdraw_shake": false
}
```

| Field                  | Default | Meaning                                              |
|------------------------|---------|------------------------------------------------------|
| `velocity_multiplier`  | 1.0     | Multiplier on projectile initial speed.              |
| `durability_cost`      | 1       | Durability consumed per shot.                        |
| `requires_metal_riser` | false   | Only builds (and only rolls on mobs and loot) with a `metal` riser. |
| `overdraw_shake`       | false   | Holding past full draw shakes the aim, as flax does. The Aim skill lengthens the grace period. |

Strings also take an optional top-level **`release_sound`**: the sound id the
bow plays when it looses an arrow. It's a plain id, so it can name a sound
defined only in your resource pack's `sounds.json`.

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
at different points in the projectile's lifecycle. Each effect type is
opinionated about *when* it runs, which means it's also opinionated
about *which def types* it makes sense to attach to.

### Lifecycle hooks ↔ where to attach

| Effect runs on this lifecycle hook                    | Only fires when attached to             |
|-------------------------------------------------------|-----------------------------------------|
| `onArrowSpawn`, `onArrowTick`, `onPreArrowHit`, `onArrowHit`, `onArrowHitBlock`, `replacesArrowHit`, `replacesArrowHitBlock` | `arrow_head` / `arrow_shaft` / `arrow_fletching` |
| `onBowRelease`, `onProjectileFired`                   | `bow_limb` / `bow_riser` / `bow_string` |

The dispatch is one-directional — the bow doesn't run arrow-hit
effects, and the arrow doesn't run bow-release effects. Attaching a
`apply_effect` (arrow on-hit) to a `bow_limb` is a silent no-op: the
JSON parses, the def loads, but the bow-release path never invokes
that hook. Each effect's section below says which def types it makes
sense to attach to.

### Ordering & composition

Effects compose: attach as many as you like, in any combination.
Two `apply_effect` entries on the same head apply two different
status effects on hit. Within a single def's effects list, hooks fire
**in JSON list order** — the first effect's hook runs first, then the
second, and so on. Across def types (head + shaft + fletching), the
firing order at each lifecycle phase is head → shaft → fletching for
arrows, and limb → riser → string for bows.

### On-hit damage modifiers (run BEFORE damage applies)

*Attach to: `arrow_head`, `arrow_shaft`, or `arrow_fletching`.*

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

*Attach to: `arrow_head`, `arrow_shaft`, or `arrow_fletching`.*

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

*Attach to: `arrow_head`, `arrow_shaft`, or `arrow_fletching`.*

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

*Attach to: `arrow_head`, `arrow_shaft`, or `arrow_fletching`.*

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

### Arrow specials (the arrow carries them out)

*Attach to: `arrow_head`, `arrow_shaft` or `arrow_fletching`.* These hold
state across ticks, so the arrow runs them itself; the effect switches them
on and sets their numbers. Put one on any part of your own to reuse it.

#### `fletcherstrestle:black_hole`
The arrow collapses into a black hole where it lands and is used up. No parameters.

#### `fletcherstrestle:splash_potion`
```json
{ "type": "fletcherstrestle:splash_potion", "radius": 4.0 }
```
The head carries a potion. Only heads with this effect can be dipped in
the Dipping Vat. On impact it shatters and splashes the potion over every
mob within `radius` blocks, weaker toward the edge. Once dipped, the item
model adds a `<head texture>_liquid` layer tinted to the potion.

#### `fletcherstrestle:resonance`
```json
{ "type": "fletcherstrestle:resonance", "delay": 20, "damage_factor": 0.3 }
```
The arrow lodges in the mob it hits; after `delay` ticks it goes off for
`damage_factor` times its impact damage, ignoring the hurt cooldown.

#### `fletcherstrestle:phase_through_blocks`
```json
{ "type": "fletcherstrestle:phase_through_blocks", "blocks": 1 }
```
The arrow slips through the first `blocks` blocks it hits.

#### `fletcherstrestle:grapple`
```json
{ "type": "fletcherstrestle:grapple", "pull": 0.15, "max_ticks": 100, "max_distance": 32.0 }
```
The arrow hooks into the block it hits and reels the shooter in by `pull`
per tick, until they arrive, drift past `max_distance`, or `max_ticks` pass.

#### `fletcherstrestle:deploy_rope`
```json
{ "type": "fletcherstrestle:deploy_rope", "max_length": 20 }
```
Shot into the underside of a block, the arrow anchors there and lets down
a climbable rope, up to `max_length` blocks or until it reaches the floor.

When an arrow has several, the order is: black hole, then phasing (block
hits only), then splash potion, then resonance (mob hits) or grapple and
rope (block hits).

### Scripted escape hatch

*Attach to: any def — the handler chooses which lifecycle hooks to
implement.*

#### `fletcherstrestle:scripted_callback`
Looks up a named callback in the live `ScriptedEffectCallbacks`
registry at runtime and delegates every lifecycle hook to it. The
escape valve for behaviors the closed vocabulary can't express —
intended for **KubeJS scripts** and **Java companion mods** that
want to attach custom logic without registering a new effect type.

```json
{ "type": "fletcherstrestle:scripted_callback", "id": "mypack:my_hit" }
```

| Field | Required | Meaning                                                |
|-------|----------|--------------------------------------------------------|
| `id`  | yes      | The callback id to look up. Free-form ResourceLocation |

If no handler is registered for the id, the effect silently no-ops
at runtime and a single warning is logged to flag the typo. This is
intentional — a missing KubeJS callback should NOT crash the game.

**KubeJS example** (`kubejs/startup_scripts/fletcher_hooks.js`):
```js
const Callbacks = Java.loadClass(
    'net.frostytrix.fletcherstrestle.material.ScriptedEffectCallbacks');
const RL = Java.loadClass('net.minecraft.resources.ResourceLocation');

Callbacks.register(RL.parse('mypack:wither_on_hit'), {
    onArrowHit: (arrow, hit) => {
        const target = hit.getEntity();
        if (target && target.addEffect) {
            const wither = Java.loadClass('net.minecraft.world.effect.MobEffects').WITHER;
            const inst = new (Java.loadClass('net.minecraft.world.effect.MobEffectInstance'))(
                wither, 100, 1);
            target.addEffect(inst);
        }
    }
});
```

Then `data/mypack/fletcherstrestle/arrow_head/cursed.json`:
```json
{
  "ingredient": { "tag": "c:ingots/cursed" },
  "stats": { "damage_multiplier": 1.3 },
  "effects": [
    { "type": "fletcherstrestle:scripted_callback",
      "id": "mypack:wither_on_hit" }
  ]
}
```

The handler object can override any of the seven lifecycle hooks
(`onArrowSpawn`, `onArrowTick`, `onPreArrowHit`, `onArrowHit`,
`onArrowHitBlock`, `onBowRelease`, `onProjectileFired`) — each
defaults to a no-op, so leave out the ones you don't need.

**Java companion mod example**:
```java
public class MyHooks {
    public static void init() {
        ScriptedEffectCallbacks.register(
            ResourceLocation.fromNamespaceAndPath("mypack", "wither_on_hit"),
            new ScriptedEffectCallbacks.Handler() {
                @Override
                public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
                    if (result.getEntity() instanceof LivingEntity target) {
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                    }
                }
            });
    }
}
```

Call `MyHooks.init()` from your `FMLCommonSetupEvent` listener.

> **Re-registration**: calling `register()` again with the same id
> overwrites the previous handler. KubeJS hot-reloads work cleanly
> with this — every reload re-registers the latest version.

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
"texture": "mypack:item/modular_bow/limbs/shared_metal"
```

The string is interpreted as a **base path** that each renderer
appends its own suffix to:

- Inventory / item-model renderer appends the per-part suffix
  (`_limb_pulling_0/_1/_2`, `_riser`, `_string_pulling_0/_1/_2`,
  `_head`, `_shaft`, `_fletching`). No file extension.
- Entity (in-flight arrow) renderer appends `.png`.

So a single override base like `"mypack:custom/steel"` produces
`mypack:custom/steel_head` for the inventory icon and
`mypack:custom/steel.png` for the entity texture — ship both. There
is no way to override only one renderer; the field controls both.

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

No material is special-cased by id any more: every behavior above is an
effect or a stat, and the built-in materials use the same JSON a pack does.
What remains in code:

- The **crossbow stock** is always drawn from `fletcherstrestle:item/mechanical_trigger`.
- **Villager trades**: which professions and levels sell modular weapons, and
  their prices. The parts themselves are rolled from the registries.
- The **rope** that `deploy_rope` lets down is always this mod's rope block.

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

Two hooks take a hit over entirely: `replacesArrowHit` and
`replacesArrowHitBlock`. Return `true` and the arrow skips its own specials,
vanilla damage and every other effect for that impact, so finish the arrow
off yourself (usually `arrow.discard()`). They run before anything else.

Override `describe()` to give the effect a short trait name: the guidebook's
material tables are built from the live registries and list it.

Modpack JSONs reference it as `"type": "mypack:my_effect"`.

---

## Fletching Table slots

A slot accepts any item that a material def's `ingredient` matches, so a new
material needs nothing else. The item tags below are a second way in, for
items you want a slot to take without a def of their own:

| Tag                                | Slot                                                |
|------------------------------------|-----------------------------------------------------|
| `fletcherstrestle:bow_limbs`       | Bow limb slots (pliable / steamed limbs)            |
| `fletcherstrestle:rough_limbs`     | Arrow shaft slot (unsteamed limbs)                  |
| `fletcherstrestle:bow_risers`      | Riser slot                                          |
| `fletcherstrestle:bow_strings`     | Bow string slot                                     |
| `fletcherstrestle:arrow_heads`     | Arrow head slot                                     |
| `fletcherstrestle:arrow_fletching` | Arrow fletching slot                                |

The workshop recipes (`fletcherstrestle:shaving`, `steaming`, `dipping`) are
ordinary datapack recipes, so a new wood gets its rough and pliable limbs the
same way the built-ins do.

---

## Beyond materials

Everything else the mod hands out is data too.

### Armed mobs
`data/<ns>/data_maps/entity_type/mob_armory.json` lists which mobs spawn with
modular weapons and how they're made (chance to be modified, signature woods,
composite chance, weighted riser and string tables, tuning range). Biome woods
live in `data/<ns>/data_maps/worldgen/biome/native_woods.json`; keys can be
biome ids or `#tags`. The server config switch is `[world] armed_mobs`.

### Loot
The `fletcherstrestle:random_assembly` loot function turns a bare
`modular_bow` or `modular_crossbow` entry into a finished weapon:

```json
{ "function": "fletcherstrestle:random_assembly",
  "min_tuning": 0.6, "max_tuning": 0.95, "limbs": ["pale_oak"] }
```

`limbs` is optional (empty means any). Riser and string are random, and a
string that needs a metal riser always gets one.

### Garlands
Which items count as garland feathers, and their colours, is the item data
map `data/<ns>/data_maps/item/garland_feather.json`:

```json
{ "values": { "mypack:peacock_feather": { "colour": "#1F7A8C" } } }
```

The string can be anything in `#fletcherstrestle:garland_strings`.

### The Garrison
| What | Where |
|------|-------|
| Blocks a Bolt Warden is built from | block tag `fletcherstrestle:garrison_golem_body` (stripped logs and woods) |
| The item that winds it up (and fits the Crossbow Bench) | item tag `fletcherstrestle:mechanisms` |
| Its drops | loot table `fletcherstrestle:entities/garrison_golem` |
| Extra things to shoot | entity type tag `fletcherstrestle:garrison_targets` |
| Hostile mobs to leave alone | entity type tag `fletcherstrestle:garrison_ignores` |
| Range, scoped range, cone, home radius | server config `[garrison]` |

Emplacements shoot hostile mobs (`Enemy`) plus `garrison_targets`, minus
`garrison_ignores`, and mount any crossbow, modded ones included.

---

## Commands & debugging

All commands live under `/fletcherstrestle` (alias `/ft`) and require op
permission level 2. They're built for exactly this workflow — adding and
verifying materials without grinding the craft pipeline.

### Inspecting what loaded

| Command           | What it does                                                              |
|-------------------|--------------------------------------------------------------------------|
| `/ft materials`   | Lists every loaded def per registry (`bow_limb`, `arrow_head`, …) with counts and ids. |
| `/ft attachments` | Lists loaded `crossbow_attachment` ids.                                  |
| `/ft dump`        | Writes every id **and its translation key** to `fletcherstrestle_registry_dump.txt` in the game directory. |

If your new material's id doesn't show up in `/ft materials`, the JSON
failed to load — check the path, the codec fields, and the server log.
The `dump` file is the fastest way to scaffold a lang file: it lists the
exact `material.<pack>.<id>` / `attachment.<pack>.<id>` keys you need.

### Spawning test gear

Skip the shave → steam → fletch pipeline and the minigame; spawn finished
gear with the materials you want. Every material/attachment argument
tab-completes from the loaded registries, so you only ever pick valid ids
(including yours).

```
/ft give bow <limb> <riser> <string> [tuning 0–1]
/ft give arrow <head> <shaft> <fletching> [count 1–64]
/ft give crossbow <limb> <riser> <string> [attachment]
```

Examples:

```
/ft give arrow broadhead oak feather 16
/ft give crossbow oak iron spider scope
/ft give bow "mypack:steel" iron spider 0.9
```

Use the **bare path** for this mod's own materials (`oak`, `spider`,
`scope`) and a **quoted** `"namespace:path"` for materials from another
namespace — tab-completion fills in the right form automatically. (A
command argument can't contain an unquoted colon, hence the quotes.) An
unknown id still produces an item — it just renders as "unfinished".

> There is also `/ft archery xp|reset|max|info` (alias `/archery …`) for
> the marksmanship skill system — not material-related, but handy.

---

## Sanity checklist when adding a material

1. JSON at `data/<pack>/fletcherstrestle/<part>/<id>.json` ✓
2. Texture at `assets/<pack>/textures/entity/projectiles/<part>/<id>.png` ✓
3. Translation key `material.<pack>.<id>` in your lang file ✓
4. Ingredient item exists (single item, item list, or tag) ✓
5. Nothing else: the Fletching Table accepts any item your `ingredient` matches ✓

That's it.
