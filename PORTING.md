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

This isn't a quick port — realistically 2–4 sessions of incremental work.
Suggested order to minimise context-switching:

1. **Phase 1 — Mechanical import renames**: AbstractArrow, VillagerTrades,
   GuiGraphics, Util, ResourceLocation. ~30 minutes once we know all the
   new paths. Mostly `Edit` + `Bash` rename loops.
2. **Phase 2 — Screen API**: rename `render` → `extractRenderState` etc.
   in the three `AbstractContainerScreen` subclasses. Verify signatures.
3. **Phase 3 — Recipes**: convert any `ItemStack` fields in recipe
   classes to `ItemStackTemplate` where required by the new codec.
4. **Phase 4 — Capabilities / deprecated APIs**: migrate FluidTank,
   ItemStackHandler etc. to their non-deprecated replacements.
5. **Phase 5 — Runtime test**: `./gradlew runClient`, fix anything that
   compiles but crashes at runtime.

Want to tackle this incrementally? Just say "let's do phase 1" in a
fresh session and I'll work through the imports systematically.

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
