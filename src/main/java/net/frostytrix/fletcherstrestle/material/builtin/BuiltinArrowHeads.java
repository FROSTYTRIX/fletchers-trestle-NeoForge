package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.effect.ModEffects;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.ArrowHeadDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.effect.ApplyMobEffectEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierByDistanceEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierIfTargetArmoredEffect;
import net.frostytrix.fletcherstrestle.material.effect.PullTargetToShooterEffect;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-head material defs. Mirrors the legacy
 * {@code ModularArrowItem.HeadStats} enum stats AND the hardcoded on-hit
 * branches that used to live in {@code ModularArrowEntity.onHitEntity}.
 *
 * <p>Each head's special behaviors (bleed for broadhead, armor-piercing
 * for bodkin, distance damage for weighted_blunt, target-pull for barbed,
 * etc.) is now expressed as a {@code MaterialEffect} in its def, so a
 * modpack can swap them out, parameterise them, or attach extra effects
 * by overriding the JSON.</p>
 *
 * <p>The complex multi-tick effects — resonance echo, rope deployment,
 * grappling hook, glass-vial potion splash — still live in
 * {@code ModularArrowEntity} for the moment; they need stateful
 * coordination with the arrow's tick lifecycle that the simple effect
 * lifecycle hooks don't expose. A future "stateful effect" extension can
 * lift them up too if needed.</p>
 */
public final class BuiltinArrowHeads {
    private BuiltinArrowHeads() {
    }

    public static final ResourceKey<ArrowHeadDef> FLINT = key("flint");
    public static final ResourceKey<ArrowHeadDef> BROADHEAD = key("broadhead");
    public static final ResourceKey<ArrowHeadDef> BODKIN_POINT = key("bodkin_point");
    public static final ResourceKey<ArrowHeadDef> RESONANCE_TIP = key("resonance_tip");
    public static final ResourceKey<ArrowHeadDef> BARBED_TIP = key("barbed_tip");
    public static final ResourceKey<ArrowHeadDef> WEIGHTED_BLUNT = key("weighted_blunt");
    public static final ResourceKey<ArrowHeadDef> WEIGHTED_HOOK = key("weighted_hook");
    public static final ResourceKey<ArrowHeadDef> TRAILING_ROPE = key("trailing_rope");
    public static final ResourceKey<ArrowHeadDef> GLASS_VIAL = key("glass_vial");

    public static void bootstrap(BootstrapContext<ArrowHeadDef> ctx) {
        // FLINT — vanilla baseline.
        register(ctx, FLINT, Ingredient.of(Items.FLINT), 1.00f, List.of());

        // BROADHEAD — applies the mod's BLEED MobEffect for 60 ticks.
        register(ctx, BROADHEAD, Ingredient.of(Items.IRON_INGOT), 1.15f,
                List.of(new ApplyMobEffectEffect(ModEffects.BLEED_EFFECT, 60, 0)));

        // BODKIN_POINT — +25% damage on armored targets.
        register(ctx, BODKIN_POINT, Ingredient.of(Items.COPPER_INGOT), 1.00f,
                List.of(new DamageMultiplierIfTargetArmoredEffect(1.25f)));

        // RESONANCE_TIP — delayed echo damage. Still hardcoded in
        // ModularArrowEntity.onHitEntity for now (stateful multi-tick).
        register(ctx, RESONANCE_TIP, Ingredient.of(Items.ECHO_SHARD), 1.00f, List.of());

        // BARBED_TIP — yanks target toward shooter on hit.
        register(ctx, BARBED_TIP, Ingredient.of(Items.IRON_NUGGET), 1.00f,
                List.of(new PullTargetToShooterEffect(0.75f, 0.25f)));

        // WEIGHTED_BLUNT — +1× damage per 100 blocks of travel.
        register(ctx, WEIGHTED_BLUNT, Ingredient.of(Items.GOLD_INGOT), 1.05f,
                List.of(new DamageMultiplierByDistanceEffect(100f)));

        // WEIGHTED_HOOK / TRAILING_ROPE / GLASS_VIAL — stateful multi-tick
        // behaviors still live in ModularArrowEntity. The defs ship with
        // empty effect lists; the behavior is triggered by the entity
        // when it sees the matching head id.
        register(ctx, WEIGHTED_HOOK, itemIng(ModItems.WEIGHTED_HOOK::get), 0.50f, List.of());
        register(ctx, TRAILING_ROPE, itemIng(() -> ModBlocks.ROPE.asItem()), 0.30f, List.of());
        register(ctx, GLASS_VIAL, Ingredient.of(Items.GLASS_BOTTLE), 0.40f, List.of());
    }

    private static Ingredient itemIng(java.util.function.Supplier<? extends ItemLike> s) {
        return Ingredient.of(s.get());
    }

    private static void register(BootstrapContext<ArrowHeadDef> ctx,
                                 ResourceKey<ArrowHeadDef> key,
                                 Ingredient ingredient, float damageMult,
                                 List<MaterialEffect> effects) {
        ctx.register(key, new ArrowHeadDef(
                ingredient,
                new net.frostytrix.fletcherstrestle.material.stats.ArrowHeadStats(damageMult),
                Optional.empty(),
                effects
        ));
    }

    private static ResourceKey<ArrowHeadDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.ARROW_HEAD,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
