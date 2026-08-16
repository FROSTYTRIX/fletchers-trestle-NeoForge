package net.frostytrix.fletcherstrestle.trades;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
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

            // Flax String — the fletcher's own trade. Buying a bowstring is the
            // reliable way to get one without farming, and it's thematically
            // theirs (string is a fletcher's stock-in-trade).
            event.getTrades().get(2).add((trader, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1),
                    Optional.empty(),
                    new ItemStack(ModItems.FLAX_STRING.get(), 4),
                    12, 3, 0.05F));

            // Vanilla bows/crossbows don't belong in this mod's fletcher — swap
            // all four for assembled modular weapons with randomised parts.
            // Vanilla sells: bow @2, crossbow @3, enchanted bow @4,
            // enchanted crossbow @5.
            // At each of those levels the weapon sale is the only listing of its
            // class (the other trade is an EmeraldForItems buy, and level 5's
            // tipped-arrow sale is its own class), so removing by class hits
            // exactly the vanilla weapon and nothing else.
            replaceListing(event, 2, VillagerTrades.ItemsForEmeralds.class,
                    new RandomModularBowTrade(ModItems.MODULAR_BOW, 2, 1, 5, false));
            replaceListing(event, 3, VillagerTrades.ItemsForEmeralds.class,
                    new RandomModularBowTrade(ModItems.MODULAR_CROSSBOW, 3, 1, 10, false));
            replaceListing(event, 4, VillagerTrades.EnchantedItemForEmeralds.class,
                    new RandomModularBowTrade(ModItems.MODULAR_BOW, 2, 3, 15, true));
            replaceListing(event, 5, VillagerTrades.EnchantedItemForEmeralds.class,
                    new RandomModularBowTrade(ModItems.MODULAR_CROSSBOW, 3, 3, 15, true));
        }

        // The shepherd works raw fibre, so they deal in unspun flax — selling it,
        // and buying it back off players who farm the stuff.
        if (event.getType() == VillagerProfession.SHEPHERD) {
            event.getTrades().get(1).add((trader, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 1),
                    Optional.empty(),
                    new ItemStack(ModItems.FLAX.get(), 6),
                    12, 2, 0.05F));

            event.getTrades().get(2).add((trader, random) -> new MerchantOffer(
                    new ItemCost(ModItems.FLAX.get(), 12),
                    Optional.empty(),
                    new ItemStack(Items.EMERALD, 1),
                    12, 3, 0.05F));
        }
    }

    /**
     * Drops every listing of {@code vanillaType} from the given trade level and
     * puts {@code replacement} in its place. Removing by class rather than by
     * index keeps this stable if another mod reorders the list.
     */
    private static void replaceListing(VillagerTradesEvent event, int level,
                                       Class<? extends VillagerTrades.ItemListing> vanillaType,
                                       VillagerTrades.ItemListing replacement) {
        List<VillagerTrades.ItemListing> trades = event.getTrades().get(level);
        if (trades == null) {
            return;
        }
        trades.removeIf(vanillaType::isInstance);
        trades.add(replacement);
    }
}
