package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.ArrowShaftDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.stats.ArrowShaftStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-shaft material defs. Mirrors the legacy
 * {@code ModularArrowItem.ShaftStats} enum 1:1.
 *
 * <p>Special case: the {@code oak} shaft accepts both {@code rough_oak_limb}
 * and vanilla {@code stick} — matches {@code ModularArrowRecipe.getArrowShaft}.
 * The other ten shafts accept the corresponding {@code rough_X_limb}.</p>
 */
public final class BuiltinArrowShafts {
    private BuiltinArrowShafts() {}

    public static final ResourceKey<ArrowShaftDef> OAK       = key("oak");
    public static final ResourceKey<ArrowShaftDef> SPRUCE    = key("spruce");
    public static final ResourceKey<ArrowShaftDef> BIRCH     = key("birch");
    public static final ResourceKey<ArrowShaftDef> JUNGLE    = key("jungle");
    public static final ResourceKey<ArrowShaftDef> DARK_OAK  = key("dark_oak");
    public static final ResourceKey<ArrowShaftDef> ACACIA    = key("acacia");
    public static final ResourceKey<ArrowShaftDef> MANGROVE  = key("mangrove");
    public static final ResourceKey<ArrowShaftDef> CHERRY    = key("cherry");
    public static final ResourceKey<ArrowShaftDef> PALE_OAK  = key("pale_oak");
    public static final ResourceKey<ArrowShaftDef> CRIMSON   = key("crimson");
    public static final ResourceKey<ArrowShaftDef> WARPED    = key("warped");

    public static void bootstrap(BootstrapContext<ArrowShaftDef> ctx) {
        // Oak shaft is the multi-ingredient case (rough_oak_limb OR stick).
        ctx.register(OAK, new ArrowShaftDef(
                Ingredient.of(ModItems.ROUGH_OAK_LIMB.get(), Items.STICK),
                new ArrowShaftStats(1.0f, 1.0f),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
        // (key, item, velocityMult, gravityMult)
        register(ctx, SPRUCE,   ModItems.ROUGH_SPRUCE_LIMB,   1.0f, 1.1f);
        register(ctx, BIRCH,    ModItems.ROUGH_BIRCH_LIMB,    1.0f, 0.9f);
        register(ctx, JUNGLE,   ModItems.ROUGH_JUNGLE_LIMB,   1.0f, 1.0f);
        register(ctx, DARK_OAK, ModItems.ROUGH_DARK_OAK_LIMB, 1.0f, 1.0f);
        register(ctx, ACACIA,   ModItems.ROUGH_ACACIA_LIMB,   1.0f, 1.0f);
        register(ctx, MANGROVE, ModItems.ROUGH_MANGROVE_LIMB, 1.0f, 1.0f);
        register(ctx, CHERRY,   ModItems.ROUGH_CHERRY_LIMB,   1.0f, 1.0f);
        register(ctx, PALE_OAK, ModItems.ROUGH_PALE_OAK_LIMB, 1.0f, 1.0f);
        register(ctx, CRIMSON,  ModItems.ROUGH_CRIMSON_LIMB,  1.0f, 1.0f);
        register(ctx, WARPED,   ModItems.ROUGH_WARPED_LIMB,   1.0f, 1.0f);
    }

    private static void register(BootstrapContext<ArrowShaftDef> ctx,
                                 ResourceKey<ArrowShaftDef> key,
                                 java.util.function.Supplier<? extends ItemLike> ingredient,
                                 float velocityMult, float gravityMult) {
        ctx.register(key, new ArrowShaftDef(
                Ingredient.of(ingredient.get()),
                new ArrowShaftStats(velocityMult, gravityMult),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
    }

    private static ResourceKey<ArrowShaftDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.ARROW_SHAFT,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
