package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.MapCodec;

/**
 * Registry entry for a kind of {@link MaterialEffect}. Wraps the per-type
 * map-codec used by the polymorphic dispatch in {@link MaterialEffect#CODEC}.
 *
 * <p>Effect types are registered through {@link ModMaterialEffectTypes#EFFECT_TYPES}
 *: companion mods can add their own by registering against the same registry
 * key on the mod-event bus.</p>
 *
 * @param codec the per-implementation map-codec; the dispatch codec calls
 *              {@code codec()} after picking the right type by id.
 */
public record MaterialEffectType<T extends MaterialEffect>(MapCodec<T> codec) {
}
