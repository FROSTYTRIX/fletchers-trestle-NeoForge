package net.frostytrix.fletcherstrestle.material.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-material numeric stats for an arrow head.
 *
 * <p>Note: most "special" head behaviors (bleed, armor-pierce math,
 * resonance echo, glass-vial splash, etc.) live in the def's effect list,
 * not here. This record only carries stats that participate in vanilla
 * damage / hit math without conditional logic.</p>
 *
 * @param damageMultiplier multiplier on base arrow damage
 */
public record ArrowHeadStats(float damageMultiplier) {

    public static final MapCodec<ArrowHeadStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0f).forGetter(ArrowHeadStats::damageMultiplier)
    ).apply(inst, ArrowHeadStats::new));

    public static final Codec<ArrowHeadStats> CODEC = MAP_CODEC.codec();
}
