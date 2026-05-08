package net.frostytrix.fletcherstrestle.trades;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModVillagerTradesEvent {
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        // We only want to modify the Fletcher!
        if (event.getType() == VillagerProfession.FLETCHER) {

            // Get the list of possible trades for Novice (Level 1)
            List<VillagerTrades.ItemListing> level1Trades = event.getTrades().get(1);

            // 1. Nuke everything
            level1Trades.clear();

            // Vanilla Trades
            level1Trades.add(new VillagerTrades.EmeraldForItems(Items.STICK, 32, 16, 2));

            level1Trades.add((trader, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1),               // Input 1: 1 Emerald
                    Optional.of(new ItemCost(Items.GRAVEL, 10)),  // Input 2: 10 Gravel
                    new ItemStack(Items.FLINT, 10),               // Output: 10 Flint
                    16,                                           // Max uses before locking
                    2,                                            // Villager XP gained
                    0.05F                                         // Price multiplier
            ));

            // Custom Trade : Modular Arrows
            level1Trades.add(new RandomModularArrowTrade(1, 12, 12, 1));
        }
    }
}
