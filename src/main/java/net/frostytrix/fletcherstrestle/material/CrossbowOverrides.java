package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;

import java.util.Optional;

/**
 * Optional sub-record on {@link BowLimbDef} that lets a single limb material
 * tune individual stats just for the crossbow — one shared limb def with a
 * small escape hatch for when crossbow draw-time should diverge from bow
 * draw-time.
 *
 * <p>Every field is optional. {@code Optional.empty()} = "use the bow value".</p>
 *
 * @param stats   per-field overrides for {@link BowLimbStats}; absent fields
 *                inherit from the parent limb def.
 */
public record CrossbowOverrides(Optional<BowLimbStats> stats) {

    public static final MapCodec<CrossbowOverrides> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BowLimbStats.CODEC.optionalFieldOf("stats").forGetter(CrossbowOverrides::stats)
    ).apply(inst, CrossbowOverrides::new));

    public static final Codec<CrossbowOverrides> CODEC = MAP_CODEC.codec();
}
