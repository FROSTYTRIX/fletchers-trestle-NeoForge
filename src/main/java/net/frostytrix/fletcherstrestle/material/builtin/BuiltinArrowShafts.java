package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.ArrowShaftDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.effect.ApplyMobEffectEffect;
import net.frostytrix.fletcherstrestle.material.effect.BounceOnBlockEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierIfTargetBelowHealthEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierOnBackstabEffect;
import net.frostytrix.fletcherstrestle.material.effect.HealShooterEffect;
import net.frostytrix.fletcherstrestle.material.effect.PierceLevelEffect;
import net.frostytrix.fletcherstrestle.material.effect.SetVelocityMultiplierAtTickEffect;
import net.frostytrix.fletcherstrestle.material.effect.TeleportSwapWithTargetEffect;
import net.frostytrix.fletcherstrestle.material.stats.ArrowShaftStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/**
 * Built-in arrow-shaft material defs.
 *
 * <p>Beyond the legacy velocity/gravity stats, each shaft now carries its
 * thematic on-hit effect as a {@code MaterialEffect}:</p>
 * <ul>
 *   <li>dark_oak — pierce level 1</li>
 *   <li>crimson — executioner bonus damage on low-HP targets</li>
 *   <li>pale_oak — backstab bonus damage</li>
 *   <li>mangrove — slowness on hit</li>
 *   <li>cherry — heals shooter + cherry-leaves particle</li>
 *   <li>warped — teleport-swap with target</li>
 * </ul>
 *
 * <p>The acacia mid-flight speed boost is a tick-time effect and stays
 * hardcoded in {@code ModularArrowEntity.tick} for now — Phase E's
 * {@code on_tick} effects need a tick-specific lifecycle hook.</p>
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
                List.of()));

        register(ctx, SPRUCE,   ModItems.ROUGH_SPRUCE_LIMB,   1.0f, 1.1f, List.of());
        register(ctx, BIRCH,    ModItems.ROUGH_BIRCH_LIMB,    1.0f, 0.9f, List.of());

        // JUNGLE — 85% chance to bounce off a block, up to 3 times per arrow.
        register(ctx, JUNGLE,   ModItems.ROUGH_JUNGLE_LIMB,   1.0f, 1.0f,
                List.of(new BounceOnBlockEffect(0.85f, 3, 0.3f)));

        register(ctx, DARK_OAK, ModItems.ROUGH_DARK_OAK_LIMB, 1.0f, 1.0f,
                List.of(new PierceLevelEffect(1)));

        // ACACIA — at tick=10, multiply velocity ×1.4 for a brief mid-flight boost.
        register(ctx, ACACIA,   ModItems.ROUGH_ACACIA_LIMB,   1.0f, 1.0f,
                List.of(new SetVelocityMultiplierAtTickEffect(10, 1.4f)));

        register(ctx, MANGROVE, ModItems.ROUGH_MANGROVE_LIMB, 1.0f, 1.0f,
                List.of(new ApplyMobEffectEffect(MobEffects.MOVEMENT_SLOWDOWN, 80, 2)));

        register(ctx, CHERRY,   ModItems.ROUGH_CHERRY_LIMB,   1.0f, 1.0f,
                List.of(new HealShooterEffect(2.0f, Optional.of(ParticleTypes.CHERRY_LEAVES), 5)));

        register(ctx, PALE_OAK, ModItems.ROUGH_PALE_OAK_LIMB, 1.0f, 1.0f,
                List.of(new DamageMultiplierOnBackstabEffect(0.5f, 1.4f,
                        Optional.of(BuiltInRegistries.SOUND_EVENT
                                .getKey(net.minecraft.sounds.SoundEvents.BREEZE_WIND_CHARGE_BURST.value())))));

        register(ctx, CRIMSON,  ModItems.ROUGH_CRIMSON_LIMB,  1.0f, 1.0f,
                List.of(new DamageMultiplierIfTargetBelowHealthEffect(0.5f, 1.5f)));

        register(ctx, WARPED,   ModItems.ROUGH_WARPED_LIMB,   1.0f, 1.0f,
                List.of(new TeleportSwapWithTargetEffect(1.0f)));
    }

    private static void register(BootstrapContext<ArrowShaftDef> ctx,
                                 ResourceKey<ArrowShaftDef> key,
                                 java.util.function.Supplier<? extends ItemLike> ingredient,
                                 float velocityMult, float gravityMult,
                                 List<MaterialEffect> effects) {
        ctx.register(key, new ArrowShaftDef(
                Ingredient.of(ingredient.get()),
                new ArrowShaftStats(velocityMult, gravityMult),
                Optional.empty(),
                effects));
    }

    private static ResourceKey<ArrowShaftDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.ARROW_SHAFT,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
