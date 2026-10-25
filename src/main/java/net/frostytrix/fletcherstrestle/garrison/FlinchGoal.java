package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A player's blow makes the warden cower and back off a few steps, keeping an
 * eye on them. It never hits back: it has nothing to hit with, and players
 * aren't its business.
 */
public class FlinchGoal extends Goal {

    private static final int DURATION = 30;
    private static final double SPEED = 1.1;

    private final GarrisonGolemEntity golem;
    @Nullable
    private Player from;
    private int ticks;

    public FlinchGoal(GarrisonGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return golem.flinchingFrom() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return from != null && from.isAlive() && ticks < DURATION;
    }

    @Override
    public void start() {
        backOff();
    }

    @Override
    public void stop() {
        from = null;
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        // Hit again while backing away: start over.
        if (golem.flinchingFrom() != null) {
            backOff();
        }
        if (from != null) {
            golem.getLookControl().setLookAt(from, 30.0f, 30.0f);
        }
    }

    private void backOff() {
        from = golem.flinchingFrom();
        golem.clearFlinch();
        ticks = 0;
        if (from == null) return;
        Vec3 away = DefaultRandomPos.getPosAway(golem, 5, 3, from.position());
        if (away != null) {
            golem.getNavigation().moveTo(away.x, away.y, away.z, SPEED);
        }
    }
}
