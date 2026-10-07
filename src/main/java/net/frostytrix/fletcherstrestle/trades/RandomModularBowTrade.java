package net.frostytrix.fletcherstrestle.trades;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Sells a fully-assembled modular bow or crossbow with randomised parts,
 * the weapon-side counterpart to {@link RandomModularArrowTrade}. Replaces the
 * fletcher's vanilla bow/crossbow trades so villagers deal in this mod's gear.
 *
 * <p>Parts are drawn from the datapack material registries, so a modpack's
 * limbs/risers/strings show up in trades automatically. Tuning is randomised in
 * a respectable band: a villager-made bow is decent but not perfectly tuned,
 * leaving room for a player's own Fletching Table work to beat it.</p>
 *
 * <p>When {@code enchanted}, the stack is enchanted exactly the way vanilla
 * does it for traded gear (same level roll, same {@code ON_TRADED_EQUIPMENT}
 * tag, and the same emerald-cost scaling).</p>
 */
public class RandomModularBowTrade implements VillagerTrades.ItemListing {
    private final Supplier<? extends Item> weapon;
    private final int baseEmeraldCost;
    private final int maxUses;
    private final int villagerXp;
    private final boolean enchanted;
    private final float priceMultiplier;

    public RandomModularBowTrade(Supplier<? extends Item> weapon, int baseEmeraldCost,
                                 int maxUses, int villagerXp, boolean enchanted) {
        this.weapon = weapon;
        this.baseEmeraldCost = baseEmeraldCost;
        this.maxUses = maxUses;
        this.villagerXp = villagerXp;
        this.enchanted = enchanted;
        this.priceMultiplier = 0.05F; // Vanilla standard
    }

    @Override
    public MerchantOffer getOffer(Entity trader, RandomSource random) {
        RegistryAccess registries = trader.level().registryAccess();

        // 0.55 to 0.90: a competent village-made weapon, but a well-played
        // Fletching Table minigame still beats it.
        ItemStack stack = new ItemStack(weapon.get());
        stack.set(ModDataComponents.BOW_ASSEMBLY.get(), net.frostytrix.fletcherstrestle.material.RandomWeapon
                .assemble(registries, random, List.of(), 0.55f, 0.90f));
        stack.set(ModDataComponents.CRAFTED_BY.get(), net.frostytrix.fletcherstrestle.component.MakersStamp.VILLAGE);

        int cost = baseEmeraldCost;
        if (enchanted) {
            // Mirrors vanilla EnchantedItemForEmeralds: roll an enchant level,
            // enchant from the traded-equipment tag, and scale the price by it.
            int level = 5 + random.nextInt(15);
            Optional<HolderSet.Named<Enchantment>> tag = registries
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getTag(EnchantmentTags.ON_TRADED_EQUIPMENT);
            stack = EnchantmentHelper.enchantItem(random, stack, level, registries, tag);
            cost = Math.min(baseEmeraldCost + level, 64);
        }

        return new MerchantOffer(
                new ItemCost(Items.EMERALD, cost),
                stack,
                maxUses,
                villagerXp,
                priceMultiplier
        );
    }
}
