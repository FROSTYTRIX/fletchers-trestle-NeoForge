package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.material.ArrowFletchingDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.effect.DropSelfOnHitEffect;
import net.frostytrix.fletcherstrestle.material.stats.ArrowFletchingStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-fletching material defs.
 *
 * <p>{@code bound} now carries its 25% drop-on-hit chance as an effect.
 * {@code serrated} (homing) and {@code vex} (block-phase) need tick-time
 * lifecycle hooks — they stay hardcoded in {@code ModularArrowEntity}
 * until Phase E expands the effect vocabulary with tick-time variants.</p>
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
        register(ctx, FEATHER,  Ingredient.of(Items.FEATHER),                          1.00f, List.of());
        register(ctx, RIGID,    Ingredient.of(Items.FLINT),                            0.84f, List.of());
        register(ctx, TRAILING, Ingredient.of(Items.STRING),                           0.75f, List.of());
        register(ctx, SERRATED, Ingredient.of(Items.PHANTOM_MEMBRANE),                 1.00f, List.of());
        register(ctx, BOUND,    Ingredient.of(Items.LEATHER),                          1.00f,
                List.of(new DropSelfOnHitEffect(0.25f)));
        register(ctx, VEX,      Ingredient.of(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE), 1.00f, List.of());
    }

    private static void register(BootstrapContext<ArrowFletchingDef> ctx,
                                 ResourceKey<ArrowFletchingDef> key,
                                 Ingredient ingredient, float inaccuracyMult,
                                 List<MaterialEffect> effects) {
        ctx.register(key, new ArrowFletchingDef(
                ingredient,
                new ArrowFletchingStats(inaccuracyMult),
                Optional.empty(),
                effects));
    }

    private static ResourceKey<ArrowFletchingDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.ARROW_FLETCHING,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
