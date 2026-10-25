package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.block.entity.EagleNestBlockEntity;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A wild eagle with eggs or eaglets in its nest won't let a player near it.
 * Come within {@link #ALARM_RANGE} blocks of the nest and it screeches and
 * circles; its first dives are bluffs that pull up just over your head. Stay
 * close to the nest and the dives start landing.
 */
public class EagleDefendNestGoal extends Goal {

    private enum Phase { CIRCLE, DIVE, PULL_UP }

    private static final double ALARM_RANGE = 10.0;
    /** Inside this, after the warnings, the dives are real. */
    private static final double STRIKE_RANGE = 6.0;
    private static final double GIVE_UP_RANGE = 16.0;
    private static final int WARNING_DIVES = 2;
    private static final int MAX_TICKS = 800;

    private final EagleEntity eagle;
    @Nullable
    private Player intruder;
    private Phase phase = Phase.CIRCLE;
    private int phaseTicks;
    private int ticks;
    private int dives;
    private int cooldown;
    /** Whether a real strike landed on this intruder. */
    private boolean struck;
    private int scanDelay;

    public EagleDefendNestGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (eagle.isTame() || eagle.isBaby() || eagle.isInjured() || eagle.getNestPos() == null) return false;
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        // canUse runs every other tick on an id-dependent parity, so count rather than use tickCount.
        if (--scanDelay > 0) return false;
        scanDelay = 5;
        if (!hasYoung()) return false;
        BlockPos nest = eagle.getNestPos();
        intruder = eagle.level().getNearestPlayer(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, ALARM_RANGE,
                p -> p instanceof Player player && !player.isCreative() && !player.isSpectator());
        return intruder != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (intruder == null || !intruder.isAlive() || intruder.isCreative() || intruder.isSpectator()) return false;
        BlockPos nest = eagle.getNestPos();
        return nest != null && ticks < MAX_TICKS
                && intruder.distanceToSqr(Vec3.atCenterOf(nest)) < GIVE_UP_RANGE * GIVE_UP_RANGE;
    }

    @Override
    public void start() {
        phase = Phase.CIRCLE;
        phaseTicks = 0;
        ticks = 0;
        dives = 0;
        struck = false;
        eagle.playSound(ModSounds.EAGLE_AMBIENT.get(), 1.6f, 0.75f);
    }

    @Override
    public void stop() {
        if (struck && intruder != null && intruder.isAlive()
                && intruder instanceof net.minecraft.server.level.ServerPlayer player) {
            net.frostytrix.fletcherstrestle.progression.ModCriteria.EAGLE.get().trigger(player,
                    net.frostytrix.fletcherstrestle.progression.EagleTrigger.NEST_DEFENCE);
        }
        intruder = null;
        cooldown = 200;
        eagle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player target = intruder;
        if (target == null) return;
        ticks++;
        phaseTicks++;
        boolean warning = dives < WARNING_DIVES;
        Vec3 head = target.position().add(0, target.getBbHeight() + (warning ? 1.6 : 0.0), 0);

        switch (phase) {
            case CIRCLE -> {
                Vec3 point = EagleFlightControl.orbitPoint(eagle.position(), target.position(), 7.0, target.getY() + 7.0, 1);
                eagle.getNavigation().stop();
                eagle.getMoveControl().setWantedPosition(point.x, point.y, point.z, 1.2);
                if (phaseTicks > 40 && eagle.getY() > target.getY() + 4.0) {
                    enter(Phase.DIVE);
                    eagle.playSound(ModSounds.EAGLE_AMBIENT.get(), 1.4f, 0.8f);
                    eagle.playSound(ModSounds.EAGLE_DIVE.get(), 1.0f, 1.1f);
                }
            }
            case DIVE -> {
                eagle.getNavigation().stop();
                eagle.getMoveControl().setWantedPosition(head.x, head.y, head.z, 2.2);
                if (eagle.position().distanceToSqr(head) < 1.5 * 1.5) {
                    boolean nearNest = target.distanceToSqr(Vec3.atCenterOf(eagle.getNestPos())) < STRIKE_RANGE * STRIKE_RANGE;
                    if (!warning && nearNest && eagle.doHurtTarget(target)) {
                        struck = true;
                    }
                    dives++;
                    enter(Phase.PULL_UP);
                } else if (phaseTicks > 60) {
                    enter(Phase.PULL_UP);
                }
            }
            case PULL_UP -> {
                Vec3 v = eagle.getDeltaMovement().multiply(1, 0, 1);
                Vec3 ahead = v.lengthSqr() > 1.0E-4 ? v.normalize().scale(7.0) : Vec3.ZERO;
                Vec3 point = eagle.position().add(ahead).add(0, 6.0, 0);
                eagle.getMoveControl().setWantedPosition(point.x, point.y, point.z, 1.3);
                if (phaseTicks > 20) enter(Phase.CIRCLE);
            }
        }
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    /** Eggs in the nest, or eaglets nearby. */
    private boolean hasYoung() {
        BlockPos nest = eagle.getNestPos();
        if (nest == null || !eagle.level().isLoaded(nest)) return false;
        if (eagle.level().getBlockEntity(nest) instanceof EagleNestBlockEntity block && block.eggCount() > 0) {
            return true;
        }
        return !eagle.level().getEntitiesOfClass(EagleEntity.class, new AABB(nest).inflate(10.0), EagleEntity::isBaby).isEmpty();
    }
}
