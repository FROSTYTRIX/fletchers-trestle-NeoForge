package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.material.effect.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Custom NeoForge registry of {@link MaterialEffectType}s: the extension point for new declarative
 * material behaviors. Register a type here (or against {@link #REGISTRY_KEY} from a companion mod)
 * and modpack JSONs can reference it immediately.
 */
public final class ModMaterialEffectTypes {
    private ModMaterialEffectTypes() {
    }

    public static final ResourceKey<Registry<MaterialEffectType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "material_effect_type"));

    public static final DeferredRegister<MaterialEffectType<?>> EFFECT_TYPES =
            DeferredRegister.create(REGISTRY_KEY, FletcherTrestle.MOD_ID);

    /** Live registry; the dispatch codec reads through it, so post-init types stay discoverable. */
    public static final Registry<MaterialEffectType<?>> REGISTRY =
            EFFECT_TYPES.makeRegistry(builder -> builder.sync(true));

    // --- On-spawn / on-hit effect types ---

    /**
     * {@code fletcherstrestle:apply_effect}: applies a MobEffect to the hit target.
     */
    public static final Supplier<MaterialEffectType<ApplyMobEffectEffect>> APPLY_EFFECT =
            EFFECT_TYPES.register("apply_effect",
                    () -> new MaterialEffectType<>(ApplyMobEffectEffect.CODEC));

    /**
     * {@code fletcherstrestle:damage_multiplier}: scales arrow base damage by a constant.
     */
    public static final Supplier<MaterialEffectType<DamageMultiplierEffect>> DAMAGE_MULTIPLIER =
            EFFECT_TYPES.register("damage_multiplier",
                    () -> new MaterialEffectType<>(DamageMultiplierEffect.CODEC));

    /**
     * {@code fletcherstrestle:pierce_level}: sets the arrow's pierce level on spawn.
     */
    public static final Supplier<MaterialEffectType<PierceLevelEffect>> PIERCE_LEVEL =
            EFFECT_TYPES.register("pierce_level",
                    () -> new MaterialEffectType<>(PierceLevelEffect.CODEC));

    /**
     * {@code fletcherstrestle:bounce_on_block}: chance to bounce on block hit.
     */
    public static final Supplier<MaterialEffectType<BounceOnBlockEffect>> BOUNCE_ON_BLOCK =
            EFFECT_TYPES.register("bounce_on_block",
                    () -> new MaterialEffectType<>(BounceOnBlockEffect.CODEC));

    /**
     * {@code fletcherstrestle:damage_multiplier_if_target_below_health}: crimson shaft executioner.
     */
    public static final Supplier<MaterialEffectType<DamageMultiplierIfTargetBelowHealthEffect>> DAMAGE_MULTIPLIER_IF_TARGET_BELOW_HEALTH =
            EFFECT_TYPES.register("damage_multiplier_if_target_below_health",
                    () -> new MaterialEffectType<>(DamageMultiplierIfTargetBelowHealthEffect.CODEC));

    /**
     * {@code fletcherstrestle:damage_multiplier_on_backstab}: pale_oak shaft.
     */
    public static final Supplier<MaterialEffectType<DamageMultiplierOnBackstabEffect>> DAMAGE_MULTIPLIER_ON_BACKSTAB =
            EFFECT_TYPES.register("damage_multiplier_on_backstab",
                    () -> new MaterialEffectType<>(DamageMultiplierOnBackstabEffect.CODEC));

    /**
     * {@code fletcherstrestle:damage_multiplier_by_distance}: weighted_blunt head.
     */
    public static final Supplier<MaterialEffectType<DamageMultiplierByDistanceEffect>> DAMAGE_MULTIPLIER_BY_DISTANCE =
            EFFECT_TYPES.register("damage_multiplier_by_distance",
                    () -> new MaterialEffectType<>(DamageMultiplierByDistanceEffect.CODEC));

    /**
     * {@code fletcherstrestle:damage_multiplier_if_target_armored}: bodkin_point head.
     */
    public static final Supplier<MaterialEffectType<DamageMultiplierIfTargetArmoredEffect>> DAMAGE_MULTIPLIER_IF_TARGET_ARMORED =
            EFFECT_TYPES.register("damage_multiplier_if_target_armored",
                    () -> new MaterialEffectType<>(DamageMultiplierIfTargetArmoredEffect.CODEC));

    /**
     * {@code fletcherstrestle:heal_shooter}: cherry shaft petal-burst.
     */
    public static final Supplier<MaterialEffectType<HealShooterEffect>> HEAL_SHOOTER =
            EFFECT_TYPES.register("heal_shooter",
                    () -> new MaterialEffectType<>(HealShooterEffect.CODEC));

    /**
     * {@code fletcherstrestle:pull_target_to_shooter}: barbed_tip head.
     */
    public static final Supplier<MaterialEffectType<PullTargetToShooterEffect>> PULL_TARGET_TO_SHOOTER =
            EFFECT_TYPES.register("pull_target_to_shooter",
                    () -> new MaterialEffectType<>(PullTargetToShooterEffect.CODEC));

    /**
     * {@code fletcherstrestle:teleport_swap_with_target}: warped shaft.
     */
    public static final Supplier<MaterialEffectType<TeleportSwapWithTargetEffect>> TELEPORT_SWAP_WITH_TARGET =
            EFFECT_TYPES.register("teleport_swap_with_target",
                    () -> new MaterialEffectType<>(TeleportSwapWithTargetEffect.CODEC));

    /**
     * {@code fletcherstrestle:drop_self_on_hit}: bound fletching.
     */
    public static final Supplier<MaterialEffectType<DropSelfOnHitEffect>> DROP_SELF_ON_HIT =
            EFFECT_TYPES.register("drop_self_on_hit",
                    () -> new MaterialEffectType<>(DropSelfOnHitEffect.CODEC));

    // --- Tick + block-hit effect types ---

    /**
     * {@code fletcherstrestle:set_velocity_multiplier_at_tick}: acacia shaft mid-flight boost.
     */
    public static final Supplier<MaterialEffectType<SetVelocityMultiplierAtTickEffect>> SET_VELOCITY_MULTIPLIER_AT_TICK =
            EFFECT_TYPES.register("set_velocity_multiplier_at_tick",
                    () -> new MaterialEffectType<>(SetVelocityMultiplierAtTickEffect.CODEC));

    /**
     * {@code fletcherstrestle:subtle_homing}: serrated fletching magnetism.
     */
    public static final Supplier<MaterialEffectType<SubtleHomingEffect>> SUBTLE_HOMING =
            EFFECT_TYPES.register("subtle_homing",
                    () -> new MaterialEffectType<>(SubtleHomingEffect.CODEC));

    // --- Bow / crossbow release effect types ---

    /**
     * {@code fletcherstrestle:ignite_arrow}: crimson bow/crossbow limb.
     */
    public static final Supplier<MaterialEffectType<IgniteArrowEffect>> IGNITE_ARROW =
            EFFECT_TYPES.register("ignite_arrow",
                    () -> new MaterialEffectType<>(IgniteArrowEffect.CODEC));

    /**
     * {@code fletcherstrestle:set_arrow_no_gravity}: warped limb.
     */
    public static final Supplier<MaterialEffectType<SetArrowNoGravityEffect>> SET_ARROW_NO_GRAVITY =
            EFFECT_TYPES.register("set_arrow_no_gravity",
                    () -> new MaterialEffectType<>(SetArrowNoGravityEffect.CODEC));

    /**
     * {@code fletcherstrestle:set_arrow_flag}: spruce punch, copper conductive, etc.
     */
    public static final Supplier<MaterialEffectType<SetArrowFlagEffect>> SET_ARROW_FLAG =
            EFFECT_TYPES.register("set_arrow_flag",
                    () -> new MaterialEffectType<>(SetArrowFlagEffect.CODEC));

    /**
     * {@code fletcherstrestle:apply_effect_to_shooter}: acacia limb speed buff.
     */
    public static final Supplier<MaterialEffectType<ApplyMobEffectToShooterEffect>> APPLY_EFFECT_TO_SHOOTER =
            EFFECT_TYPES.register("apply_effect_to_shooter",
                    () -> new MaterialEffectType<>(ApplyMobEffectToShooterEffect.CODEC));

    // --- Scripted escape hatch (KubeJS / companion mods) ---

    /**
     * {@code fletcherstrestle:scripted_callback}: looks up a
     * {@link net.frostytrix.fletcherstrestle.material.ScriptedEffectCallbacks.Handler}
     * by id at runtime and delegates every lifecycle hook to it.
     * The escape valve for behaviors the closed vocabulary can't
     * express, accessible to both KubeJS scripts and Java companion
     * mods through {@link net.frostytrix.fletcherstrestle.material.ScriptedEffectCallbacks#register}.
     */
    public static final Supplier<MaterialEffectType<ScriptedCallbackEffect>> SCRIPTED_CALLBACK =
            EFFECT_TYPES.register("scripted_callback",
                    () -> new MaterialEffectType<>(ScriptedCallbackEffect.CODEC));

    public static void register(IEventBus bus) {
        EFFECT_TYPES.register(bus);
    }
}
