package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for an arrow shaft.
 *
 * @param velocityMultiplier  multiplier on initial velocity at spawn
 * @param gravityMultiplier   multiplier on the arrow's gravity (>1 drops faster)
 */
public record ArrowShaftStats(
        float velocityMultiplier,
        float gravityMultiplier) {

    public static final MapCodec<ArrowShaftStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("velocity_multiplier", 1.0f).forGetter(ArrowShaftStats::velocityMultiplier),
            Codec.FLOAT.optionalFieldOf("gravity_multiplier", 1.0f).forGetter(ArrowShaftStats::gravityMultiplier)
    ).apply(inst, ArrowShaftStats::new));

    public static final Codec<ArrowShaftStats> CODEC = MAP_CODEC.codec();
}
