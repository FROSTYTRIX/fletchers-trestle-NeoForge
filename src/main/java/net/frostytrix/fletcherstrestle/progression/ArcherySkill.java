package net.frostytrix.fletcherstrestle.progression;

/**
 * The three archery skill-tree branches. Each can be ranked up to
 * {@link #MAX_RANK} by spending points earned on level-up.
 */
public enum ArcherySkill {
    DRAW,   // faster draw (down to 0.8x draw time)
    CRIT,   // crit chance (up to 30%)
    AIM;    // steadier aim (down to 0.7x spread) + longer flax-string grace

    public static final int MAX_RANK = 10;

    public int rank(ArcherySkills skills) {
        return switch (this) {
            case DRAW -> skills.draw();
            case CRIT -> skills.crit();
            case AIM -> skills.aim();
        };
    }

    /** Returns a copy of {@code skills} with this branch's rank increased by one. */
    public ArcherySkills increment(ArcherySkills skills) {
        return switch (this) {
            case DRAW -> new ArcherySkills(skills.draw() + 1, skills.crit(), skills.aim());
            case CRIT -> new ArcherySkills(skills.draw(), skills.crit() + 1, skills.aim());
            case AIM -> new ArcherySkills(skills.draw(), skills.crit(), skills.aim() + 1);
        };
    }
}
