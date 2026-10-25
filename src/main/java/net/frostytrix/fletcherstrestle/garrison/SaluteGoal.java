package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * An idle warden snaps off a salute to a player walking past, at most once a
 * minute each. A player sneaking by goes unsaluted.
 */
public class SaluteGoal extends Goal {

    private static final double RANGE = 4.5;
    private static final long COOLDOWN = 1200;
    private static final int DURATION = 40;
    private static final int SCAN_INTERVAL = 10;

    private final GarrisonGolemEntity golem;
    @Nullable
    private Player player;
    private int ticks;
    private int scanDelay;

    public SaluteGoal(GarrisonGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // canUse runs every other tick on an id-dependent parity, so count down rather than use tickCount.
        if (--scanDelay > 0) return false;
        scanDelay = SCAN_INTERVAL / 2;
        if (golem.getGarrisonState() != GarrisonGolemEntity.IDLE) return false;
        Player near = golem.level().getNearestPlayer(golem, RANGE);
        if (near == null || near.isSpectator() || near.isCrouching() || !golem.hasLineOfSight(near)) {
            return false;
        }
        if (!golem.salute(near, COOLDOWN)) return false;
        player = near;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return player != null && player.isAlive() && ticks < DURATION
                && golem.getGarrisonState() == GarrisonGolemEntity.IDLE;
    }

    @Override
    public void start() {
        ticks = 0;
        golem.getNavigation().stop();
        golem.cue(GarrisonGolemEntity.EVENT_SALUTE);
        golem.playSound(ModSounds.GARRISON_TICK.get(), 1.0f, 1.4f);
    }

    @Override
    public void stop() {
        player = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (player != null) {
            golem.getLookControl().setLookAt(player, 30.0f, 30.0f);
        }
    }
}
