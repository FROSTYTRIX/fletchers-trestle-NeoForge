package net.frostytrix.fletcherstrestle.trades;

// 26.1: villager trade definitions moved from runtime-event code to a
// pure data-driven system. The mod's Fletcher trades now live in:
//
//   data/fletcherstrestle/villager_trade/fletcher/1/*.json
//     emerald_for_modular_arrows.json   — 4 emeralds → 8 modular arrows
//     emerald_for_flax_seeds.json       — 1 emerald  → 6 flax seeds
//
//   data/minecraft/tags/villager_trade/fletcher/level_1.json
//     adds the two trade IDs onto the vanilla novice-fletcher list
//     (no `replace: true`, so vanilla's stick/arrow/gravel-flint trades
//     stay).
//
// There's nothing left to do in Java — this file is kept only so old
// references in the codebase still resolve.
public final class ModVillagerTradesEvent {
    private ModVillagerTradesEvent() {}
}
