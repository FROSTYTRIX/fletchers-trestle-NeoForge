package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for a bow riser (the central grip).
 *
 * @param maxDurability       weapon-durability cap when this riser is used
 * @param inaccuracyMultiplier  multiplier applied to base arrow inaccuracy
 *                              (iron riser ≈ 0.2 = laser-precise)
 */
public record BowRiserStats(
        int maxDurability,
        float inaccuracyMultiplier) {

    public static final MapCodec<BowRiserStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.fieldOf("max_durability").forGetter(BowRiserStats::maxDurability),
            Codec.FLOAT.optionalFieldOf("inaccuracy_multiplier", 1.0f).forGetter(BowRiserStats::inaccuracyMultiplier)
    ).apply(inst, BowRiserStats::new));

    public static final Codec<BowRiserStats> CODEC = MAP_CODEC.codec();
}
