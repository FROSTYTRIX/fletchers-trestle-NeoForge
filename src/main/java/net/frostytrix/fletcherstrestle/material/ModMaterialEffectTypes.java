package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.material.effect.ApplyMobEffectEffect;
import net.frostytrix.fletcherstrestle.material.effect.BounceOnBlockEffect;
import net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierEffect;
import net.frostytrix.fletcherstrestle.material.effect.PierceLevelEffect;
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

    public static void register(IEventBus bus) {
        EFFECT_TYPES.register(bus);
    }
}
