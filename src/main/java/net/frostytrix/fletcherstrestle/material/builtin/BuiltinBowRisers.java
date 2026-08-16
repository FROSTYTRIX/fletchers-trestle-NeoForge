package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.BowRiserDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.effect.SetArrowFlagEffect;
import net.frostytrix.fletcherstrestle.material.stats.BowRiserStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/**
 * Built-in bow-riser material defs. The copper riser's "conductive"
 * trait: calls down lightning on hit during a thunderstorm: is
 * expressed as a {@code set_arrow_flag} effect that stamps
 * {@code fletcherstrestle:conductive} on the fired arrow; the arrow
 * entity's onHit handler reads that flag.
 */
public final class BuiltinBowRisers {
    private BuiltinBowRisers() {
    }

    public static final ResourceKey<BowRiserDef> WOOD = key("wood");
    public static final ResourceKey<BowRiserDef> IRON = key("iron");
    public static final ResourceKey<BowRiserDef> COPPER = key("copper");

    public static void bootstrap(BootstrapContext<BowRiserDef> ctx) {
        register(ctx, WOOD, ModItems.WOOD_RISER, 250, 1.0f, List.of());
        register(ctx, IRON, ModItems.IRON_RISER, 750, 0.2f, List.of());
        register(ctx, COPPER, ModItems.COPPER_RISER, 400, 1.0f,
                List.of(new SetArrowFlagEffect("fletcherstrestle:conductive", true)));
    }

    private static void register(BootstrapContext<BowRiserDef> ctx,
                                 ResourceKey<BowRiserDef> key,
                                 java.util.function.Supplier<? extends ItemLike> ingredient,
                                 int maxDurability, float inaccuracyMult,
                                 List<MaterialEffect> effects) {
        ctx.register(key, new BowRiserDef(
                Ingredient.of(ingredient.get()),
                new BowRiserStats(maxDurability, inaccuracyMult),
                Optional.empty(),
                effects
        ));
    }

    private static ResourceKey<BowRiserDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.BOW_RISER,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
