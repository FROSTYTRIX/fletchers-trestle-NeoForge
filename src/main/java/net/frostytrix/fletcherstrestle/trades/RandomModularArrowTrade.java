package net.frostytrix.fletcherstrestle.trades;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularArrowItem; // Ensure this is imported!
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

public class RandomModularArrowTrade implements VillagerTrades.ItemListing {
    private final int emeraldCost;
    private final int arrowCount;
    private final int maxUses;
    private final int villagerXp;
    private final float priceMultiplier;

    public RandomModularArrowTrade(int emeraldCost, int arrowCount, int maxUses, int villagerXp) {
        this.emeraldCost = emeraldCost;
        this.arrowCount = arrowCount;
        this.maxUses = maxUses;
        this.villagerXp = villagerXp;
        this.priceMultiplier = 0.05F; // Vanilla standard
    }

    @Override
    public MerchantOffer getOffer(Entity trader, RandomSource random) {
        // 1. Fetch the absolute "Source of Truth" arrays directly from your Enums!
        ModularArrowItem.HeadStats[] heads = ModularArrowItem.HeadStats.values();
        ModularArrowItem.ShaftStats[] shafts = ModularArrowItem.ShaftStats.values();
        ModularArrowItem.FletchingStats[] fletchings = ModularArrowItem.FletchingStats.values();

        // 2. Pick a random Enum and convert its name to lowercase
        // (e.g., BODKIN_POINT automatically becomes "bodkin_point", DARK_OAK becomes "dark_oak")
        String head = heads[random.nextInt(heads.length)].name().toLowerCase();
        String shaft = shafts[random.nextInt(shafts.length)].name().toLowerCase();
        String fletching = fletchings[random.nextInt(fletchings.length)].name().toLowerCase();

        // 3. Construct the Modular Arrow Stack
        ItemStack arrowStack = new ItemStack(ModItems.MODULAR_ARROW.get(), arrowCount);
        arrowStack.set(ModDataComponents.ARROW_ASSEMBLY.get(), new ArrowAssembly(head, shaft, fletching));

        // 4. Return the new Trade Offer
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                arrowStack,
                maxUses,
                villagerXp,
                priceMultiplier
        );
    }
}