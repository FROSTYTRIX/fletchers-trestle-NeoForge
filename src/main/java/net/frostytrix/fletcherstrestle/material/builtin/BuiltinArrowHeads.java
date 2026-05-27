package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.ArrowHeadDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.stats.ArrowHeadStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-head material defs. Mirrors the legacy
 * {@code ModularArrowItem.HeadStats} enum, with stats limited to the
 * damage multiplier (the "causes bleed" / "armor piercing" booleans become
 * effects in Phase E).
 *
 * <p>Ingredient mapping mirrors {@code ModularArrowRecipe.getArrowHead}.</p>
 */
public final class BuiltinArrowHeads {
    private BuiltinArrowHeads() {}

    public static final ResourceKey<ArrowHeadDef> FLINT          = key("flint");
    public static final ResourceKey<ArrowHeadDef> BROADHEAD      = key("broadhead");
    public static final ResourceKey<ArrowHeadDef> BODKIN_POINT   = key("bodkin_point");
    public static final ResourceKey<ArrowHeadDef> RESONANCE_TIP  = key("resonance_tip");
    public static final ResourceKey<ArrowHeadDef> BARBED_TIP     = key("barbed_tip");
    public static final ResourceKey<ArrowHeadDef> WEIGHTED_BLUNT = key("weighted_blunt");
    public static final ResourceKey<ArrowHeadDef> WEIGHTED_HOOK  = key("weighted_hook");
    public static final ResourceKey<ArrowHeadDef> TRAILING_ROPE  = key("trailing_rope");
    public static final ResourceKey<ArrowHeadDef> GLASS_VIAL     = key("glass_vial");

    public static void bootstrap(BootstrapContext<ArrowHeadDef> ctx) {
        // (key, ingredient item, damageMult)
        register(ctx, FLINT,          Ingredient.of(Items.FLINT),          1.00f);
        register(ctx, BROADHEAD,      Ingredient.of(Items.IRON_INGOT),     1.15f);
        register(ctx, BODKIN_POINT,   Ingredient.of(Items.COPPER_INGOT),   1.00f);
        register(ctx, RESONANCE_TIP,  Ingredient.of(Items.ECHO_SHARD),     1.00f);
        register(ctx, BARBED_TIP,     Ingredient.of(Items.IRON_NUGGET),    1.00f);
        register(ctx, WEIGHTED_BLUNT, Ingredient.of(Items.GOLD_INGOT),     1.05f);
        register(ctx, WEIGHTED_HOOK,  itemIng(ModItems.WEIGHTED_HOOK::get), 0.50f);
        register(ctx, TRAILING_ROPE,  itemIng(() -> ModBlocks.ROPE.asItem()), 0.30f);
        register(ctx, GLASS_VIAL,     Ingredient.of(Items.GLASS_BOTTLE),   0.40f);
    }

    private static Ingredient itemIng(java.util.function.Supplier<? extends ItemLike> s) {
        return Ingredient.of(s.get());
    }

    private static void register(BootstrapContext<ArrowHeadDef> ctx,
                                 ResourceKey<ArrowHeadDef> key,
                                 Ingredient ingredient, float damageMult) {
        ctx.register(key, new ArrowHeadDef(
                ingredient,
                new ArrowHeadStats(damageMult),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
    }

    private static ResourceKey<ArrowHeadDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.ARROW_HEAD,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
