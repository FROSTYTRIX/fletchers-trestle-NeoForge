package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.BowLimbDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.effect.ApplyMobEffectToShooterEffect;
import net.frostytrix.fletcherstrestle.material.effect.IgniteArrowEffect;
import net.frostytrix.fletcherstrestle.material.effect.SetArrowFlagEffect;
import net.frostytrix.fletcherstrestle.material.effect.SetArrowNoGravityEffect;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/**
 * Built-in bow-limb material defs. Stats mirror the legacy
 * {@code ModularBowItem.LimbStats} enum; behaviors that used to be hard-
 * coded {@code if ("crimson".equals(limbId))} branches in
 * {@code ModularBowItem.createProjectile} / {@code releaseUsing} are now
 * {@code MaterialEffect} entries on the relevant limb def.
 *
 * <p>Effects attached per material:</p>
 * <ul>
 *   <li>acacia — {@code apply_effect_to_shooter} (movement speed II, 30 ticks)</li>
 *   <li>spruce — {@code set_arrow_flag} ({@code fletcherstrestle:punch})</li>
 *   <li>crimson — {@code ignite_arrow} (100 seconds)</li>
 *   <li>warped — {@code set_arrow_no_gravity}</li>
 * </ul>
 */
public final class BuiltinBowLimbs {
    private BuiltinBowLimbs() {}

    public static final ResourceKey<BowLimbDef> OAK       = key("oak");
    public static final ResourceKey<BowLimbDef> SPRUCE    = key("spruce");
    public static final ResourceKey<BowLimbDef> BIRCH     = key("birch");
    public static final ResourceKey<BowLimbDef> JUNGLE    = key("jungle");
    public static final ResourceKey<BowLimbDef> ACACIA    = key("acacia");
    public static final ResourceKey<BowLimbDef> DARK_OAK  = key("dark_oak");
    public static final ResourceKey<BowLimbDef> MANGROVE  = key("mangrove");
    public static final ResourceKey<BowLimbDef> CHERRY    = key("cherry");
    public static final ResourceKey<BowLimbDef> PALE_OAK  = key("pale_oak");
    public static final ResourceKey<BowLimbDef> CRIMSON   = key("crimson");
    public static final ResourceKey<BowLimbDef> WARPED    = key("warped");

    public static void bootstrap(BootstrapContext<BowLimbDef> ctx) {
        // (id, item, drawTime, dmgMult, amphibious, slowFalling, effects)
        register(ctx, OAK,      ModItems.PLIABLE_OAK_LIMB,      20.0f, 1.00f, false, false, List.of());
        register(ctx, SPRUCE,   ModItems.PLIABLE_SPRUCE_LIMB,   22.0f, 1.00f, false, false,
                List.of(new SetArrowFlagEffect("fletcherstrestle:punch", true)));
        register(ctx, BIRCH,    ModItems.PLIABLE_BIRCH_LIMB,    10.0f, 0.70f, false, false, List.of());
        register(ctx, JUNGLE,   ModItems.PLIABLE_JUNGLE_LIMB,   18.0f, 0.90f, false, false, List.of());
        register(ctx, ACACIA,   ModItems.PLIABLE_ACACIA_LIMB,   20.0f, 1.00f, false, false,
                List.of(new ApplyMobEffectToShooterEffect(MobEffects.MOVEMENT_SPEED, 30, 1)));
        register(ctx, DARK_OAK, ModItems.PLIABLE_DARK_OAK_LIMB, 35.0f, 1.60f, false, false, List.of());
        register(ctx, MANGROVE, ModItems.PLIABLE_MANGROVE_LIMB, 22.0f, 1.00f, true,  false, List.of());
        register(ctx, CHERRY,   ModItems.PLIABLE_CHERRY_LIMB,   20.0f, 0.85f, false, true,  List.of());
        register(ctx, PALE_OAK, ModItems.PLIABLE_PALE_OAK_LIMB, 26.0f, 1.00f, false, false, List.of());
        register(ctx, CRIMSON,  ModItems.PLIABLE_CRIMSON_LIMB,  24.0f, 1.10f, false, false,
                List.of(new IgniteArrowEffect(100)));
        register(ctx, WARPED,   ModItems.PLIABLE_WARPED_LIMB,   20.0f, 1.00f, false, false,
                List.of(new SetArrowNoGravityEffect()));
    }

    private static void register(BootstrapContext<BowLimbDef> ctx,
                                 ResourceKey<BowLimbDef> key,
                                 java.util.function.Supplier<? extends ItemLike> ingredient,
                                 float drawTime, float damageMult,
                                 boolean amphibious, boolean givesSlowFalling,
                                 List<MaterialEffect> effects) {
        ctx.register(key, new BowLimbDef(
                Ingredient.of(ingredient.get()),
                new BowLimbStats(drawTime, damageMult, amphibious, givesSlowFalling),
                Optional.empty(),
                effects,
                Optional.empty()
        ));
    }

    private static ResourceKey<BowLimbDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.BOW_LIMB,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
