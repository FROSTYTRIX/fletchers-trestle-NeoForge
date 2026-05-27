package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for a bow limb. Mirrors the closed schema in
 * the legacy {@code ModularBowItem.LimbStats} enum so the datagen migration
 * in Phase C is purely a 1:1 dump.
 *
 * @param drawTimeTicks    ticks needed for a full draw (vanilla bow is ~20)
 * @param damageMultiplier multiplier applied to base arrow damage
 * @param amphibious       whether shooting works at full strength underwater
 * @param givesSlowFalling whether aiming with this limb grants Slow Falling
 */
public record BowLimbStats(
        float drawTimeTicks,
        float damageMultiplier,
        boolean amphibious,
        boolean givesSlowFalling) {

    public static final MapCodec<BowLimbStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("draw_time_ticks").forGetter(BowLimbStats::drawTimeTicks),
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0f).forGetter(BowLimbStats::damageMultiplier),
            Codec.BOOL.optionalFieldOf("amphibious", false).forGetter(BowLimbStats::amphibious),
            Codec.BOOL.optionalFieldOf("gives_slow_falling", false).forGetter(BowLimbStats::givesSlowFalling)
    ).apply(inst, BowLimbStats::new));

    public static final Codec<BowLimbStats> CODEC = MAP_CODEC.codec();
}
