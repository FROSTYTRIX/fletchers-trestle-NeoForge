# Fletcher's Trestle — 2.3.1

By the Book. The Fletcher's Guide is now a real, illustrated **in-game handbook**
powered by Patchouli — and the jungle bow finally lives up to its "agility" name.

---

## 📖 The guidebook, powered by Patchouli

- **A real in-game manual** — the Fletcher's Guide now opens an illustrated
  **Patchouli** book instead of the old placeholder screen. Chapters for Getting
  Started, Woodworking Stations, Modular Equipment, and Companions, plus a full
  **Reference** section (materials, stats, enchantments, skills) — with inline
  crafting recipes and a live rotating **eagle render**.
- **Optional, never forced** — Patchouli is a soft dependency. Installed? You get
  the book and its recipe. Not installed? Both quietly disappear and nothing else
  about the mod changes.

## 🏹 Jungle "Agility", for real

- The **jungle limb** finally does what its tooltip always promised: you **walk at
  full speed while drawing**, with no movement penalty — and no FOV distortion.
  (The perk simply never existed in the code before; now it does.)

## 🦅 Eagle polish

- The eagle carries a **slight forward lean** in the guidebook render, reading as
  poised and soaring rather than standing bolt upright.
- Removed the leftover **[WIP]** tag from the **Eagle Spawn Egg** name (EN & FR).

---

# Fletcher's Trestle — 2.3.0

The Eagle Has Landed. The eagle companion finally gets its **real model** — a
commissioned, fully-animated bird — and with it, **natural spawning goes live**
across the mountains. Plus a nasty bow bug squashed.

---

## 🦅 The eagle, for real

- **A proper model** — the placeholder shape is gone, replaced by a commissioned
  Blockbench eagle: distinct head, hooked beak, layered wings, tail fan, and
  taloned legs.
- **Alive in the air** — a reworked flight animation: the wings open and *soar*
  when the eagle glides, and beat harder the faster it flies. Idle eagles now trace slow, lazy **circles**, facing the way they
  fly and gently drifting down instead of hovering frozen in place.
- **Natural spawning is ON** — eagles now spawn on high, sunlit mountain ridges
  (Stony/Jagged/Frozen Peaks, Snowy Slopes, Windswept Hills), and wild
  **eagle nests** generate in the world. Can still be disabled via the `eagles.natural_spawning` config.

## 🪵 Perches actually work now

- **Eagles land on their perch.** Previously a bound eagle would stall a couple
  blocks *above* its perch and never settle — the flight move-control refuses
  to close the last short distance onto a thin crossbar. The eagle now
  hand-flies the final descent and sits properly. Its idle soaring no longer
  fights the landing.

## 🎯 Bug fixes

- **The modular bow can now fire vanilla arrows and shoot in creative.** Firing
  any non-modular arrow (a vanilla arrow, or the infinite creative-mode arrow)
  crashed the shot on the server with a `getPickResult()` null — so the bow
  would draw but never release. Only modular arrows worked. Fixed; all ammo
  fires now, modular arrows still get their full assembly bonuses.

---

# Fletcher's Trestle — 2.2.1

The Singularity. A new **creative-only Black Hole arrow** collapses a piece of
the world into a gravitational set piece — and modular arrows can finally be
fired from dispensers.

---

## 🕳️ The Black Hole arrow

A new **creative-only** arrow head that turns its impact point into a black hole:

- **Gargantua, in Minecraft** — a fully procedural 3D set piece (no textures):
  an opaque event-horizon sphere wrapped in a warm, Doppler-shifted accretion
  disk, a lensed halo bending over the void, and a crisp photon ring, inspired
  by *Interstellar*.
- **It devours everything** — blocks, dropped items, mobs, and even block
  entities (chests, machines) are dragged in and consumed. Crossing the event
  horizon is instant death.
- **An expanding crater** — destruction starts tight at the impact and sweeps
  outward over its lifetime, frictionlessly hauling everything inward and
  carving a growing crater. Only exposed blocks erode; bedrock and other
  unbreakable blocks resist.
- Assembled at the Fletching Table from a **Barrier** (keeping it creative-only),
  and fully integrated: it appears in the `/ft` commands and JEI, and has a
  proper translated name.

## 🎯 Bug Fixes

- **Dispensers can now fire modular arrows**, carrying their full assembly
  (head, shaft, fletching and every behavior) — so you can even wire a
  black-hole arrow to a redstone trigger.

---

# Fletcher's Trestle — 2.2.0

The Workshop & Fieldcraft update. The **Steam Box** becomes a proper,
automatable machine, a new **Arrow Slit** lets you build disguised firing
positions, the crossbow gains a **Bayonet**, and an abandoned **Fletcher's
Camp** now generates in the world. Plus a full **`/ft` command suite** and
support for the popular tooltip mods.

---

## ♨️ The Steam Box, rebuilt

The Steam Box is now a real workstation you can plumb and automate:

- **Proper water tank** — water lives in a NeoForge fluid tank shown as a
  live, rising surface, and the tank is **water-only**, so pipes and pumps
  from other mods can fill it automatically (no more bucket-only).
- **Empty a bucket back out** — right-click with an empty bucket to take a
  bucket of water back.
- **Hopper & pipe input** — hoppers/pipes can feed Rough Limbs straight in
  (raw limbs only, so it won't clog).
- **Smart output** — finished limbs are **pushed into an adjacent chest or
  barrel** (and wait if it's full), left in place for an item **pipe** to
  pull, or popped out on top if nothing's attached. Hoppers and other steam
  boxes are ignored so it doesn't get confused.
- **Comparator output** — read the water level (0–15) with a comparator.
- **Water-gated steaming** — progress only advances while there's both heat
  *and* water; run dry and it pauses until you refill.

## 🧱 The Arrow Slit

A new directional cover block — a loophole you shoot through:

- **Arrows pass through the slit**, but mobs and melee can't easily get through.
- **Disguises as any full block** — right-click it with a block to make it
  wear that block's look (per-face textures, biome tint, transparency for
  glass/leaves, and light from glowstone, etc.); sneak + empty-hand to take
  the disguise back. It also takes on the worn block's hardness and blast
  resistance.

## 🗡️ Bayonet attachment

A new crossbow attachment: slot a sword onto the crossbow at the Crossbow
Bench to make it a melee weapon. Stabbing wears the crossbow down, and when
you pull the sword back off it comes out as worn as the crossbow has become
(it can never be repaired by re-installing). One attachment per crossbow, as
always.

## 🏕️ Fletcher's Camp

A small abandoned woodworking camp now generates in forests, taigas, plains
and meadows — the stations in the wild plus a supply chest of early
fletching gear.

## 🪶 Commands & tooltips

- **`/ft` command tree** (op-only): give modular bows/arrows/crossbows,
  inspect the material and attachment registries, dump them to a file, and
  manage archery progression (`/archery` still works too).
- **Jade / TheOneProbe / WTHIT** — the Steam Box shows its tank, contents
  and a live steaming/heat/water status in all three.

## 🧰 Other changes & fixes

- **Dipping Vat** — now water-only (won't accept lava/etc. from pipes); its
  break particles match its wood.
- Fixed the **bayonet not applying its attack stats** and the bench needing
  an extra click to unpack a crossbow.
- Fixed the **magazine draw animation** so it matches the slower reload.
- Fixed the **Eagle Whistle recipe** failing to load.
- Internal: arrow villager trades now pull from the data-driven registries;
  removed leftover deprecated code.

---

# Fletcher's Trestle — 2.1.0

The Marksmanship update. A new **Crossbow Bench** with data-driven
attachments, an **archery skill tree** you level up and spend points in,
a full **advancement tree**, and an **in-game guidebook** that replaces
the old wiki link.

---

## 🛠️ The Crossbow Bench

A new workstation that takes over all crossbow work from the Smithing
Table:

- **Assemble & disassemble** — drop a Modular Bow + Mechanical Trigger
  to build a crossbow (carrying over its limbs/riser/string); pull the
  trigger back out to revert it to a bow.
- **Fitting view** — place a finished crossbow and its trigger and
  attachment appear in their slots, so you can swap parts freely.
- **Live readout** — the bench shows what the weapon is made of and what
  is fitted.
- Crafted from planks, a tripwire hook and iron. The old Smithing Table
  bow → crossbow recipes have been **removed** in favour of the bench.

## 🔭 Crossbow Attachments (data-driven)

A new `crossbow_attachment` datapack registry — pack makers can add
their own attachments from JSON, just like materials. Two ship built-in:

- **Scope** (a spyglass) — aim-down-sights zoom on a loaded crossbow,
  toggled with a keybind (default **V**).
- **Magazine** — holds **3 bolts** for repeating fire (one per click
  until empty), in exchange for a **2× slower** draw. Crafted from iron
  and redstone.

Each crossbow takes one attachment, installed at the bench.

## 🎯 Archery Skills

Land hits to earn archery XP; every level grants a point to spend across
a three-branch **skill tree** (open with **K**, or from the guidebook):

- **Faster Draw** — down to 0.8× bow draw time.
- **Crit Chance** — up to 30% chance for a 1.5× damage arrow.
- **Steady Aim** — down to 0.7× spread, and a longer grace period before
  a flax string starts shaking your aim.

XP comes from hits, **headshots** (the top of any mob's hitbox — no
per-mob setup for now...), and kills; the practice dummy gives none, and XP is kept
on death. Points and ranks are server-validated and sync to your client.
Admins can use `/archery xp|reset|info` for testing.

## 🏆 Advancements

A new **"Fletcher's Trestle"** advancement tab guides you through the
mod: woodworking → bow → arrow, the crossbow bench → crossbow →
attachment, headshots and long shots, archery levels 5/10/20/30, and
taming an eagle.

## 📖 In-Game Guidebook

The **Fletcher's Guide** now opens a real in-game book instead of just
linking out:

- **Chapters → sub-chapters → pages** (e.g. Woodworking → Shaving Horse
  / Steam Box / Fletching Table) on a parchment layout.
- **Crafting recipes** shown inline, plus **assembly examples** for the
  bow, arrow, crossbow and glass-vial potion arrow.
- An **interactive skill-tree page** to spend points without leaving the
  book, and an **Open Wiki** button for the full online reference.
- Covers the eagle ecosystem too, including **Perch** and **Nest** logic.
- Now crafts from a **Book + Feather**, looks like a glinting book.

---

## 🏹 Bug fixes & changes

- **Bench & Fletching Table no longer eat your items** when the game
  closes with the GUI open. The bench persists its contents (and drops
  them when broken); the Fletching Table returns work items to your
  inventory on logout/quit.
- **Fixed crossbow stats that never applied** — the modular crossbow's
  string-velocity and riser-accuracy multipliers were computed but
  discarded; they now actually affect the shot.
- The **Dipping Vat** recipe is now an upside-down "pants" of planks
  with a bucket in the middle.
- **JEI** gains a Crossbow Bench category (assembly + attachments).

---

## 🌍 Localization

Full **French** coverage for everything new — the bench, attachments,
skill tree, advancements and the entire guidebook.
