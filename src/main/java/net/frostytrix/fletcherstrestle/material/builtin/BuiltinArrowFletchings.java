package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.material.ArrowFletchingDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.stats.ArrowFletchingStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-fletching material defs. Mirrors the legacy
 * {@code ModularArrowItem.FletchingStats} enum 1:1.
 *
 * <p>{@code rigid}, {@code serrated}, {@code bound}, and {@code vex} only
 * exist as inaccuracy multipliers in the legacy enum — their special
 * behaviors (homing for serrated, drop-on-hit for bound, phase for vex)
 * are pure {@code if-equals} branches in {@code ModularArrowEntity} that
 * will migrate to {@code effects} in Phase E.</p>
 */
public final class BuiltinArrowFletchings {
    private BuiltinArrowFletchings() {}

    public static final ResourceKey<ArrowFletchingDef> FEATHER  = key("feather");
    public static final ResourceKey<ArrowFletchingDef> RIGID    = key("rigid");
    public static final ResourceKey<ArrowFletchingDef> TRAILING = key("trailing");
    public static final ResourceKey<ArrowFletchingDef> SERRATED = key("serrated");
    public static final ResourceKey<ArrowFletchingDef> BOUND    = key("bound");
    public static final ResourceKey<ArrowFletchingDef> VEX      = key("vex");

    public static void bootstrap(BootstrapContext<ArrowFletchingDef> ctx) {
        // (key, ingredient, inaccuracyMult)
        register(ctx, FEATHER,  Ingredient.of(Items.FEATHER),                         1.00f);
        register(ctx, RIGID,    Ingredient.of(Items.FLINT),                           0.84f);
        register(ctx, TRAILING, Ingredient.of(Items.STRING),                          0.75f);
        register(ctx, SERRATED, Ingredient.of(Items.PHANTOM_MEMBRANE),                1.00f);
        register(ctx, BOUND,    Ingredient.of(Items.LEATHER),                         1.00f);
        register(ctx, VEX,      Ingredient.of(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE), 1.00f);
    }

    private static void register(BootstrapContext<ArrowFletchingDef> ctx,
                                 ResourceKey<ArrowFletchingDef> key,
                                 Ingredient ingredient, float inaccuracyMult) {
        ctx.register(key, new ArrowFletchingDef(
                ingredient,
                new ArrowFletchingStats(inaccuracyMult),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
    }

    private static ResourceKey<ArrowFletchingDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.ARROW_FLETCHING,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
