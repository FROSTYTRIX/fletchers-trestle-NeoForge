package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Hurt, an eagle doesn't run along the ground: it beats hard up and away from
 * whatever hit it, out of reach, before going back to what it was doing.
 */
public class EaglePanicGoal extends Goal {

    private static final int DURATION = 50;
    private static final double FLEE_DISTANCE = 14.0;
    private static final double FLEE_HEIGHT = 9.0;

    private final EagleEntity eagle;
    private Vec3 escape = Vec3.ZERO;
    private int ticks;

    public EaglePanicGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (eagle.isOrderedToSit()) return false;
        boolean hurtRecently = eagle.getLastHurtByMob() != null
                && eagle.tickCount - eagle.getLastHurtByMobTimestamp() < 10;
        // An owner's stray hit isn't a reason to flee.
        if (hurtRecently && eagle.isTame() && eagle.getLastHurtByMob() == eagle.getOwner()) return false;
        return hurtRecently || eagle.isOnFire();
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < DURATION;
    }

    @Override
    public void start() {
        ticks = 0;
        LivingEntity attacker = eagle.getLastHurtByMob();
        Vec3 away = attacker != null
                ? eagle.position().subtract(attacker.position()).multiply(1, 0, 1)
                : Vec3.directionFromRotation(0, eagle.getYRot());
        if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
        escape = eagle.position().add(away.normalize().scale(FLEE_DISTANCE)).add(0, FLEE_HEIGHT, 0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        eagle.getNavigation().stop();
        eagle.getMoveControl().setWantedPosition(escape.x, escape.y, escape.z, 1.6);
    }
}
