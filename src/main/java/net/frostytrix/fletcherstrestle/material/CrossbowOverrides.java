package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;

import java.util.Optional;

/**
 * Optional sub-record on {@link BowLimbDef} that lets a single limb material
 * tune individual stats *just for the crossbow*. Picked over the user's
 * "shared def + optional override" answer in the roadmap — one source of
 * truth, with a tiny escape hatch for the cases (if any) where a designer
 * wants crossbow draw-time to diverge from bow draw-time.
 *
 * <p>Every field is optional. {@code Optional.empty()} = "use the bow value".</p>
 *
 * <p>Phase A only defines the codec; no consumer reads
 * {@code crossbowOverrides} yet. Phase D wires it into the crossbow
 * stat-lookup path.</p>
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
