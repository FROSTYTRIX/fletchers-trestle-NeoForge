# Fletcher's Trestle: 2.7.0

Well Armed. Skeletons and pillagers now carry **modular weapons** built from the
woods around them, a finished bow can be **restrung and retuned**, and maxing a
skill branch unlocks a **capstone**. The workshop finally makes some noise,
and the **Garrison** arrives as a first, work-in-progress look.

---

## 💀 Armed mobs

- **Every skeleton, stray, bogged and pillager now spawns with a modular bow or
  crossbow.** The basic one is oak limbs, a wooden riser and a spider silk string.
- **A third to two thirds of them carry something better,** depending on the local
  difficulty: at least one limb is made of a **signature wood**. Strays use
  spruce, bogged use mangrove, and skeletons and pillagers use the wood of the
  biome they spawn in: birch in a birch forest, jungle in a jungle. Where only oak
  grows, they pick another Overworld wood.
- **Risers and strings vary too.** Two in three risers are wood, the rest copper
  or iron. Strings are spider silk, flax or high tension, in that order of
  likelihood, and a high-tension string always comes with a metal riser.
- **Composites can turn up** while `composite_bows` is on.
- **`/summon` with NBT is left alone**: a mob given its gear by the command keeps
  exactly that gear.
- **Limb and riser traits work for them**: a stray's bow fires its arrows the way
  yours would. Enchantments the game gave the mob carry over to the new weapon.
- **Fully data-driven**: which mobs, how often and which woods live in two data
  maps, `mob_armory` and `native_woods`. Turn the whole thing off with
  `armed_mobs` under `[world]`.

## 🏰 The Garrison (work in progress)

*A first playable version: the models and textures are placeholders, and the
numbers and behaviour may still change.*

- **The Emplacement**: a swivel mount for a crossbow, modular or vanilla. Mount
  the crossbow, fit a quiver for ammunition, and point it at the field it should
  cover: it fires 45° either side of its facing, 16 blocks out, 32 with a Scope
  (all three in the server config).
- **The Bolt Warden**: a clockwork golem that crews emplacements. Build one from
  four stripped logs in a T and wind it up with a Mechanical Trigger, which it
  drops again if it's destroyed.
- **It guards its post**: it stays within 16 blocks of where it was built (also
  configurable), runs
  to whichever loaded post can see a hostile mob, reloads at the crossbow's own
  speed and fires. Magazines fire in bursts.
- **Hostile mobs only.** It never shoots players, villagers or animals, and holds
  fire while anyone stands in the line. Every shot wears the crossbow, the bolts
  can be picked back up, and its kills give no archery XP.
- **Redstone** holds a post's fire, and a **comparator** reads its quiver.
- **New advancement**: Wind It Up, for building your first Bolt Warden.

## 🔧 Bench work

- **Restring a bow or crossbow** at the Fletching Table: put the finished weapon
  in the riser slot, leave the limb slots empty, and add a string. It swaps the
  string and restores **half the durability**.
- **Retune it** by playing the minigame with the weapon in place. Retuning costs
  10% durability, unless you restring at the same time.
- **Everything else is kept**: enchantments, name, both woods of a composite and
  a crossbow's attachment.
- **Maker's stamp**: weapons now remember who built them, shown on the tooltip.
  Bought ones are signed by a village fletcher.

## 🎯 Capstones

- **Max a skill branch to unlock its capstone.** Each costs 10 points, and an
  archer can own **two of the three**.
  - **Snap Shot** (Draw): release just as you reach full draw for no spread and
    +10% damage.
  - **Called Shot** (Crit): headshots always crit.
  - **Dead Calm** (Aim): sneak and hold still for a second for no spread, no flax
    shake, and one more step of scope zoom.
- **Headshots follow the mob's shape.** On long, low mobs like cows, horses and
  spiders, the head is the front of the body, not the top.
- **Four new advancements**: archery levels 40 and 50, your first capstone, two
  capstones, and your first restring.

## 🏹 The quiver

- **It hangs on your back.** Turn it off with `quiver_on_back` in the client
  config.
- **The selected arrow sticks out of it**, fletching up, in your inventory and on
  your back. Coloured fletchings and tipped arrows show their colours.

## 🔊 Sounds

- **Bows creak as you draw** and click at full draw. **Each string has its own
  release sound.**
- **The workshop is alive**: the Steam Box hisses and steams while it works, the
  Shaving Horse throws shavings, and the Fletching Table plucks.
- **Hits sound off**: a ding on a headshot, a thunk on the Archery Target, and a
  bell on a bullseye.

## 🗝️ Trial Chambers

- **Vaults can hold modular bows and crossbows**, already tuned, plus books with
  the mod's enchantments. Ominous vaults roll better tuning and rarer woods.
- **Pale oak limbs** turn up in vaults and supply chests, and steam like any
  other wood. They had no source on 1.21.1.

## 🧩 For modpacks and addons

- **No more reserved ids.** The glass vial, resonance tip, weighted hook,
  trailing rope, black hole and vex behaviours are now effects anyone can put
  on their own parts: `splash_potion`, `resonance`, `grapple`, `deploy_rope`,
  `black_hole` and `phase_through_blocks`, each with its own numbers to tune.
  A pack that overrides one of those six parts' files should add the effect to
  its copy, or the part loses its trick.
- **New stat flags**: `photosynthetic` on limbs (what Photosynthesis checks,
  false for crimson and warped) and `overdraw_shake` on strings (the flax
  wobble).
- **The Fletching Table takes any part with a material file.** New parts no
  longer also need adding to the slot tags.
- **The guidebook's tables are live for arrow parts too**: heads, shafts and
  fletchings list a pack's own parts and traits.
- **Garland feathers** and their colours are an item data map,
  `garland_feather`, and garland strings an item tag.
- **The Garrison is data too**: block, item and entity tags for what a warden
  is built from, what winds it up and what it shoots, a loot table for its
  drops, and a `[garrison]` server config for range, cone and home radius.
- **Java addons** get two new effect hooks that can take over an arrow's
  impact entirely.
- The config screen now names every option, in English and French.

## 🐛 Fixes

- **Garlands no longer vanish.** Fixed a bug where a garland disappeared as soon
  as the nail it was first tied to went off screen.
- **The quiver bar fits the quiver.** Fixed a bug where switching arrows showed
  a nine-slot bar, whether the quiver had three slots or five.
- **Pillagers can use modular crossbows.** Fixed a bug where a pillager holding
  one never fired.
- **Traits work on vanilla arrows.** Fixed a bug where amphibious, punch and
  conductive did nothing when a modular bow fired a plain arrow.
- **Headshots land where the arrow does.** Fixed a bug where hits were measured
  from where the arrow was a tick earlier, which also threw off the Heavy Dummy's
  readout.
- **Photosynthesis checks the whole bow.** Fixed a bug where a bow with an iron
  riser, or a Nether wood as its lower limb, could still repair in sunlight.
- Fixed a bug where a leftover test recipe baked potatoes in the Steam Box.
- Fixed a bug where the stripped oak shaving recipe was misspelled.
- Fixed a bug where about thirty texts showed their raw translation key.

## ⚙️ Configuration

- `restring_repair` and `retune_cost` under `[crafting]`, `capstone_cost` and
  `max_capstones` under `[marksmanship]`, `armed_mobs` under `[world]`, the new
  `[garrison]` section, and `quiver_on_back` in the client config.

## 📖 Guidebook

- **The material tables are live**: limbs, risers, strings, arrow heads, shafts
  and fletchings are read from the game, so the numbers are always right and a
  modpack's materials show up.
- New **Out in the World** entry for armed mobs and Trial Chamber loot, and a
  **Garrison** entry.
- The Fletching Table, Modular Bows, Quiver, Archery Skills, Enchantments and
  Villager Trades entries cover the new features.

---

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
