package net.frostytrix.fletcherstrestle.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Spent skill-point ranks per branch (Phase 2 skill tree). Stored on the
 * player via {@link ModAttachments#ARCHERY_SKILLS}.
 */
public record ArcherySkills(int draw, int crit, int aim) {

    public static final ArcherySkills EMPTY = new ArcherySkills(0, 0, 0);

    public static final Codec<ArcherySkills> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("draw", 0).forGetter(ArcherySkills::draw),
            Codec.INT.optionalFieldOf("crit", 0).forGetter(ArcherySkills::crit),
            Codec.INT.optionalFieldOf("aim", 0).forGetter(ArcherySkills::aim)
    ).apply(inst, ArcherySkills::new));

    /** Total points spent across all branches. */
    public int total() {
        return draw + crit + aim;
    }
}
