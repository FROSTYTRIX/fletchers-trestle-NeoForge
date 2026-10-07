package net.frostytrix.fletcherstrestle.progression;

import net.frostytrix.fletcherstrestle.client.ClientArcheryData;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Aim capstone: after a second of sneaking still, shots have zero spread and
 * no flax shake, and a scoped crossbow zooms one step further.
 *
 * <p>Stillness is measured from the change in position between ticks rather than
 * from velocity, which isn't reliable for players on the server. Each side keeps
 * its own count: the server needs it for spread, the client for the scope.</p>
 */
public final class DeadCalm {
    private DeadCalm() {
    }

    /** One second of stillness before the calm settles. */
    public static final int SETTLE_TICKS = 20;
    /** Squared distance a "still" player may drift in a tick (about a twentieth of a block). */
    private static final double STILL = 0.0025;

    private static final Map<UUID, Integer> SERVER = new HashMap<>();
    private static final Map<UUID, Integer> CLIENT = new HashMap<>();

    private static Map<UUID, Integer> side(Player player) {
        return player.level().isClientSide() ? CLIENT : SERVER;
    }

    /** Called every player tick, on both sides. Returns true on the tick the calm settles. */
    public static boolean tick(Player player) {
        boolean still = player.isCrouching() && player.onGround()
                && player.position().distanceToSqr(player.xo, player.yo, player.zo) < STILL;
        Map<UUID, Integer> counts = side(player);
        if (!still) {
            counts.remove(player.getUUID());
            return false;
        }
        int held = counts.merge(player.getUUID(), 1, Integer::sum);
        return held == SETTLE_TICKS && owns(player);
    }

    /** Whether this archer owns Dead Calm and has been still long enough. */
    public static boolean isCalm(Player player) {
        return owns(player) && side(player).getOrDefault(player.getUUID(), 0) >= SETTLE_TICKS;
    }

    /** The client only knows its own player's capstones, through the synced mirror. */
    private static boolean owns(Player player) {
        return player.level().isClientSide()
                ? ClientArcheryData.hasCapstone(ArcherySkill.AIM)
                : ArcheryProgression.hasCapstone(player, ArcherySkill.AIM);
    }

    /** Forget a player who left, so the server map doesn't grow. */
    public static void forget(Player player) {
        side(player).remove(player.getUUID());
    }
}
