# Fletcher's Trestle: 2.6.0

Laminated. Two woods can now be built into a single **composite bow** that keeps
the powers of both. Three new **enchantments**, and the woodworking side of the
mod finally gets **advancements** of its own.

---

## 🪵 Composite bows

- **Build a bow from two different woods** to get a composite: crimson over
  warped, dark oak over birch, any pairing you like.
- **The stats are averaged, the powers are kept.** Draw time and damage are the
  average of the two woods, but the bow keeps every trait either wood has, and
  both woods' effects apply. A crimson and warped bow ignites its arrows and
  fires them without gravity.
- **Off by default.** Turn composites on with `composite_bows` under `[crafting]`
  in the server config. While it is off, two different limbs will not assemble.
- **Each limb shows its own wood**, so you can spot a composite without opening
  the tooltip. Composite crossbows too.
- **The Crossbow Bench keeps both woods**, so a composite bow makes a composite
  crossbow.

## ✨ Enchantments

- **Tinker's Mark** (max V): adds 2% tuning per level, up to 100%. Removing the
  enchantment puts the bow back to the tuning you earned at the table.
- **Quick Nock**: moves the quiver to the next loaded arrow type after every shot.
- **Follow Through** (max III): adds 2% damage per level for each consecutive hit,
  up to five. Missing resets the streak.

## 🏆 Advancements

- **Eleven new advancements** for the workshop and the decorative blocks, which
  had none: the Shaving Horse, Steam Box and Dipping Vat, growing flax and
  spinning it into string, your first quiver, weaving and dyeing linen, hanging a
  garland, and displaying a weapon on the rack.

## 🐛 Fixes

- **The quiver no longer goes dead.** Fixed a bug where the bow would stop shooting
  if no arrows were in the player's inventory and the current selected slot empty.
- **High-tension strings need a metal riser.** Fixed a bug where the guide said a
  metal riser was required but the Fletching Table let you build the bow anyway.
- **Crossbow tooltips were hiding half the bow.** Fixed a bug where a composite
  crossbow named only its upper wood, both on the tooltip and on the Crossbow
  Bench screen.
- **Mismatched limbs no longer eat a limb.** Fixed a bug where two different woods
  crafted happily and then silently threw the bottom limb's wood away.

## 🛠️ Commands

- **`/ft give bow` and `/ft give crossbow` take a `lower_limb`** to spawn a
  composite, and the crossbow now accepts a `tuning` like the bow does. Pass
  `none` as the attachment to reach the arguments after it.

## 📖 Guidebook

- New **Composite Bows** entry, linked from Modular Bows and Modular Crossbows.
- The crossbow entry now explains that **tuning carries over** from the bow it was
  built from, where it gives a faster reload.

---

# Fletcher's Trestle: 2.5.0

Flax gets a second life as **linen**, a dyeable textile block family. The
**Weapon Rack** finally gives a well-tuned bow somewhere to be admired, and
**garlands** put those coloured feathers to use as bunting.

---

## 🧵 Linen

- **A new block family in 17 colours**: undyed linen plus all 16 dyes, each with
  a **block, stairs, slab and carpet**. Every colour has its own texture rather
  than a tint, so they read as distinct fabrics.
- **Woven from Flax String** (4 → 1), then dyed 8-at-a-time. The dye recipe
  accepts *any* linen, so a colour you've gone off can simply be re-dyed.
- **It behaves like wool**: shears cut it fast, it carries the wool, stairs,
  slab and carpet tags, and it makes **beds** and **banners** just like wool.
- **The Shepherd trades it**, the way it already trades wool: buying plain
  linen, selling dyed linen by the colour, and selling carpets at Journeyman.
- **A second creative tab**: "Fletcher's Trestle: Decorations": keeps the
  linen family from burying the archery gear.

## 🏹 The Weapon Rack

- **A wall-mounted display for a bow or crossbow**, mounted like a torch. It
  renders the **actual weapon** you hang on it, materials and all, so a
  cherry-limbed bow looks different to a dark oak one. Vanilla bows and
  crossbows work too.
- Right-click to hang a weapon, right-click empty-handed to take it back.
  Breaking the wall behind it drops the rack and whatever it held.
- Crafted from three planks and two sticks.

## 🎀 Nails and garlands

- **Nails** drive into any block face, floor, wall or ceiling. Cheap to make and
  the anchor point for everything below.
- **Garlands** are woven from a string and **seven feathers**. The recipe reads
  what you put in the grid, so the feathers you choose decide the colours, and
  they are spread **proportionally** along the finished bunting: four red and
  three blue hangs roughly four-sevenths red, whether the span is two blocks or
  twelve.
- **String one up** by right-clicking a nail and then a second one, up to 12
  blocks away. While it's tied to the first nail the garland trails from your
  hand, the way a lead does, so you can see where it will fall.
- The cord **sags under its own weight**, hanging deeper over longer spans, and
  the pennants follow the curve. A nail holds up to **four** garlands, so you can
  chain them along a wall or fan them out from one point.
- Break either nail to take one down. It drops back to you and clears the link
  at the far end.

## 🐛 Fixes

- Modular weapons now use the same **item-frame display transform** as vanilla
  bows, so they no longer sit backwards relative to a vanilla bow wherever the
  fixed display context is used.

## 📖 Guidebook

- New **Linen**, **Weapon Rack** and **Nails & Garlands** entries.

---

# Fletcher's Trestle: 2.4.0

Trade Secrets. Villagers now deal in modular gear, flax is something you can
actually **find** out in the world, and two arrow bugs are dead.

---

## 🤝 Villager trades

- **The Fletcher sells modular weapons.** Its vanilla bow and crossbow trades are
  gone; replaced with **fully assembled modular bows and crossbows**, complete
  with randomised limbs, riser, and string just as the modular arrow had replaced the vanilla one.
  The Expert and Master tiers sell **enchanted** ones, using vanilla's own enchantment roll and pricing.
  - Parts come from the material registries, so a modpack's materials show up in
    trades automatically.
  - Tuning lands between 55% and 90% a bought weapon can be good, but a good run
    at the Fletching Table and a little bit of skill sill is better.
- **The Fletcher sells Flax String**, so you can buy a bowstring outright.
- **The Shepherd sells Flax**, and buys your surplus back.

> Villager trades are rolled when a villager takes its profession, so you'll need
> a freshly-professioned villager to see these.

## 🌾 Flax you can find

- Flax, Flax String, and Flax Seeds now generate in **village chests** (fletcher,
  shepherd, and the farming houses), plus **shipwrecks**, **pillager outposts**,
  and the **village temple**.
- A **fletcher's chest** is the jackpot: 85% chance for 2–5 Flax String usually enough
  to string a bow the moment you find it.

## ⚖️ Bowstring rebalance

- **Flax String → 0.85x** velocity (was 1.3x). It's farmable, renewable, and
  shaky on an overdraw now it's genuinely the budget option rather than a
  straight upgrade.
- **High Tension → 1.4x** velocity (was 1.8x), still 2 durability per shot.

## 🎯 Bug fixes

- **Fast arrows no longer swerve and teleport.** A High Tension shot flies past
  the speed vanilla's spawn packet can describe (it clamps each axis to 3.9),
  which *rotated* the velocity the client received so the arrow appeared to
  curve off course and then snap onto the target. Arrows now send their
  true velocity, so what you see is the shot you actually took. (Damage and
  range were always correct; only the visuals were wrong.)
- **Homing arrows no longer jitter.** The serrated fletching's homing ran on both
  sides and could pick *different* targets on client and server. It's now
  server-authoritative and always tracks the nearest target.

## 📖 Guidebook

- New entries for **the Fletching Table**, **the Dipping Vat**, and
  **Villager Trades**, and a rewritten **Flax Farming** entry covering every way
  to get flax.

---

# Fletcher's Trestle: 2.3.1

By the Book. The Fletcher's Guide is now a real, illustrated **in-game handbook**
powered by Patchouli and the jungle bow finally lives up to its "agility" name.

---

## 📖 The guidebook, powered by Patchouli

- **A real in-game manual**: the Fletcher's Guide now opens an illustrated
  **Patchouli** book instead of the old placeholder screen. Chapters for Getting
  Started, Woodworking Stations, Modular Equipment, and Companions, plus a full
  **Reference** section (materials, stats, enchantments, skills), with inline
  crafting recipes and a live rotating **eagle render**.
- **Optional, never forced**: Patchouli is a soft dependency. Installed? You get
  the book and its recipe. Not installed? Both quietly disappear and nothing else
  about the mod changes.

## 🏹 Jungle "Agility", for real

- The **jungle limb** finally does what its tooltip always promised: you **walk at
  full speed while drawing**, with no movement penalty, and no FOV distortion.
  (The perk simply never existed in the code before; now it does.)

## 🦅 Eagle polish

- The eagle carries a **slight forward lean** in the guidebook render, reading as
  poised and soaring rather than standing bolt upright.
- Removed the leftover **[WIP]** tag from the **Eagle Spawn Egg** name (EN & FR).

---

# Fletcher's Trestle: 2.3.0

The Eagle Has Landed. The eagle companion finally gets its **real model**: a
commissioned, fully-animated bird, and with it, **natural spawning goes live**
across the mountains. Plus a nasty bow bug squashed.

---

## 🦅 The eagle, for real

- **A proper model**: the placeholder shape is gone, replaced by a commissioned
  Blockbench eagle: distinct head, hooked beak, layered wings, tail fan, and
  taloned legs.
- **Alive in the air**: a reworked flight animation: the wings open and *soar*
  when the eagle glides, and beat harder the faster it flies. Idle eagles now trace slow, lazy **circles**, facing the way they
  fly and gently drifting down instead of hovering frozen in place.
- **Natural spawning is ON**: eagles now spawn on high, sunlit mountain ridges
  (Stony/Jagged/Frozen Peaks, Snowy Slopes, Windswept Hills), and wild
  **eagle nests** generate in the world. Can still be disabled via the `eagles.natural_spawning` config.

## 🪵 Perches actually work now

- **Eagles land on their perch.** Previously a bound eagle would stall a couple
  blocks *above* its perch and never settle: the flight move-control refuses
  to close the last short distance onto a thin crossbar. The eagle now
  hand-flies the final descent and sits properly. Its idle soaring no longer
  fights the landing.

## 🎯 Bug fixes

- **The modular bow can now fire vanilla arrows and shoot in creative.** Firing
  any non-modular arrow (a vanilla arrow, or the infinite creative-mode arrow)
  crashed the shot on the server with a `getPickResult()` null, so the bow
  would draw but never release. Only modular arrows worked. Fixed; all ammo
  fires now, modular arrows still get their full assembly bonuses.

---

# Fletcher's Trestle: 2.2.1

The Singularity. A new **creative-only Black Hole arrow** collapses a piece of
the world into a gravitational set piece, and modular arrows can finally be
fired from dispensers.

---

## 🕳️ The Black Hole arrow

A new **creative-only** arrow head that turns its impact point into a black hole:

- **Gargantua, in Minecraft**: a fully procedural 3D set piece (no textures):
  an opaque event-horizon sphere wrapped in a warm, Doppler-shifted accretion
  disk, a lensed halo bending over the void, and a crisp photon ring, inspired
  by *Interstellar*.
- **It devours everything**: blocks, dropped items, mobs, and even block
  entities (chests, machines) are dragged in and consumed. Crossing the event
  horizon is instant death.
- **An expanding crater**: destruction starts tight at the impact and sweeps
  outward over its lifetime, frictionlessly hauling everything inward and
  carving a growing crater. Only exposed blocks erode; bedrock and other
  unbreakable blocks resist.
- Assembled at the Fletching Table from a **Barrier** (keeping it creative-only),
  and fully integrated: it appears in the `/ft` commands and JEI, and has a
  proper translated name.

## 🎯 Bug Fixes

- **Dispensers can now fire modular arrows**, carrying their full assembly
  (head, shaft, fletching and every behavior), so you can even wire a
  black-hole arrow to a redstone trigger.

---

# Fletcher's Trestle: 2.2.0

The Workshop & Fieldcraft update. The **Steam Box** becomes a proper,
automatable machine, a new **Arrow Slit** lets you build disguised firing
positions, the crossbow gains a **Bayonet**, and an abandoned **Fletcher's
Camp** now generates in the world. Plus a full **`/ft` command suite** and
support for the popular tooltip mods.

---

## ♨️ The Steam Box, rebuilt

The Steam Box is now a real workstation you can plumb and automate:

- **Proper water tank**: water lives in a NeoForge fluid tank shown as a
  live, rising surface, and the tank is **water-only**, so pipes and pumps
  from other mods can fill it automatically (no more bucket-only).
- **Empty a bucket back out**: right-click with an empty bucket to take a
  bucket of water back.
- **Hopper & pipe input**: hoppers/pipes can feed Rough Limbs straight in
  (raw limbs only, so it won't clog).
- **Smart output**: finished limbs are **pushed into an adjacent chest or
  barrel** (and wait if it's full), left in place for an item **pipe** to
  pull, or popped out on top if nothing's attached. Hoppers and other steam
  boxes are ignored so it doesn't get confused.
- **Comparator output**: read the water level (0–15) with a comparator.
- **Water-gated steaming**: progress only advances while there's both heat
  *and* water; run dry and it pauses until you refill.

## 🧱 The Arrow Slit

A new directional cover block: a loophole you shoot through:

- **Arrows pass through the slit**, but mobs and melee can't easily get through.
- **Disguises as any full block**: right-click it with a block to make it
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
and meadows: the stations in the wild plus a supply chest of early
fletching gear.

## 🪶 Commands & tooltips

- **`/ft` command tree** (op-only): give modular bows/arrows/crossbows,
  inspect the material and attachment registries, dump them to a file, and
  manage archery progression (`/archery` still works too).
- **Jade / TheOneProbe / WTHIT**: the Steam Box shows its tank, contents
  and a live steaming/heat/water status in all three.

## 🧰 Other changes & fixes

- **Dipping Vat**: now water-only (won't accept lava/etc. from pipes); its
  break particles match its wood.
- Fixed the **bayonet not applying its attack stats** and the bench needing
  an extra click to unpack a crossbow.
- Fixed the **magazine draw animation** so it matches the slower reload.
- Fixed the **Eagle Whistle recipe** failing to load.
- Internal: arrow villager trades now pull from the data-driven registries;
  removed leftover deprecated code.

---

# Fletcher's Trestle: 2.1.0

The Marksmanship update. A new **Crossbow Bench** with data-driven
attachments, an **archery skill tree** you level up and spend points in,
a full **advancement tree**, and an **in-game guidebook** that replaces
the old wiki link.

---

## 🛠️ The Crossbow Bench

A new workstation that takes over all crossbow work from the Smithing
Table:

- **Assemble & disassemble**: drop a Modular Bow + Mechanical Trigger
  to build a crossbow (carrying over its limbs/riser/string); pull the
  trigger back out to revert it to a bow.
- **Fitting view**: place a finished crossbow and its trigger and
  attachment appear in their slots, so you can swap parts freely.
- **Live readout**: the bench shows what the weapon is made of and what
  is fitted.
- Crafted from planks, a tripwire hook and iron. The old Smithing Table
  bow → crossbow recipes have been **removed** in favour of the bench.

## 🔭 Crossbow Attachments (data-driven)

A new `crossbow_attachment` datapack registry: pack makers can add
their own attachments from JSON, just like materials. Two ship built-in:

- **Scope** (a spyglass): aim-down-sights zoom on a loaded crossbow,
  toggled with a keybind (default **V**).
- **Magazine**: holds **3 bolts** for repeating fire (one per click
  until empty), in exchange for a **2× slower** draw. Crafted from iron
  and redstone.

Each crossbow takes one attachment, installed at the bench.

## 🎯 Archery Skills

Land hits to earn archery XP; every level grants a point to spend across
a three-branch **skill tree** (open with **K**, or from the guidebook):

- **Faster Draw**: down to 0.8× bow draw time.
- **Crit Chance**: up to 30% chance for a 1.5× damage arrow.
- **Steady Aim**: down to 0.7× spread, and a longer grace period before
  a flax string starts shaking your aim.

XP comes from hits, **headshots** (the top of any mob's hitbox: no
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
- **Fixed crossbow stats that never applied**: the modular crossbow's
  string-velocity and riser-accuracy multipliers were computed but
  discarded; they now actually affect the shot.
- The **Dipping Vat** recipe is now an upside-down "pants" of planks
  with a bucket in the middle.
- **JEI** gains a Crossbow Bench category (assembly + attachments).

---

## 🌍 Localization

Full **French** coverage for everything new: the bench, attachments,
skill tree, advancements and the entire guidebook.
