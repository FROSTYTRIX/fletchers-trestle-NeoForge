package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for a bow string.
 *
 * @param velocityMultiplier  multiplier applied to the projectile's initial speed
 * @param durabilityCost      durability consumed per shot (high-tension = 2)
 */
public record BowStringStats(
        float velocityMultiplier,
        int durabilityCost) {

    public static final MapCodec<BowStringStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("velocity_multiplier", 1.0f).forGetter(BowStringStats::velocityMultiplier),
            Codec.INT.optionalFieldOf("durability_cost", 1).forGetter(BowStringStats::durabilityCost)
    ).apply(inst, BowStringStats::new));

    public static final Codec<BowStringStats> CODEC = MAP_CODEC.codec();
}
