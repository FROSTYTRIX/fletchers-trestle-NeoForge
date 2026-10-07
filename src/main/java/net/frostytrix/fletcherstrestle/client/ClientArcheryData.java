package net.frostytrix.fletcherstrestle.client;

import net.frostytrix.fletcherstrestle.progression.ArcheryProgression;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.frostytrix.fletcherstrestle.progression.ArcherySkills;

/**
 * Client-side mirror of the local player's archery XP + skill ranks, kept in
 * sync by {@code ArcherySyncPacket}. The skill screen reads from here.
 *
 * <p>Holds only primitives + common-side helpers so it is safe to reference
 * from the (common) packet handler without pulling client classes onto a
 * dedicated server.</p>
 */
public final class ClientArcheryData {
    private ClientArcheryData() {
    }

    public static boolean loaded = false;
    public static int xp = 0;
    public static int draw = 0;
    public static int crit = 0;
    public static int aim = 0;
    public static int capstones = 0;

    public static void set(int xp, int draw, int crit, int aim, int capstones) {
        ClientArcheryData.xp = xp;
        ClientArcheryData.draw = draw;
        ClientArcheryData.crit = crit;
        ClientArcheryData.aim = aim;
        ClientArcheryData.capstones = capstones;
        ClientArcheryData.loaded = true;
    }

    public static int level() {
        return ArcheryProgression.levelForXp(xp);
    }

    public static int pointsSpent() {
        return skills().total();
    }

    public static ArcherySkills skills() {
        return new ArcherySkills(draw, crit, aim, capstones);
    }

    public static boolean hasCapstone(ArcherySkill skill) {
        return skills().hasCapstone(skill);
    }

    public static int pointsAvailable() {
        return Math.max(0, level() - pointsSpent());
    }

    public static int rank(ArcherySkill skill) {
        return switch (skill) {
            case DRAW -> draw;
            case CRIT -> crit;
            case AIM -> aim;
        };
    }
}
