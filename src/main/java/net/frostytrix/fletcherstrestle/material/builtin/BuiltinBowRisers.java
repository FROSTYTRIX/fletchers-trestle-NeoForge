package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.BowRiserDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.stats.BowRiserStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in bow-riser material defs. Mirrors the legacy
 * {@code ModularBowItem.RiserStats} enum 1:1.
 */
public final class BuiltinBowRisers {
    private BuiltinBowRisers() {}

    public static final ResourceKey<BowRiserDef> WOOD   = key("wood");
    public static final ResourceKey<BowRiserDef> IRON   = key("iron");
    public static final ResourceKey<BowRiserDef> COPPER = key("copper");

    public static void bootstrap(BootstrapContext<BowRiserDef> ctx) {
        // (key, item, maxDurability, inaccuracyMult)
        register(ctx, WOOD,   ModItems.WOOD_RISER,   250, 1.0f);
        // Iron riser is the laser-precise one (0.2 = ~5x baseline accuracy).
        register(ctx, IRON,   ModItems.IRON_RISER,   750, 0.2f);
        register(ctx, COPPER, ModItems.COPPER_RISER, 400, 1.0f);
    }

    private static void register(BootstrapContext<BowRiserDef> ctx,
                                 ResourceKey<BowRiserDef> key,
                                 java.util.function.Supplier<? extends ItemLike> ingredient,
                                 int maxDurability, float inaccuracyMult) {
        ctx.register(key, new BowRiserDef(
                Ingredient.of(ingredient.get()),
                new BowRiserStats(maxDurability, inaccuracyMult),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
    }

    private static ResourceKey<BowRiserDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.BOW_RISER,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
