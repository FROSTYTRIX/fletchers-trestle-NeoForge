# 🦅 Fletcher's Trestle — Eagle System Roadmap

Tracks the next major additions to the eagle companion system.

---

## ✅ Already done

- Smooth flight (custom `travel()`, no gravity, dynamic settling)
- Fetch-and-return with 16-slot inventory and chained pickups
- Spyglass lock-on hunt with 60s timeout, 128-block range
- Sneak + right-click fetch-mode toggle *(retained as a close-range fallback)*
- Stuck-busy bug fix (state sanitization on save, cold-start return)
- **Phase A — Eagle Whistle** ✅ (right-click toggles fetch, sneak+RC recalls, right-click on eagle binds, sneak+RC on eagle unbinds)
- **Phase B — Improved Spawning** ✅ (custom `has_eagle` biome tag, weight 2, altitude/sky/light predicate)
- **Phase C — Perch Block** ✅ (claimable block, eagle binds to it, returns when owner is far/offline)
- **Phase D — Nesting System** ✅ (nest block, egg item, breeding ritual, 20-min hatch → auto-tamed eaglet)
- **Phase E — Natural nests** ✅ (worldgen Feature places nests on high sunlit ridges, seeds random eggs + wild patrolling eagles, replaces flat biome spawn rule)

---

## 🎯 PHASE A — Eagle Whistle Item

**Why first:** the current shift+right-click toggle is bad UX because the eagle is rarely near you when you need to stop it. A whistle works at range and adds recall, which we want anyway.

### Design

- New item: **Eagle Whistle**
- Crafted from iron nugget + bone + string
- Behaviors:
  - **Right-click in air**: toggles fetch mode on your tamed eagles within 64 blocks. Chat: `Eagles: fetch ON/OFF`
  - **Sneak + right-click in air**: recall — clears any hunt, paths each eagle back to the player
  - **Right-click on a specific eagle**: bind whistle to that eagle (stores UUID in the stack). Subsequent uses only affect the bound eagle.

### Pseudo code

```java
class EagleWhistleItem extends Item {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        UUID bound = readBoundUUID(stack);

        List<EagleEntity> targets = level.getEntitiesOfClass(
            EagleEntity.class, player.getBoundingBox().inflate(64),
            e -> e.isOwnedBy(player) && (bound == null || e.getUUID().equals(bound)));

        if (player.isShiftKeyDown()) {
            for (EagleEntity e : targets) {
                e.setHuntTarget(null);
                e.getNavigation().moveTo(player.getX(), player.getY()+1, player.getZ(), 1.4);
            }
            player.displayClientMessage("Eagles recalled.", true);
        } else {
            boolean newMode = targets.stream().findFirst()
                .map(e -> !e.isFetchModeEnabled()).orElse(true);
            for (EagleEntity e : targets) e.setFetchModeEnabled(newMode);
            player.displayClientMessage("Eagles: fetch " + (newMode ? "ON" : "OFF"), true);
        }
        return InteractionResult.SUCCESS;
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (target instanceof EagleEntity eagle && eagle.isOwnedBy(player)) {
            writeBoundUUID(stack, eagle.getUUID());
            player.displayClientMessage("Whistle bound to this eagle.", true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
```

### Assets needed

| Asset | Path | Notes |
|---|---|---|
| Item texture | `textures/item/eagle_whistle.png` | 16×16 |
| Item model | `models/item/eagle_whistle.json` | parent `item/generated` |
| Lang | `lang/en_us.json` | `item.fletcherstrestle.eagle_whistle = Eagle Whistle` |
| Recipe | `recipe/eagle_whistle.json` | iron nugget + bone + string |
| Sound *(optional)* | reuse `minecraft:block.note_block.flute` |

---

## 🌍 PHASE B — Improved Spawning

**Why second:** pure data files, no new models, big payoff for the "rare and meaningful" feel. Unblocks the wild-nest part of Phase D.

### Design

- Eagles spawn naturally only in:
  - Stony Peaks, Jagged Peaks, Frozen Peaks, Snowy Slopes, Windswept Hills
- Requirements:
  - `y > 80`
  - `canSeeSky(pos)`
  - Daylight (`brightness > 8`)
- Spawn weight: **2** (very rare — vanilla phantom is 50, wolf is 8)
- Pack size: 1–1 (solo birds)
- Spawn egg stays as the creative-mode entry point

### Pseudo code

**Biome modifier (data file):**
```json
// data/fletcherstrestle/neoforge/biome_modifier/add_eagle_spawns.json
{
  "type": "neoforge:add_spawns",
  "biomes": "#fletcherstrestle:has_eagle",
  "spawners": [{
    "type": "fletcherstrestle:eagle",
    "weight": 2,
    "minCount": 1,
    "maxCount": 1
  }]
}
```

**Biome tag:**
```json
// data/fletcherstrestle/tags/worldgen/biome/has_eagle.json
{ "values": [
  "minecraft:stony_peaks",
  "minecraft:jagged_peaks",
  "minecraft:frozen_peaks",
  "minecraft:snowy_slopes",
  "minecraft:windswept_hills"
]}
```

**Spawn predicate (Java):**
```java
public static boolean checkEagleSpawnRules(EntityType<EagleEntity> type,
        LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
    return pos.getY() > 80
        && level.canSeeSky(pos)
        && level.getRawBrightness(pos, 0) > 8;
}

@SubscribeEvent
static void onSpawnPlacement(RegisterSpawnPlacementsEvent event) {
    event.register(ModEntities.EAGLE.get(),
        SpawnPlacementTypes.NO_RESTRICTIONS,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        EagleEntity::checkEagleSpawnRules,
        Operation.REPLACE);
}
```

### Assets needed

- **No new textures or models** — pure data + a Java tweak
- 2 JSON files (biome modifier + tag) + spawn predicate registration

---

## 🪵 PHASE C — Perch Block

**Why third:** moderate scope, but unblocks the nest's claim-pattern in Phase D.

### Design

- New block: **Eagle Perch** — wood post with a horizontal branch
- Recipe: 1 stripped log + 1 stick + 1 rope
- Behaviors:
  - **Right-click empty hand** on unclaimed perch with an idle owned eagle nearby → claim. Perch BE stores eagle UUID; eagle stores `BlockPos perchPos`.
  - **Right-click** claimed perch → show owner + bound eagle name
  - **Sneak + right-click** claimed perch → unclaim
- AI: new `EaglePerchGoal`
  - Active when: tamed, owner > 20 blocks away OR offline, has assigned perch
  - Priority: between `FollowOwnerGoal` and `RandomFlyingGoal`
  - Eagle flies to perch position, lands on top, plays `STATE_PERCHED` idle pose

### Pseudo code

**Block + BE:**
```java
class EaglePerchBlock extends Block implements EntityBlock {
    public InteractionResult use(...) {
        var be = (EaglePerchBlockEntity) level.getBlockEntity(pos);
        if (player.isShiftKeyDown()) { be.unclaim(); return SUCCESS; }
        if (be.isUnclaimed()) {
            EagleEntity nearby = findNearestOwnedIdleEagle(player, 16);
            if (nearby != null) {
                be.claim(player.getUUID(), nearby.getUUID());
                nearby.setPerchPos(pos);
                return SUCCESS;
            }
        }
        // otherwise: show claim info
    }
}

class EaglePerchBlockEntity extends BlockEntity {
    UUID ownerUUID, eagleUUID;  // saved to NBT
}
```

**Goal:**
```java
class EaglePerchGoal extends Goal {
    public boolean canUse() {
        return eagle.isTame()
            && eagle.getPerchPos() != null
            && eagle.getEagleState() == STATE_IDLE
            && (ownerIsFar() || ownerIsAbsent())
            && perchStillExists();
    }

    public void tick() {
        if (distanceToPerchSqr() > 1.5) {
            navigateTo(perchPos.above());
        } else {
            eagle.setEagleState(STATE_PERCHED);
            eagle.setOrderedToSit(true);  // reuse sit animation
            stop();
        }
    }
}
```

### Assets needed

| Asset | Path | Notes |
|---|---|---|
| Block model | `models/block/eagle_perch.json` | post + crossbar |
| Item model | `models/item/eagle_perch.json` | parent of block model |
| Blockstate | `blockstates/eagle_perch.json` | 4 horizontal facings |
| Textures | `textures/block/eagle_perch_post.png`, `eagle_perch_top.png` | can re-use vanilla log |
| Loot table | `loot_table/blocks/eagle_perch.json` | drops self |
| Recipe | `recipe/eagle_perch.json` | log + stick + rope |
| Lang | `block.fletcherstrestle.eagle_perch = Eagle Perch` |
| Animation tweak | `EagleModel.animateIdle()` | tighter pose for `STATE_PERCHED` |

---

## 🪺 PHASE D — Nesting System

**Why last:** biggest scope. Builds directly on Phase C's claim pattern. Adds true breeding ecology with eggs, eaglets, and growth.

### Sub-phase D1 — Nest block

- New block: **Eagle Nest** — bowl of sticks
- BE stores: owner UUID (nullable for wild), `List<EggData> eggs` (max 3), `List<UUID> claimedEagles` (max 2 — breeding pair)
- Crafting: 4 sticks + 2 feathers (bowl pattern). Can also generate naturally in mountain biome trees (rare loot drop)
- Block state property: `egg_count = 0..3` for visual differentiation
- Right-click empty: show claim status, egg count, hatch progress in chat
- Sneak + right-click: unclaim

### Sub-phase D2 — Egg item

- New item: **Eagle Egg**, speckled brown
- Cannot be removed from nest without breaking it
- If nest broken with eggs inside, eggs drop as items but lose incubation progress

### Sub-phase D3 — Breeding behavior

- Trigger: two adult tamed eagles (same owner) near each other, both fed rabbit/fish, both have a claimed nest
- Both fly to the nest, "ritual" animation: orbit nest, then land beside it for ~3 sec
- Egg added to BE with `laidAtTick` + `hatchAtTick` (current + 20 minutes default)
- Love cooldown: 5 minutes per parent

### Sub-phase D4 — Egg → Eaglet

- BE ticks each egg. On `now >= hatchAtTick`, spawn an `EagleEntity` with `setBaby(true)`
- Inherits owner UUID from nest (auto-tamed)
- Uses scaled-down model (~60%) via `getScale()` override
- Reduced flight amplitude, follows nearest parent until adult
- Grows up normally (vanilla baby aging, accelerated with food)

### Pseudo code

**Block entity:**
```java
class EagleNestBlockEntity extends BlockEntity {
    @Nullable UUID ownerUUID;
    List<UUID> claimedEagles = new ArrayList<>();
    List<EggData> eggs = new ArrayList<>();

    record EggData(long laidAtTick, long hatchAtTick) {}

    public void tick() {
        long now = level.getGameTime();
        eggs.removeIf(egg -> {
            if (now >= egg.hatchAtTick()) {
                spawnEaglet();
                return true;
            }
            return false;
        });
        if (eggsChanged) updateEggCountBlockstate();
    }

    private void spawnEaglet() {
        EagleEntity eaglet = ModEntities.EAGLE.get().create(level);
        eaglet.setBaby(true);
        eaglet.setPos(worldPosition.getX()+0.5,
                      worldPosition.getY()+1,
                      worldPosition.getZ()+0.5);
        if (ownerUUID != null) {
            Player owner = level.getPlayerByUUID(ownerUUID);
            if (owner != null) eaglet.tame(owner);
        }
        level.addFreshEntity(eaglet);
    }
}
```

**Breeding goal:**
```java
class EagleBreedGoal extends Goal {
    public boolean canUse() {
        return eagle.isInLoveMode()
            && eagle.getNestPos() != null
            && partnerNearby() != null
            && nestHasEggSpace();
    }

    public void tick() {
        flyTo(nestPos.above());
        if (atNest()) {
            ritualTicks++;
            if (ritualTicks > 60) {
                nestBE.addEgg(new EggData(level.getGameTime(),
                                          level.getGameTime() + HATCH_TIME));
                eagle.setLoveCooldown(6000);
                stop();
            }
        }
    }
}
```

### Assets needed

**Nest block:**
| Asset | Path | Notes |
|---|---|---|
| Block models | `models/block/eagle_nest_0.json` … `_3.json` | 4 variants for egg count |
| Item model | `models/item/eagle_nest.json` | parent of `_0` variant |
| Blockstate | `blockstates/eagle_nest.json` | maps `egg_count` to variants |
| Textures | `eagle_nest_top.png`, `eagle_nest_side.png`, `eagle_nest_eggs.png` |
| Loot table | `loot_table/blocks/eagle_nest.json` | drops self + any eggs |
| Recipes | crafted + worldgen chance drop from spruce/oak leaves in mountains |
| Lang | `block.fletcherstrestle.eagle_nest = Eagle Nest` |

**Eagle egg:**
| Asset | Path | Notes |
|---|---|---|
| Texture | `textures/item/eagle_egg.png` | 16×16, speckled brown |
| Model | `models/item/eagle_egg.json` | `item/generated` |
| Lang | `item.fletcherstrestle.eagle_egg = Eagle Egg` |

**Eaglet:**
- **No new entity type** — baby variant of `EagleEntity`
- Optional separate texture: `textures/entity/eagle_baby.png` (fluffier/lighter)
- `EagleModel.getScale()` returns `0.6f` when baby
- Reduced wing flap amplitude in flight animation when baby

**Sounds:**
| Sound | Placeholder |
|---|---|
| `eagle.egg_hatch` | `minecraft:entity.turtle.egg_hatch` |
| `eagle.baby_ambient` | `minecraft:entity.parrot.ambient` (pitched up) |

---

## 🔮 PHASE E — Deferred ideas (not yet designed)

Mentioned in brainstorm but not in the immediate plan. Listed so they aren't lost:

| Idea | Scope | Key need |
|---|---|---|
| Eagle leveling / XP | Medium | XP field on entity, scales fetch range / hunt speed |
| Eagle equipment (saddle slot) | Medium | Vanilla horse-style armor slot |
| Hawk / Owl / Falcon variants | Large | Shared base class, separate entity types |
| Eagle-dropped arrows (aerial bombing) | Small | New goal: hover above target, drop carried item |
| Boss hunt assist | Large | Per-boss scripted weak-point reveals |
| Eagle inventory screen | Small | Vanilla container screen tied to the 16-slot inventory |

---

## 📋 Suggested implementation order

1. **Phase A (Whistle)** — fixes UX immediately, smallest scope
2. **Phase B (Spawning)** — pure data, no new assets, fast win
3. **Phase C (Perch)** — one new block model; sets up the claim pattern
4. **Phase D (Nesting)** — biggest lift; builds on Phase C
