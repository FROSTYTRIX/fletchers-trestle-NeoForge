# Porting to MC 26.1 / NeoForge 26.1

This branch (`26.1.2`) is a work-in-progress port of Fletcher's Trestle from
MC 1.21.1 (tagged `v1.6.0-mc1.21.1`) to MC 26.1.2 / NeoForge 26.1.

Source for the API changes:
https://neoforged.net/news/26.1release/

---

## What's done

| Area | Status | Notes |
|---|---|---|
| `gradle.properties` | ✅ | `minecraft_version` → `26.1.2`, `neo_version` → `26.1.0` (placeholder — fix to the actual published version), Parchment lines commented out |
| `build.gradle` | ✅ | Java toolchain bumped to **25**, Parchment block removed |
| `gradle-wrapper.properties` | ✅ | Already at Gradle 9.4.0 (≥ 9.1 required) |
| `neoforge.mods.toml` template | ✅ | Uses `${...}` substitution from `gradle.properties` — no direct edit needed |
| Mod version | ✅ | Bumped to `2.0.0` to signal the breaking version jump |
| TODO markers in screens | ✅ | Tagged in `ArcheryTargetScreen.java` and `FletchingScreen.java` |

## What still needs to be done

NeoForge 26.1 includes a sweeping refactor of foundational Minecraft
packages. A first `./gradlew compileJava` run against the real
26.1.2.63-beta SDK surfaced **100 errors across ~30 files**.

### Confirmed package moves (need find-and-replace + import updates)

| Old import | New import | Files affected (rough) |
|---|---|---|
| `net.minecraft.world.entity.projectile.AbstractArrow` | `net.minecraft.world.entity.projectile.arrow.AbstractArrow` | ~5 |
| `net.minecraft.world.entity.npc.VillagerTrades` | `net.minecraft.world.item.trading.VillagerTrades` | 1 (RandomModularArrowTrade) |
| `net.minecraft.world.entity.animal.horse.AbstractHorse` | TBD — package renamed/removed; check `entity/animal/horse` → `entity/animal/equine`? | 2 mixins |
| `net.minecraft.client.gui.GuiGraphics` | `net.minecraft.client.gui.GuiGraphicsExtractor` | Many (every renderer + screen) |
| `net.minecraft.Util` | `net.minecraft.util.Util` | a few |
| `net.minecraft.resources.ResourceLocation` | TBD — may have moved into `util` namespace | Many |
| `net.minecraft.world.InteractionResultHolder` | Removed or renamed — probably folded into `InteractionResult` | A few |
| `net.minecraft.world.item.ArmorItem` | TBD — armor system likely refactored | a few |
| `net.minecraft.world.item.ItemNameBlockItem` | TBD | a few |

### Other code-level changes
- `Screen#render` → `Screen#extractRenderState`
- `Screen#renderBackground` → `Screen#extractBackground`
- `ItemStackTemplate` for recipes that construct stacks at registry-load time
- `IFluidHandler`, `ItemStackHandler`, `FluidTank`, `FluidUtil` are **deprecated for removal** — migrate before they're gone in 26.2

### Practical workflow

Multi-session port. Status:

#### ✅ Phase 1 — Mechanical renames (done)
Applied via sed across 60 files. Compile dropped 100 → 200 errors (the
first crash masked many more).

| Old | New |
|---|---|
| `world.entity.projectile.AbstractArrow` | `.projectile.arrow.AbstractArrow` |
| `world.entity.npc.VillagerTrades` | `world.item.trading.VillagerTrades` |
| `world.entity.animal.horse.AbstractHorse` | `.animal.equine.AbstractHorse` |
| `client.gui.GuiGraphics` | `client.gui.GuiGraphicsExtractor` |
| `resources.ResourceLocation` | `resources.Identifier` |
| `world.entity.MobSpawnType` | `world.entity.EntitySpawnReason` |
| `world.InteractionResultHolder<ItemStack>` | `world.InteractionResult` (drop generic) |

`InteractionResultHolder.fail(stack)` / `.consume(stack)` / `.sidedSuccess(...)`
all replaced by the enum-style constants `InteractionResult.FAIL` / `.CONSUME` /
`.SUCCESS`. `Item.use(...)` return type changed from `InteractionResultHolder<ItemStack>`
to `InteractionResult`.

#### 🔴 Phase 2 — Rewritten subsystems (next session)
These classes are *removed*, not renamed. Each needs reimplementation:

- **Item-model overlay system** (`ItemProperties`, `BakedModel`, `BakedQuad`,
  `ItemOverrides`, `ItemLayerModel`, `IUnbakedGeometry`, `IGeometryLoader`,
  `IGeometryBakingContext`, `ConfiguredModel`, `ItemTransforms`, `Material`,
  `ModelState`, `RegisterGeometryLoaders`) — the bow/crossbow pull predicates
  and the modular arrow's custom baked model need rewriting against the new
  item-model API. **This is the biggest chunk of work.**
- **Datagen** (`ItemTagsProvider`, `BlockStateProvider`, `ItemModelProvider`,
  `ExistingFileHelper`, `IConditionBuilder`, `ConfiguredModel`) — every
  `datagen/*Provider.java` will need updates.
- **Villager system** (`VillagerProfession`, `VillagerTradesEvent`,
  `VillagerTrades.ItemListing`) — `RandomModularArrowTrade` +
  `ModVillagerTradesEvent` need rewrites.
- **`ItemInteractionResult`** folded into `InteractionResult.ItemContext`.
- **`ArmorItem`**, **`ItemNameBlockItem`** — folded into `Item` + data
  components. Affects a few datagen references only.
- **`net.minecraft.Util`** — moved to `net.minecraft.util.Util`. Trivial fix.

#### 🟡 Phase 3 — API-shape tweaks
- `Screen#render` → `Screen#extractRenderState`, `Screen#renderBackground`
  → `Screen#extractBackground`
- Recipe `ItemStack` → `ItemStackTemplate` where built at registry-load
- Deprecated capability migrations (`FluidTank`, `ItemStackHandler`, `FluidUtil`)

#### 🟢 Phase 4 — Runtime
`./gradlew runClient`, fix what crashes at runtime.

### Next-session prompt
> "Let's continue the 26.1 port — start phase 2 with the BakedModel system rewrite"

---

## Stubbed-out subsystems

Through phase 2 the following subsystems were **stubbed** (replaced with
empty placeholder classes + migration TODOs) so the rest of the mod can
compile. Each needs a real rewrite against the new 26.1 API:

| File | Reason | What's lost |
|---|---|---|
| `client/renderer/ModularArrowRenderer.java` | `ArrowRenderer<T,S>` 2-param generic + extract/submit pattern | Custom modular-arrow layer rendering (shaft/fletching/head + potion overlay) |
| `block/entity/renderer/DippingVatRenderer.java` | `BlockEntityRenderer<T,S>` 2-param generic | Custom dipping vat fluid + colour rendering |
| `block/entity/renderer/ShavingHorseRenderer.java` | Same | Custom shaving horse rendering |
| `entity/client/EagleRenderer.java` | `MobRenderer<T,S,M>` 3-param generic | Eagle uses default mob rendering — no custom model |
| `entity/client/HeavyDummyRenderer.java` | `LivingEntityRenderer<T,S,M>` 3-param generic | Heavy dummy uses default rendering |
| `client/model/ModularBakedModel.java` | `BakedModel`/`IUnbakedGeometry` etc. all removed; ItemModel API replaces them | Modular bow/crossbow texture overlays |
| `client/model/ModularUnbakedGeometry.java` | Same | — |
| `client/model/ModularModelLoader.java` | `RegisterGeometryLoaders` event gone | — |
| `trades/RandomModularArrowTrade.java` | `VillagerTrades.ItemListing` removed; trades are now `ResourceKey<VillagerTrade>` datapack entries | Custom Fletcher villager trade |
| `trades/ModVillagerTradesEvent.java` | `VillagerTradesEvent` removed | — |

All registrations of these classes in `ModClientEvents.java` are also
commented out with matching TODOs.

## Remaining compile errors

After the stubs and the mechanical work, **100 errors remain**, concentrated in:

| Files | Count | Subsystem | What needs to happen |
|---|---|---|---|
| Custom `Recipe<*>` implementations (Dipping, ModularArrow, Steaming, Shaving, ModularWeapon) | ~50 | Recipe API | New abstract methods (`recipeBookCategory`, `placementInfo`, `showNotification`, `group`), `assemble()` lost its `HolderLookup.Provider` param, `getSerializer`/`getType` have stricter generic bounds |
| Datagen providers (`Mod*Provider.java`) | ~20 | Datagen API | Provider classes restructured |
| JEI compat (`compat/jei/*.java`) | ~15 | JEI 29 API | JEI itself moved many classes |
| Misc | ~15 | Various | One-off fixes |

The Recipe rewrite alone is probably another full session. Two
honest options:

1. **Stub recipes too** — the mod compiles but you can't craft modular
   bows/arrows or use the steam box / dipping vat / shaving horse.
   Eagles + arrows still work as items via vanilla recipes.
2. **Keep porting properly** — fix each recipe class one at a time
   over 2–3 more sessions.

### Recommended approach when you next pick this up
Ecosystem maturity matters. 26.1 is brand new (beta). Wait 1–2 weeks
for someone to publish a comprehensive 1.21 → 26.1 migration guide and
the actual port will be much faster.

---

## Phase 2b — recipes (✅ done)

All 5 custom recipes (DippingRecipe, ModularArrowRecipe, ModularWeaponRecipe,
SteamingRecipe, ShavingHorseRecipe) ported to the new 26.1 API.

**The recipe migration pattern** (apply to any other Recipe<T> impls):
1. `RecipeSerializer` is now a **record** `(MapCodec<T>, StreamCodec<...>)`, not an interface.
   Replace inner `static class Serializer implements RecipeSerializer<T>` with:
   - Static `MapCodec<T> CODEC` and `StreamCodec<...> STREAM_CODEC` fields at top level
   - `public static RecipeSerializer<T> serializer() { return new RecipeSerializer<>(CODEC, STREAM_CODEC); }`
   - Update `ModRecipes` to use `RecipeName::serializer` instead of `Serializer::new`
2. Add 4 new abstract methods: `recipeBookCategory()`, `placementInfo()`,
   `showNotification()`, `group()`. For station recipes:
   `return RecipeBookCategories.CRAFTING_MISC` / `PlacementInfo.NOT_PLACEABLE` / `false` / `""`.
3. Drop the `HolderLookup.Provider` param from `assemble()`.
4. Update `getSerializer()`/`getType()` to use bounded generics
   `<? extends Recipe<TInputType>>`.
5. Remove `canCraftInDimensions()` and `getResultItem()`.
6. `Ingredient.CODEC_NONEMPTY` → `Ingredient.CODEC`.
7. `ItemStack.STRICT_CODEC` → `ItemStack.CODEC`.
8. `CompoundTag.getString()` returns `Optional<String>` — add `.orElse("")`.
9. `Registry.getHolder(Identifier)` → `Registry.get(Identifier)` (also returns `Optional`).

## Phase 2c — partial: AbstractArrow API changes

AT updated for the new `world.entity.projectile.arrow.AbstractArrow` package:
- `inGround` field exposed
- `setPierceLevel(B)V` exposed
- `baseDamage` field exposed (replaces removed `getBaseDamage()`)

Sed renames applied across codebase:
- `.getBaseDamage()` → `.baseDamage`
- `MobEffects.MOVEMENT_SLOWDOWN` → `MobEffects.SLOWNESS`
- `SoundEvents.LEASH_KNOT_PLACE` → `SoundEvents.WOOL_PLACE`
- `.level().isClientSide` (field) → `.level().isClientSide()` (method)
- `getPersistentData().getBoolean("...")` → `...getBoolean("...").orElse(false)`

### What's still broken in ModularArrowEntity (~6 specific call sites)
- `spawnAtLocation(ItemStack)` → now `spawnAtLocation(ServerLevel, ItemLike)`
- `EntityType.LIGHTNING_BOLT.create(Level)` → new signature
  `create(ServerLevel, Consumer<T>, BlockPos, EntitySpawnReason, boolean, boolean)`
- `lightning.moveTo(Vec3)` → only `moveTo(double, double, double, float, float)` exists
- `this.inGround` access from within ModularArrowEntity (subclass) — should
  work via AT but isn't yet, may need a clean rebuild to pick up

## Next session

Pick whichever feels right:
- **`ModularArrowEntity` finish** — ~6 specific call-site fixes; gameplay-critical
- **JEI compat layer** (~24 errors across `compat/jei/*`) — broken-but-not-blocking
- **Datagen providers** (~15 errors) — only runs at `runData`, doesn't block runtime
- **`ModEvents` enchantment events** (~11 errors) — affects custom enchantments

Recipe migration pattern in this doc above is the best reference for
applying the same approach to any remaining Recipe-like subsystems.

### 2. `Screen` method renames

Per the changelog:
- `Screen#render` → `Screen#extractRenderState`
- `Screen#renderBackground` → `Screen#extractBackground`

Affected files (marked with `TODO(port-26.1)`):
- `src/main/java/net/frostytrix/fletcherstrestle/menu/ArcheryTargetScreen.java`
- `src/main/java/net/frostytrix/fletcherstrestle/menu/FletchingScreen.java`
- `src/main/java/net/frostytrix/fletcherstrestle/menu/QuiverScreen.java` (no explicit override — `renderBg` only)

Plus the `renderBg` (lowercase 'g') method might have changed. Verify
signature against `AbstractContainerScreen` in 26.1.

### 3. `ItemStackTemplate` for recipes

Per the changelog: "creating new ItemStacks now requires loaded registries.
Developers must use `ItemStackTemplate` for representing stacks in recipes
and data files before registries load."

Candidate places to audit:
- `src/main/java/net/frostytrix/fletcherstrestle/recipe/DippingRecipe.java`
- `src/main/java/net/frostytrix/fletcherstrestle/recipe/ModularArrowRecipe.java`
- `src/main/java/net/frostytrix/fletcherstrestle/recipe/SteamingRecipe.java`
- `src/main/java/net/frostytrix/fletcherstrestle/recipe/ShavingHorseRecipe.java`
- `src/main/java/net/frostytrix/fletcherstrestle/recipe/ModularWeaponRecipe.java`

Likely impact: their `output` / `result` `ItemStack` fields may need to
become `ItemStackTemplate`, and their `Codec`s updated to the new template
codec. Recipe matching/assembling should still work the same way after
construction.

### 4. `ChunkPos` API

- `new ChunkPos(blockPos)` → `ChunkPos.containing(blockPos)`
- `new ChunkPos(packedLong)` → `ChunkPos.unpack()`
- `chunkPos.asLong()` → `chunkPos.pack()`

Quick check (run from project root):
```bash
grep -rn "new ChunkPos\|\.asLong()" src/main/java
```

Currently no matches in our codebase, but worth re-running after any
significant refactor.

### 5. JEI

`gradle.properties` still pins `jei_version=19.12.0.134` which is the
1.21.1-compatible version. Look up the JEI build for MC 26.1 on
https://www.curseforge.com/minecraft/mc-mods/jei/files and bump.

If JEI hasn't updated yet, you can either:
- Drop JEI dependency temporarily (comment out the `dependencies { }` lines
  in `build.gradle` and the imports in `compat/jei/`)
- Wait for JEI to ship a 26.1 build

### 6. Java 25 toolchain

You'll need a JDK 25 installation. Install via:
- `sdk install java 25.0.1-tem` (sdkman)
- Or download from https://adoptium.net/

Set in IntelliJ: Project Structure → SDK → 25.

---

## Workflow from here

```bash
# Switch to this branch
git checkout 26.1.2

# Try to build, see what fails
./gradlew --refresh-dependencies compileJava

# Fix what comes up, commit incrementally
git add -p
git commit -m "Fix Screen renames for 26.1"
git push

# When green, do a runtime smoke test
./gradlew runClient

# Once stable, bump to v2.0.0 and tag
git tag -a v2.0.0-mc26.1.2 -m "Port to MC 26.1.2"
git push --tags
```

## Backporting future bugfixes

For any fix you want on BOTH versions:
```bash
# Fix on 26.1.2 first (the active branch)
git checkout 26.1.2
# ... make + commit the fix ...

# Cherry-pick to the 1.21.1 line
git checkout main          # main is still 1.21.1
git cherry-pick <commit-hash>
git push origin main
```

Or if `main` has already moved on to 26.1, use the snapshot tag's branch:
```bash
git checkout -b backport-fix v1.6.0-mc1.21.1
git cherry-pick <commit-hash>
# Build a one-off backport release from this branch
```
