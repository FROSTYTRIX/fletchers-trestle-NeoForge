# Fletcher's Trestle — 2.0.1

A small patch on top of 2.0.0: a launch-crash hotfix and a French
translation.

---

## 🏹 Bug fixes

- **Fixed a crash on launch** introduced with the eagle spawn system.
  The spawn-placement registration read the `eagles.natural_spawning`
  server-config value too early in mod loading (before configs are
  loaded), throwing `IllegalStateException: Cannot get config value
  before config is loaded` and aborting startup. The natural-spawning
  gate now runs at spawn-attempt time instead; the game boots normally
  and natural spawning stays off by default as intended.

---

## 🌍 Localization

- **French translation** (`fr_fr`) added — full coverage of items,
  blocks, the eagle entity, materials, enchantments, config screen,
  keybinds, and sound subtitles.
