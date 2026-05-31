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
