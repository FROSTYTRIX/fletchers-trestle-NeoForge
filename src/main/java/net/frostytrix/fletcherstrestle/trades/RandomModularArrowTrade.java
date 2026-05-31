package net.frostytrix.fletcherstrestle.trades;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

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
        // Pull the parts from the data-driven material registries, so trades
        // automatically include any heads/shafts/fletchings a modpack adds.
        var registries = trader.level().registryAccess();
        String head = randomEntry(registries.registryOrThrow(ModMaterialRegistries.ARROW_HEAD), random);
        String shaft = randomEntry(registries.registryOrThrow(ModMaterialRegistries.ARROW_SHAFT), random);
        String fletching = randomEntry(registries.registryOrThrow(ModMaterialRegistries.ARROW_FLETCHING), random);

        ItemStack arrowStack = new ItemStack(ModItems.MODULAR_ARROW.get(), arrowCount);
        arrowStack.set(ModDataComponents.ARROW_ASSEMBLY.get(), new ArrowAssembly(head, shaft, fletching));

        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                arrowStack,
                maxUses,
                villagerXp,
                priceMultiplier
        );
    }

    /**
     * Picks a random entry id from a material registry. Built-ins are stored
     * as a bare path ("bodkin_point"); entries from other namespaces keep
     * their full id ("mypack:steel") — both are understood by the resolver.
     */
    private static <T> String randomEntry(Registry<T> registry, RandomSource random) {
        List<ResourceKey<T>> keys = new ArrayList<>(registry.registryKeySet());
        if (keys.isEmpty()) return "";
        ResourceLocation id = keys.get(random.nextInt(keys.size())).location();
        return id.getNamespace().equals(FletcherTrestle.MOD_ID) ? id.getPath() : id.toString();
    }
}
