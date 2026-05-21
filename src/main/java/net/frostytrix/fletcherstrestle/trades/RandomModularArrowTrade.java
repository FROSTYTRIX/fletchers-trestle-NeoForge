package net.frostytrix.fletcherstrestle.trades;

// TODO(port-26.1): This class implemented VillagerTrades.ItemListing in
// 1.21.1 to generate a random modular-arrow trade for the Fletcher
// villager. In 26.1, VillagerTrades.ItemListing no longer exists —
// trades are ResourceKey<VillagerTrade> entries declared in datapack
// JSON, not Java classes. See ModVillagerTradesEvent for the full
// migration notes.
//
// The original generation logic is preserved below in a static helper
// so a future custom VillagerTrade subclass can reuse it:
//
//   import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
//   import net.frostytrix.fletcherstrestle.component.ModDataComponents;
//   import net.frostytrix.fletcherstrestle.item.ModItems;
//   import net.frostytrix.fletcherstrestle.item.custom.ModularArrowItem;
//   import net.minecraft.util.RandomSource;
//   import net.minecraft.world.item.ItemStack;
//
//   public static ItemStack rollRandomArrow(int count, RandomSource random) {
//       var heads      = ModularArrowItem.HeadStats.values();
//       var shafts     = ModularArrowItem.ShaftStats.values();
//       var fletchings = ModularArrowItem.FletchingStats.values();
//       String head      = heads[random.nextInt(heads.length)].name().toLowerCase();
//       String shaft     = shafts[random.nextInt(shafts.length)].name().toLowerCase();
//       String fletching = fletchings[random.nextInt(fletchings.length)].name().toLowerCase();
//       ItemStack stack = new ItemStack(ModItems.MODULAR_ARROW.get(), count);
//       stack.set(ModDataComponents.ARROW_ASSEMBLY.get(),
//                 new ArrowAssembly(head, shaft, fletching));
//       return stack;
//   }
public final class RandomModularArrowTrade {
    private RandomModularArrowTrade() {}
}
