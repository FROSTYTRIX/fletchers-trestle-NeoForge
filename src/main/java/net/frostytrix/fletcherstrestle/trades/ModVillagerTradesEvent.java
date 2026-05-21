package net.frostytrix.fletcherstrestle.trades;

// TODO(port-26.1): Villager trade system fully rewritten in 26.1.
//
//   - net.minecraft.world.entity.npc.VillagerProfession moved to
//     net.minecraft.world.entity.npc.villager.VillagerProfession
//   - VillagerTrades.ItemListing no longer exists. Trades are now
//     ResourceKey<VillagerTrade> entries (data-driven), declared in
//     datapack JSON or registered via a Bootstrap context.
//   - net.neoforged.neoforge.event.village.VillagerTradesEvent removed.
//     There's no longer an event hook to mutate trade lists at runtime.
//
// To restore the Fletcher trades:
//   1. Define each desired trade as a data file in
//        data/fletcherstrestle/villager_trade/fletcher/1/*.json
//      (mimicking vanilla — see `data/minecraft/villager_trade/fletcher/`).
//   2. For the random-modular-arrow trade, define a custom VillagerTrade
//      subclass that produces a randomised ItemStack per pull.
//   3. Wire it up via a tag entry in
//        data/fletcherstrestle/tags/villager_trade/fletcher/level_1.json
//
// Until that's done, vanilla Fletcher trades work normally; the mod
// just adds nothing extra.
public final class ModVillagerTradesEvent {
    private ModVillagerTradesEvent() {}
}
