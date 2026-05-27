package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A declarative behavior attached to a bow / arrow material via JSON.
 *
 * Each implementation is a record (or near-record) holding parameters parsed
 * from JSON via its {@link MaterialEffectType#codec()}. Dispatch is by the
 * effect's registered type id; see {@link #CODEC}.
 *
 * <h3>Why an interface, not a sealed class</h3>
 * 3rd-party mods register their own {@link MaterialEffectType}s through the
 * {@link ModMaterialEffectTypes#EFFECT_TYPES DeferredRegister}, so the set of
 * implementations is open. The dispatch codec walks the registry at parse
 * time — adding a type post-init still works.
 *
 * <h3>Lifecycle hooks</h3>
 * Each hook is a {@code default} no-op; an effect only overrides the ones it
 * cares about. This keeps a tiny effect like {@code apply_effect} from having
 * to stub out four irrelevant methods. The actual call sites are wired in
 * Phase E — for Phase A we just want the codec system to round-trip.
 *
 * <p>Hooks:</p>
 * <ul>
 *   <li>{@link #onArrowSpawn} — once, at entity spawn (use for pierce level,
 *       gravity tweaks, base velocity).</li>
 *   <li>{@link #onArrowTick} — every server tick while in flight.</li>
 *   <li>{@link #onArrowHit} — when the arrow hits a living entity.</li>
 *   <li>{@link #onBowRelease} — when a bow / crossbow releases the shot.</li>
 * </ul>
 */
public interface MaterialEffect {
    /**
     * Polymorphic codec that picks an implementation by registered effect-type
     * id. Reads the live registry — works for types registered after class
     * load as well.
     */
    Codec<MaterialEffect> CODEC = ModMaterialEffectTypes.REGISTRY
            .byNameCodec()
            .dispatch(MaterialEffect::type, MaterialEffectType::codec);

    /**
     * The type that produced this effect. Used by {@link #CODEC} to look up
     * the right per-type codec when serialising back to JSON / network.
     */
    MaterialEffectType<? extends MaterialEffect> type();

    /** Called once when the arrow is added to the world. */
    default void onArrowSpawn(ModularArrowEntity arrow) {}

    /** Called every server-side tick while the arrow is in flight. */
    default void onArrowTick(ModularArrowEntity arrow) {}

    /** Called when the arrow hits an entity. {@code result} is the hit info. */
    default void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {}

    /**
     * Called when a bow / crossbow releases a shot. The default no-op covers
     * the common case where a bow effect only modifies arrow stats via the
     * surrounding code rather than running its own logic on release.
     */
    default void onBowRelease(LivingEntity shooter, ItemStack weapon) {}
}
