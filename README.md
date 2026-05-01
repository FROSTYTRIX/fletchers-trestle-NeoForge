# Fletcher's Trestle

> **Fletcher's Trestle** overhauls Minecraft's archery by replacing the basic crafting table bow with a deep, rewarding modular system.  
> From the raw log to the perfect release, every step of the process is in your hands.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.227-orange)
![Version](https://img.shields.io/badge/version-1.1.0-blue)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## 🏹 Overview

Fletcher's Trestle transforms the vanilla fletching table into a fully functional crafting station for modular bows and arrows. Instead of a single shapeless recipe, you'll source, process, and assemble every part yourself — choosing materials that affect the final weapon's feel and power.

---

## ✨ Features

### Modular Bow Crafting
Build your bow part by part via the upgraded **Fletching Table** UI:

- **Limbs** — available in all vanilla wood types (Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Crimson, Warped). Each limb must first be roughed out as a `Rough Limb`, then bent into a `Pliable Limb` using the Steam Box.
- **Risers** — the handle of the bow. Available in Wood, Iron, and Copper — each with different properties.
- **Strings** — `Flax String` (crafted from grown flax) or `High Tension String` for more demanding builds.

### Modular Arrow Crafting
Arrows are also assembled from components, giving you control over their behavior.

### The Fletching Minigame
Crafting a bow isn't just clicking a button. A **skill-based minigame** runs when you assemble the bow, determining the final quality of the result. The better you play, the better your bow. Configurable via `fletcherstrestle-client.toml`.

### Quiver
Store and manage multiple arrow types in a **Quiver** item. A HUD element shows your current arrow count, with a configurable position.

### Bleed Effect
Certain arrow combinations can apply a **Bleed** status effect to targets, dealing damage over time.

### New Crops & Materials
- **Flax** — a new crop grown from `Flax Seeds`. Harvest it to produce `Flax`, which is spun into `Flax String`.

### New Workblocks
- **Steam Box** — used to bend `Rough Limbs` into `Pliable Limbs`, a required step before assembling a bow.
- **Shaving Horse** — the woodworking bench used to shape raw wood into bow parts.

---

## 🪵 Crafting Workflow

```
Raw Log
  └─► Drawknife + Shaving Horse ──► Rough Limb
                                          │
                                    Steam Box ──► Pliable Limb
                                                        │
                                  Fletching Table ◄─────┤◄─── Riser
                                        │          ◄─────────── String
                                        ▼
                                  [ Minigame ]
                                        │
                                        ▼
                                   Modular Bow
```

---

## ⚙️ Configuration

The mod exposes several options in `fletcherstrestle-client.toml`:

| Option | Default | Description |
|---|---|---|
| `cursor_speed` | `0.02` | Speed of the cursor in the fletching minigame |
| `punish_multiplier` | `3.0` | How harshly missing the sweet spot is penalized |
| `minimum_score` | `0.2` | Minimum quality score even if you miss completely |
| `quiver_hud_x` | `0` | Horizontal offset of the Quiver HUD (0 = centered) |
| `quiver_hud_y` | `15` | Vertical position of the Quiver HUD from top of screen |

---

## 📦 Installation

1. Install [NeoForge 21.1.227](https://neoforged.net/) for Minecraft **1.21.1**
2. Download the latest `.jar` from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fletchers-trestle)
3. Drop the `.jar` into your `mods/` folder
4. Launch the game

**No dependencies required.**

---

## 🛠️ Building from Source

```bash
git clone https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge.git
cd fletchers-trestle-NeoForge
./gradlew build
```

The compiled `.jar` will be in `build/libs/`.

---

## 🗺️ Roadmap

- [ ] Modular Crossbow
- [ ] More arrow effect types
- [ ] Additional riser materials
- [ ] Datapacks support for custom limb materials

---

## 📜 License

This project is licensed under the **MIT License**. See [`TEMPLATE_LICENSE.txt`](./TEMPLATE_LICENSE.txt) for details.

---

## 👤 Author

Made with ❤️ by **FROSTYTRIX**  
[GitHub](https://github.com/FROSTYTRIX) · [Twitter / X](https://twitter.com/FROSTYTRIX_code) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fletchers-trestle)
