package net.frostytrix.fletcherstrestle.progression;

import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Spent skill-point ranks per branch, plus the capstones
 * bought at the tip of a maxed branch. Stored on the player via
 * {@link ModAttachments#ARCHERY_SKILLS}.
 *
 * <p>{@code capstones} is a bitmask indexed by {@link ArcherySkill#ordinal()}.
 * It's an optional field, so saves from before capstones load as "none".</p>
 */
public record ArcherySkills(int draw, int crit, int aim, int capstones) {

    public ArcherySkills(int draw, int crit, int aim) {
        this(draw, crit, aim, 0);
    }

    public static final ArcherySkills EMPTY = new ArcherySkills(0, 0, 0);

    public static final Codec<ArcherySkills> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("draw", 0).forGetter(ArcherySkills::draw),
            Codec.INT.optionalFieldOf("crit", 0).forGetter(ArcherySkills::crit),
            Codec.INT.optionalFieldOf("aim", 0).forGetter(ArcherySkills::aim),
            Codec.INT.optionalFieldOf("capstones", 0).forGetter(ArcherySkills::capstones)
    ).apply(inst, ArcherySkills::new));

    public boolean hasCapstone(ArcherySkill skill) {
        return (capstones & (1 << skill.ordinal())) != 0;
    }

    public ArcherySkills withCapstone(ArcherySkill skill) {
        return new ArcherySkills(draw, crit, aim, capstones | (1 << skill.ordinal()));
    }

    public int capstoneCount() {
        return Integer.bitCount(capstones);
    }

    /** Total points spent: every rank, plus each capstone at its configured cost. */
    public int total() {
        return draw + crit + aim + capstoneCount() * FletcherConfig.CAPSTONE_COST.get();
    }
}
