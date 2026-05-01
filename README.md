# Fletcher's Trestle

> **Minecraft 1.21.1 · NeoForge 21.1.227 · v1.1.0 · MIT License**

[![CurseForge](https://img.shields.io/curseforge/dt/1228552?logo=curseforge&label=CurseForge&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/fletchers-trestle)
[![GitHub](https://img.shields.io/github/v/release/FROSTYTRIX/fletchers-trestle-NeoForge?logo=github&label=GitHub)](https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Fletcher's Trestle** overhauls Minecraft's archery by replacing the basic crafting table bow with a deep, rewarding modular system.  
From the raw log to the perfect release, every step of the process is in your hands.

---

## ✨ Features

### 🏹 Modular Bow Crafting
Bows are no longer assembled in a crafting table — they are **built step by step** using dedicated tools and blocks. Each component impacts the final weapon's stats.

### 🪚 New Blocks & Tools
| Block / Item | Role |
|---|---|
| **Shaving Horse** | Strip logs into rough limbs using the Drawknife |
| **Steam Box** | Bend a rough limb into a pliable bow limb |
| **Drawknife** | Tool used on the Shaving Horse |
| **Fletching Table** *(modified)* | Final assembly + arrow crafting via the fletching minigame |

### 🎯 Fletching Minigame
Arrow and bow crafting on the Fletching Table is driven by an **interactive minigame** instead of a static recipe. Precision and timing affect the quality of the result.

### 🪶 Modular Arrows
Craft arrows from individual components: shaft, nock, fletching, and arrowhead. Mix and match materials to tune your ammunition.

### 🩸 Bleed Effect
A new status effect applied by certain arrowheads. Deals damage over time after impact.

### 🌾 Flax
A new crop. Flax fibres are used in string and fletching components, providing an early-game alternative to wool-based string.

### 🎒 Quiver + HUD
Carry multiple arrow types in a Quiver item. A dedicated HUD element displays the currently selected ammunition.

---

## 🔧 Crafting Pipeline

```
Oak Log
  └─► Shaving Horse + Drawknife
        └─► Rough Bow Limb
              └─► Steam Box
                    └─► Pliable Bow Limb
                          └─► Fletching Table (minigame)
                                └─► Assembled Bow
```

Arrows follow a parallel pipeline: grow Flax → harvest fibres → combine with a shaft material and arrowhead at the Fletching Table.

---

## ⚙️ Configuration

All options are exposed in `fletcherstrestle-common.toml` (located in your `config/` folder).

| Key | Default | Description |
|---|---|---|
| `minigameCursorSpeed` | `2.0` | Speed of the cursor in the fletching minigame |
| `minigamePunishMultiplier` | `1.5` | Penalty multiplier for missed clicks |
| `minigameMinScore` | `50` | Minimum score required to obtain a result |
| `quiverHudX` | `10` | X position of the Quiver HUD element |
| `quiverHudY` | `10` | Y position of the Quiver HUD element |

---

## 📦 Installation

1. Install **[NeoForge 21.1.227](https://neoforged.net/)** for Minecraft 1.21.1.
2. Download the latest `.jar` from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fletchers-trestle) or [GitHub Releases](https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge/releases).
3. Drop the `.jar` into your `mods/` folder.
4. Launch the game. Configuration is auto-generated on first run.

---

## 🛠️ Build from Source

```bash
git clone https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge.git
cd fletchers-trestle-NeoForge
./gradlew build
# Output: build/libs/fletcherstrestle-<version>.jar
```

Requires **Java 21** and an internet connection for Gradle to download NeoForge MDK dependencies.

---

## 🗺️ Roadmap

- [ ] **Modular Crossbow** — extend the modular system to crossbows with interchangeable prods, stocks, and triggers
- [ ] Additional arrowhead materials (bone, obsidian, flint tiers)
- [ ] Quiver skin system (dyeable)
- [ ] JEI / REI integration for all custom recipes
- [ ] Data-driven recipe support

---

## 🔗 Links

- 📖 [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/fletchers-trestle)
- 💻 [GitHub repository](https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge)
- 🐦 [@FROSTYTRIX_code on X/Twitter](https://twitter.com/FROSTYTRIX_code)

---

## 📜 License

This mod is released under the **MIT License**. See [LICENSE](LICENSE) for details.  
The NeoForge MDK template is licensed separately under [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
