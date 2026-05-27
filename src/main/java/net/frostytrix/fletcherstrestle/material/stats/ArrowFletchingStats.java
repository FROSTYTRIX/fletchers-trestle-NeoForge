package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for an arrow fletching.
 *
 * @param inaccuracyMultiplier  multiplier on base inaccuracy (<1 = tighter group)
 */
public record ArrowFletchingStats(float inaccuracyMultiplier) {

    public static final MapCodec<ArrowFletchingStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("inaccuracy_multiplier", 1.0f).forGetter(ArrowFletchingStats::inaccuracyMultiplier)
    ).apply(inst, ArrowFletchingStats::new));

    public static final Codec<ArrowFletchingStats> CODEC = MAP_CODEC.codec();
}
