package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.material.effect.ApplyMobEffectEffect;
import net.frostytrix.fletcherstrestle.material.effect.BounceOnBlockEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierByDistanceEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierIfTargetArmoredEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierIfTargetBelowHealthEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierOnBackstabEffect;
import net.frostytrix.fletcherstrestle.material.effect.DropSelfOnHitEffect;
import net.frostytrix.fletcherstrestle.material.effect.HealShooterEffect;
import net.frostytrix.fletcherstrestle.material.effect.PierceLevelEffect;
import net.frostytrix.fletcherstrestle.material.effect.PullTargetToShooterEffect;
import net.frostytrix.fletcherstrestle.material.effect.TeleportSwapWithTargetEffect;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Custom NeoForge registry of {@link MaterialEffectType}s. This is the
 * extension point for 3rd-party mods that want to add new declarative
 * material behaviors — register a new effect type here and modpack JSONs can
 * reference it immediately.
 *
 * <p>The registry is created via {@link DeferredRegister#makeRegistry} which
 * fires {@code NewRegistryEvent} for us — no extra wiring in
 * {@link FletcherTrestle} beyond the standard {@code REGISTRY.register(bus)}
 * call.</p>
 *
 * <p>Phase A ships four representative effects so the data model can be
 * exercised end-to-end (codec, registry, list-of-effects on a def). The full
 * ~20-effect vocabulary lands in Phase E along with the call-site refactor;
 * adding effects later is purely additive.</p>
 */
public final class ModMaterialEffectTypes {
    private ModMaterialEffectTypes() {}

    /** Registry key used by both the registry creation and the dispatch codec. */
    public static final ResourceKey<Registry<MaterialEffectType<?>>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "material_effect_type"));

    public static final DeferredRegister<MaterialEffectType<?>> EFFECT_TYPES =
            DeferredRegister.create(REGISTRY_KEY, FletcherTrestle.MOD_ID);

    /**
     * The live registry. {@link DeferredRegister#makeRegistry} returns a
     * {@link Registry} that's backed by the real registry once
     * {@code NewRegistryEvent} fires. The dispatch codec in
     * {@link MaterialEffect#CODEC} reads through this at parse time, so types
     * registered after class-load are still discoverable.
     */
    public static final Registry<MaterialEffectType<?>> REGISTRY =
            EFFECT_TYPES.makeRegistry(builder -> builder
                    .sync(true) // material defs are server-side, but synced effects are friendlier
            );

    // --- Built-in effect types (Phase A: 4 representative implementations) ---

    /** {@code fletcherstrestle:apply_effect} — applies a MobEffect to the hit target. */
    public static final Supplier<MaterialEffectType<ApplyMobEffectEffect>> APPLY_EFFECT =
            EFFECT_TYPES.register("apply_effect",
                    () -> new MaterialEffectType<>(ApplyMobEffectEffect.CODEC));

    /** {@code fletcherstrestle:damage_multiplier} — scales arrow base damage by a constant. */
    public static final Supplier<MaterialEffectType<DamageMultiplierEffect>> DAMAGE_MULTIPLIER =
            EFFECT_TYPES.register("damage_multiplier",
                    () -> new MaterialEffectType<>(DamageMultiplierEffect.CODEC));

    /** {@code fletcherstrestle:pierce_level} — sets the arrow's pierce level on spawn. */
    public static final Supplier<MaterialEffectType<PierceLevelEffect>> PIERCE_LEVEL =
            EFFECT_TYPES.register("pierce_level",
                    () -> new MaterialEffectType<>(PierceLevelEffect.CODEC));

    /** {@code fletcherstrestle:bounce_on_block} — chance to bounce on block hit. */
    public static final Supplier<MaterialEffectType<BounceOnBlockEffect>> BOUNCE_ON_BLOCK =
            EFFECT_TYPES.register("bounce_on_block",
                    () -> new MaterialEffectType<>(BounceOnBlockEffect.CODEC));

    // --- Phase E on-hit effect types ---

    /** {@code fletcherstrestle:damage_multiplier_if_target_below_health} — crimson shaft executioner. */
    public static final Supplier<MaterialEffectType<DamageMultiplierIfTargetBelowHealthEffect>> DAMAGE_MULTIPLIER_IF_TARGET_BELOW_HEALTH =
            EFFECT_TYPES.register("damage_multiplier_if_target_below_health",
                    () -> new MaterialEffectType<>(DamageMultiplierIfTargetBelowHealthEffect.CODEC));

    /** {@code fletcherstrestle:damage_multiplier_on_backstab} — pale_oak shaft. */
    public static final Supplier<MaterialEffectType<DamageMultiplierOnBackstabEffect>> DAMAGE_MULTIPLIER_ON_BACKSTAB =
            EFFECT_TYPES.register("damage_multiplier_on_backstab",
                    () -> new MaterialEffectType<>(DamageMultiplierOnBackstabEffect.CODEC));

    /** {@code fletcherstrestle:damage_multiplier_by_distance} — weighted_blunt head. */
    public static final Supplier<MaterialEffectType<DamageMultiplierByDistanceEffect>> DAMAGE_MULTIPLIER_BY_DISTANCE =
            EFFECT_TYPES.register("damage_multiplier_by_distance",
                    () -> new MaterialEffectType<>(DamageMultiplierByDistanceEffect.CODEC));

    /** {@code fletcherstrestle:damage_multiplier_if_target_armored} — bodkin_point head. */
    public static final Supplier<MaterialEffectType<DamageMultiplierIfTargetArmoredEffect>> DAMAGE_MULTIPLIER_IF_TARGET_ARMORED =
            EFFECT_TYPES.register("damage_multiplier_if_target_armored",
                    () -> new MaterialEffectType<>(DamageMultiplierIfTargetArmoredEffect.CODEC));

    /** {@code fletcherstrestle:heal_shooter} — cherry shaft petal-burst. */
    public static final Supplier<MaterialEffectType<HealShooterEffect>> HEAL_SHOOTER =
            EFFECT_TYPES.register("heal_shooter",
                    () -> new MaterialEffectType<>(HealShooterEffect.CODEC));

    /** {@code fletcherstrestle:pull_target_to_shooter} — barbed_tip head. */
    public static final Supplier<MaterialEffectType<PullTargetToShooterEffect>> PULL_TARGET_TO_SHOOTER =
            EFFECT_TYPES.register("pull_target_to_shooter",
                    () -> new MaterialEffectType<>(PullTargetToShooterEffect.CODEC));

    /** {@code fletcherstrestle:teleport_swap_with_target} — warped shaft. */
    public static final Supplier<MaterialEffectType<TeleportSwapWithTargetEffect>> TELEPORT_SWAP_WITH_TARGET =
            EFFECT_TYPES.register("teleport_swap_with_target",
                    () -> new MaterialEffectType<>(TeleportSwapWithTargetEffect.CODEC));

    /** {@code fletcherstrestle:drop_self_on_hit} — bound fletching. */
    public static final Supplier<MaterialEffectType<DropSelfOnHitEffect>> DROP_SELF_ON_HIT =
            EFFECT_TYPES.register("drop_self_on_hit",
                    () -> new MaterialEffectType<>(DropSelfOnHitEffect.CODEC));

    public static void register(IEventBus bus) {
        EFFECT_TYPES.register(bus);
    }
}
