package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for a bow riser (the central grip).
 *
 * @param maxDurability        weapon-durability cap when this riser is used
 * @param inaccuracyMultiplier multiplier applied to base arrow inaccuracy
 *                             (iron riser ≈ 0.2 = laser-precise)
 * @param metal                whether this riser is rigid enough to carry a
 *                             high-tension string
 */
public record BowRiserStats(
        int maxDurability,
        float inaccuracyMultiplier,
        boolean metal) {

    public static final MapCodec<BowRiserStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.fieldOf("max_durability").forGetter(BowRiserStats::maxDurability),
            Codec.FLOAT.optionalFieldOf("inaccuracy_multiplier", 1.0f).forGetter(BowRiserStats::inaccuracyMultiplier),
            Codec.BOOL.optionalFieldOf("metal", false).forGetter(BowRiserStats::metal)
    ).apply(inst, BowRiserStats::new));

    public static final Codec<BowRiserStats> CODEC = MAP_CODEC.codec();
}
