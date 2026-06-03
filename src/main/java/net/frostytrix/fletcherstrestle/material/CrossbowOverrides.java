package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;

import java.util.Optional;

/**
 * Optional sub-record on {@link BowLimbDef} letting one limb material tune stats just for the
 * crossbow (e.g. when crossbow draw-time should diverge from the bow). Absent fields inherit
 * from the parent limb def.
 */
public record CrossbowOverrides(Optional<BowLimbStats> stats) {

    public static final MapCodec<CrossbowOverrides> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BowLimbStats.CODEC.optionalFieldOf("stats").forGetter(CrossbowOverrides::stats)
    ).apply(inst, CrossbowOverrides::new));

    public static final Codec<CrossbowOverrides> CODEC = MAP_CODEC.codec();
}
