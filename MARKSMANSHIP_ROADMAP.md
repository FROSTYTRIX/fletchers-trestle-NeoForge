# 🎯 Fletcher's Trestle — Marksmanship & Progression Roadmap

The next batch of additions, following the eagle ecosystem. Four
features, scoped tight:

1. **Crossbow Attachment Slot** — install a scope or a magazine
   (data-driven from the start).
2. **Archery Skill & XP** — the player gets better, not just the gear.
3. **Advancement Tree** — milestone scaffolding (vanilla data, no deps).
4. **In-Game Guide** — a self-contained, rich data-driven guidebook
   (no Patchouli).

> **The Wilds (world & mobs) and the Auto-Crossbow Turret are parked**
> for now — kept in the parking lot at the bottom so the design isn't
> lost, but out of the active plan.

No code in this document — design, scope, data/asset needs, and the
open decisions to settle before implementation.

---

## Guiding principles

- **Reuse the data-driven pattern.** Crossbow attachments ship as JSON
  defs in a datapack registry, exactly like the existing bow/arrow
  materials — so they're modpack-extensible for free.
- **No new hard dependencies.** The guidebook is a self-contained custom
  screen; the advancement tree is vanilla advancement JSON; nothing pulls
  in Patchouli/JEI/etc. as required.
- **Config-gate each feature**, following the existing
  `FletcherConfig` SERVER/CLIENT split.
- **Translate as we go** — every new key lands in `en_us.json` and
  `fr_fr.json`.
- **JEI entries** for every new recipe (the integration already exists).

---

## PHASE 1 — Crossbow Attachment Slot

**Why first:** it establishes the attachment data model and the bench
that all crossbow work routes through, which the later phases reference.
Note this grew from "smallest" to **medium** scope once the bench took on
bow→crossbow assembly (see below).

### Design

- A crossbow stack gains an **attachment data component** storing the
  installed attachment id(s) (mirrors how `BOW_ASSEMBLY` already stores
  bow parts).
- **One universal slot to start** (decided). Forces a scope-vs-magazine
  choice rather than a power-stack. Restricting now is save-safe;
  loosening later isn't. Expansion path if it feels too tight: "one optic
  + one mechanism," or a higher-tier crossbow stock that grants a 2nd slot.
- Two attachment families ship as built-in JSON defs:
  - **Scope** — zoom + reduced sway/inaccuracy while aiming. Hooks into
    the crossbow's existing FOV-zoom logic (already in `ModClientEvents`).
    Tiers possible (lens vs spyglass-grade).
  - **Magazine** — holds N bolts for burst/repeating fire before a
    reload, traded against heavier reload time.
- Behavior/stats resolve from the installed attachment at aim/fire time.
- **Install via a dedicated bench (decided).** A new workstation block —
  the **Crossbow Bench** (id `crossbow_bench`) — handles **both**
  crossbow jobs:
  1. **Bow → Crossbow assembly** (Modular Bow + Tripwire Hook +
     Mechanical Trigger → Modular Crossbow). This **moves off the
     Smithing Table**, consolidating all crossbow work in one themed
     block with a purpose-built UI.
  2. **Attachment install / removal** into the single slot — reversible,
     so players can swap their one choice freely.
- **Migration (decided):** the Smithing Table bow→crossbow path is
  **removed** — the Crossbow Bench is the single source of truth. Update
  the wiki note, the advancement node, and the guide to match.

### New data / registries — DATA-DRIVEN FROM THE START (decided)

- A datapack registry **`fletcherstrestle:crossbow_attachment`**, one
  JSON per attachment at
  `data/<pack>/fletcherstrestle/crossbow_attachment/<id>.json`, mirroring
  the material-system shape. Draft schema:
  - `ingredient` — what item installs this attachment (vanilla
    `Ingredient` syntax: item / list / tag).
  - `type` — slot category (`optic`, `mechanism`, …) for future
    multi-slot rules.
  - `stats` — e.g. `zoom`, `sway_multiplier`, `inaccuracy_multiplier`,
    `reload_multiplier`, `magazine_size`.
  - `texture` (optional) — overlay/icon override, like materials.
  - `effects` (optional) — reuse the existing effect vocabulary where it
    makes sense (e.g. on-fire hooks).
- The two built-ins (`scope`, `magazine`) ship as these JSONs — they are
  the reference examples, identical to how materials work.
- Document it in `MODPACK_INTEGRATION.md` alongside the material slots.

### The bench: recipe type + JEI

- **Bow→Crossbow uses a custom recipe type** `fletcherstrestle:crossbow_assembly`,
  following the exact pattern the mod already uses for Fletching /
  Shaving / Steaming / Dipping (Recipe class + RecipeType + Serializer,
  registered in `ModRecipes`). Data-driven JSON so the assembly recipe is
  tweakable/pack-overridable.
- **Attachment install is registry-driven menu logic, not a vanilla
  recipe** — the crossbow input carries variable component data and the
  output is the same crossbow with a modified component, which doesn't
  fit a fixed-output recipe cleanly. The bench reads the
  `crossbow_attachment` registry: the def's `ingredient` decides what
  installs, and removal returns the item.
- **JEI** (integration already exists): a **Crossbow Assembly** category
  driven by the `crossbow_assembly` recipes, plus an **Attachments**
  category enumerated from the `crossbow_attachment` registry (mirrors
  how the modular materials surface).

### Assets needed

| Asset | Notes |
|---|---|
| Attachment items | scope + magazine textures/models |
| Attachment JSON defs | the two built-ins (reference examples) |
| Scope reticle overlay | drawn when scoped |
| **Crossbow Bench** | block model + blockstate + block entity + menu + screen + GUI texture + loot table + recipe |
| Crossbow-assembly recipe type | Recipe + RecipeType + Serializer; assembly recipe JSON |
| Remove Smithing Table recipe | delete the existing bow→crossbow smithing recipe + update advancement/wiki/guide |
| JEI categories | Crossbow Assembly + Attachments |
| Recipes, lang (en + fr) | standard |

### Still open

- Exact **attachment stat schema** field list (draft above) — proposing
  to lock it as drafted.

---

## PHASE 2 — Archery Skill & XP

**Why:** all power is currently gear-side; this gives the player
long-term growth that the advancement tree then rewards.

### Design

- A **per-player data attachment** stores archery XP + level.
  **XP is NOT lost on death** (decided).
- **XP sources:** landing hits, headshots, long-range shots, kills.
  **The practice dummy gives no XP** (decided — prevents AFK grinding).
- **Headshots are universal, no per-entity setup needed (decided
  approach):** a hit counts as a headshot if the arrow's impact Y lands
  in the top ~25–30% of the target's hitbox (above eye height). This
  generalizes the Heavy-Dummy's height-threshold logic to any
  `LivingEntity`.
- **Level rewards (scaling passives):** reduced sway, faster draw, crit
  chance, +quiver capacity, possibly recipe unlocks.
- **Skill tree (decided direction):** ship **linear passive bonuses
  first**; store the data so a light "pick-one-perk-per-tier" tree can be
  layered on later. Don't block the feature on the tree.
- **Readout:** a stats page in the guidebook (Phase 4) and/or a small
  toggleable HUD element.

### Config

Enable/disable, XP rates, max level. (No persist-on-death toggle — XP
never drops on death.)

### Assets needed

Minimal — level-up sound, HUD/screen elements, lang.

---

## PHASE 3 — Advancement Tree

**Why:** cheap (pure data, zero dependency) and it ties the active
features together into a visible progression.

### Design

- **Root:** "Pull Up a Trestle" (craft a drawknife / first rough limb).
- **Branches:**
  - *Woodworking* — shave → steam → assemble first bow.
  - *Arrows* — first modular arrow → glass-vial potion arrow.
  - *Crossbow* — assemble → install an attachment.
  - *Companions* — tame an eagle → fetch → breed.
  - *Marksmanship* — first headshot → 100-block hit → reach skill levels.
- A few **custom criterion triggers** for events vanilla can't catch
  (skill-level-reached, attachment-installed, long-range headshot). Still
  no external dependency — just small in-mod trigger classes.

### Assets needed

Advancement JSONs, icons (reuse existing item textures), lang, optional
background texture.

---

## PHASE 4 — In-Game Guide (no Patchouli)

**Why:** onboarding. The Fletcher's Guide today just opens the GitHub
wiki; a real in-game book keeps players in the game.

### Design

- A custom guidebook item opens a **custom `Screen`** — fully
  self-contained, no library dependency.
- **Content is data-driven:** entries defined as JSON inside the mod, so
  it's trivial to extend and even **datapack/modpack-extensible** (fits
  the mod's ethos). Structure mirrors the wiki: Getting Started,
  Woodworking, Modular Equipment, Companions, Reference.

#### Rich content entry types (decided: go rich, not plain text)

The guide should render, not just describe:

- **Recipe widgets for every recipe type** — vanilla crafting **and** the
  mod's custom types: Fletching (minigame), Shaving Horse, Steam Box,
  Dipping Vat, plus the modular weapon/arrow recipes. Show inputs →
  output with item icons. The JEI categories already model these, so the
  same data can drive the in-guide widgets.
- **Item & block showcase entries** — icon + display name + description +
  hover tooltip; clicking jumps to "how it's made / what it's used in."
- **Station how-to entries** — usage walkthroughs for the Shaving Horse /
  Steam Box / Fletching Table / Dipping Vat (multi-step diagrams).
- **Entity viewer** — a rotating render, e.g. the eagle. *Deferred to a
  future version (parked).*
- **Images / diagrams** for the crafting-pipeline flow.
- **Cross-links** between entries + a searchable index.
- **Auto-generated "Materials & Stats" page** built from the loaded
  material (and now attachment) registries, so it never goes stale as
  packs add content.
- Keep an "Open Wiki" button as a secondary link to the full online ref.

### Assets needed

GUI textures (frame, widgets, recipe slots), entry JSONs, lang. Book item
texture already exists.

### On jar-in-jar libraries (decided against, with reasoning)

Jar-in-Jar (JiJ) bundles a dependency *inside* the mod jar so users don't
install it separately — great for true utility libraries. But **JiJ-ing a
full guidebook *mod* (Patchouli, Modonomicon) is risky:** if a player also
installs it (common in packs) you get duplicate-mod / version conflicts,
and those mods aren't designed to be embedded (plus licensing). So:

- **Chosen path: roll our own data-driven guide `Screen`** — zero
  dependency, no conflict risk, self-contained, and matches the mod's
  data-driven ethos.
- *Fallback if scope balloons:* hard-depend on Patchouli/Modonomicon.
- If a JiJ-able library is ever reconsidered, confirm it's **explicitly
  published as JiJ-safe** (permissive license, meant to be embedded)
  first — most guidebook libs are not.

### Authoring approach — HYBRID (decided)

- **Auto-generate the reference pages** (Materials & Stats, Attachments,
  "all recipes for X") from the registries + RecipeManager. Because the
  content is data-driven, pack-added materials/attachments/recipes appear
  in the guide automatically — never stale, zero authoring.
- **Hand-write the teaching pages** — Getting Started, the
  shave→steam→fletch pipeline, station how-tos, the eagle guide. Prose
  the registries can't produce.
- **Recipe widgets are the glue** — an entry references a recipe id/tag
  and the screen renders it live from loaded recipes; same data that
  feeds JEI.

---

## 📋 Suggested implementation order

1. **Phase 1 — Crossbow attachments** (small; establishes the data model)
2. **Phase 2 — Skill & XP** (player-progression foundation)
3. **Phase 3 — Advancements** (cheap, data-only, ties things together)
4. **Phase 4 — Guidebook** (capstone; documents everything — the screen
   framework can be scaffolded earlier and filled in as features land)

---

## 🅿️ Parking lot (designed, not in the active plan)

Kept so the work isn't lost:

- **The Wilds — World & Mobs** *(parked)*: huntable game animals
  (deer/boar/pheasant → hide/sinew/antler/feathers), their drops as
  data-driven materials + biome-tied materials, rival Ranger mobs,
  structures (Fletcher's Camp / Ranger Tower / Eyrie Ruins), and a Bowyer
  villager profession. Biggest lift; would feed the eagle (prey) and the
  material system.
- **Auto-Crossbow Turret** *(parked)*: base-defense block holding a
  modular crossbow + ammo; aims/fires at hostiles with an owner
  whitelist; attachments affect range/fire-rate.
- **Guidebook entity viewer** *(parked)*: a rotating entity render
  (e.g. the eagle) — nice-to-have, not in v1.
- From earlier brainstorming: wind affecting arrow flight (pairs with
  Gale Force), more eagle variants / rideable Roc, REI/EMI support,
  Curios quiver slot, Create deployer compat, mastercraft quality tiers
  from the fletching minigame score.

---

## ✅ Resolved decisions

1. **Scope: only Phases 1–4 are active**; Wilds + Turret parked.
2. **Attachment slot count** → one universal slot to start; expand later.
3. **Attachments are data-driven from the start** → `crossbow_attachment`
   datapack registry; the two built-ins ship as JSON.
4. **Attachment install → the Crossbow Bench** (id `crossbow_bench`),
   which **also takes over bow→crossbow assembly — the Smithing Table
   path is removed** — via a custom `crossbow_assembly` recipe type, with
   JEI categories. Attachment install is registry-driven menu logic, not
   a vanilla recipe.
5. **XP on death** → never lost. **Dummy XP** → none.
6. **Headshots** → universal height-fraction check, no per-entity setup.
7. **Skill tree** → linear passives first; data shaped so a perk tree can
   layer on later.
8. **Guidebook** → custom data-driven `Screen`; rich (recipe widgets,
   item/block showcases, station how-tos, auto-generated material pages);
   no Patchouli, no JiJ'd guidebook mod.
9. **Guidebook authoring → hybrid** (hand-written teaching pages +
   auto-generated reference/recipe rendering).
10. **Entity viewer** → deferred to a future version.

## ❓ Still open

1. Attachment **stat schema** field list (draft in Phase 1 — proposing to
   lock as drafted).
