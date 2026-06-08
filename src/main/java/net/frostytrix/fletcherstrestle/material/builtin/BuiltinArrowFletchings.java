package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.ArrowFletchingDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.effect.DropSelfOnHitEffect;
import net.frostytrix.fletcherstrestle.material.effect.SubtleHomingEffect;
import net.frostytrix.fletcherstrestle.material.stats.ArrowFletchingStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-fletching material defs. {@code bound} (drop-on-hit) and {@code serrated} (homing)
 * use effects; {@code vex} (block-phase) is stateful and keyed off its id in {@code ModularArrowEntity}.
 */
public final class BuiltinArrowFletchings {
    private BuiltinArrowFletchings() {
    }

    public static final ResourceKey<ArrowFletchingDef> FEATHER = key("feather");
    public static final ResourceKey<ArrowFletchingDef> RIGID = key("rigid");
    public static final ResourceKey<ArrowFletchingDef> TRAILING = key("trailing");
    public static final ResourceKey<ArrowFletchingDef> SERRATED = key("serrated");
    public static final ResourceKey<ArrowFletchingDef> BOUND = key("bound");
    public static final ResourceKey<ArrowFletchingDef> VEX = key("vex");
    // Coloured feathers — identical stats to plain feather, just cosmetic.
    public static final ResourceKey<ArrowFletchingDef> RED_FEATHER = key("red_feather");
    public static final ResourceKey<ArrowFletchingDef> BLUE_FEATHER = key("blue_feather");
    public static final ResourceKey<ArrowFletchingDef> GREEN_FEATHER = key("green_feather");
    public static final ResourceKey<ArrowFletchingDef> CYAN_FEATHER = key("cyan_feather");
    public static final ResourceKey<ArrowFletchingDef> LIGHT_GRAY_FEATHER = key("light_gray_feather");
    public static final ResourceKey<ArrowFletchingDef> BROWN_FEATHER = key("brown_feather");

    public static void bootstrap(BootstrapContext<ArrowFletchingDef> ctx) {
        register(ctx, FEATHER, Ingredient.of(Items.FEATHER), 1.00f, List.of());
        register(ctx, RIGID, Ingredient.of(Items.FLINT), 0.84f, List.of());
        register(ctx, TRAILING, Ingredient.of(Items.STRING), 0.75f, List.of());
        // SERRATED — mid-flight magnetism, pulls arrow toward nearest target
        // within 5 blocks. Skips the first 2 ticks so initial trajectory holds.
        register(ctx, SERRATED, Ingredient.of(Items.PHANTOM_MEMBRANE), 1.00f,
                List.of(new SubtleHomingEffect(5.0f, 1.0f, 2)));
        register(ctx, BOUND, Ingredient.of(Items.LEATHER), 1.00f,
                List.of(new DropSelfOnHitEffect(0.25f)));
        register(ctx, VEX, Ingredient.of(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE), 1.00f, List.of());

        register(ctx, RED_FEATHER, Ingredient.of(ModItems.RED_FEATHER.get()), 1.00f, List.of());
        register(ctx, BLUE_FEATHER, Ingredient.of(ModItems.BLUE_FEATHER.get()), 1.00f, List.of());
        register(ctx, GREEN_FEATHER, Ingredient.of(ModItems.GREEN_FEATHER.get()), 1.00f, List.of());
        register(ctx, CYAN_FEATHER, Ingredient.of(ModItems.CYAN_FEATHER.get()), 1.00f, List.of());
        register(ctx, LIGHT_GRAY_FEATHER, Ingredient.of(ModItems.LIGHT_GRAY_FEATHER.get()), 1.00f, List.of());
        register(ctx, BROWN_FEATHER, Ingredient.of(ModItems.BROWN_FEATHER.get()), 1.00f, List.of());
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
