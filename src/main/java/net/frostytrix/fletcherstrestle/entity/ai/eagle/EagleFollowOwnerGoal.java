package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A tame eagle keeping company with its owner, falconry style: it circles
 * overhead while they're on the move, and once they stop, it comes down to a
 * lookout nearby (a post, a branch, the top of a wall) and waits there until
 * they set off again. Under a roof or underground there's no room to circle, so
 * it stays close and lands sooner.
 *
 * <p>When the owner takes off on an elytra it flies as their wingman, matching
 * their speed off one wingtip. A whistle recall brings it straight in to land
 * beside the owner. Left far behind, it catches up by teleporting, as vanilla
 * pets do.</p>
 */
public class EagleFollowOwnerGoal extends Goal {

    private enum Phase { ESCORT, SETTLE, WAIT }

    private static final double TELEPORT = 48.0;
    /** Further than this it flies straight to the owner instead of circling. */
    private static final double CATCH_UP = 16.0;
    /** A waiting eagle stays put until the owner is this far away. */
    private static final double LEAVE_PERCH = 12.0;
    /** A waiting eagle walks back over once the owner is this far from it. */
    private static final double WALK_UP = 3.5;
    /** Ticks the owner must stand still before the eagle comes down. */
    private static final int SETTLE_AFTER = 60;
    private static final int SETTLE_AFTER_INDOORS = 20;
    /** Wingman: how far off the owner's wingtip, and how far behind before it teleports up. */
    private static final double WING_OFFSET = 2.6;
    private static final double WING_LOST = 28.0;

    private final EagleEntity eagle;
    private final EagleLanding landing = new EagleLanding();
    private Phase phase = Phase.ESCORT;
    @Nullable
    private Vec3 spot;
    private int stillTicks;
    private int direction = 1;
    private int repath;
    private Vec3 lastOwnerPos = Vec3.ZERO;
    private Vec3 ownerStep = Vec3.ZERO;
    /** Elytra distance flown with the eagle at the owner's wing, toward the advancement. */
    private double wingDistance;
    private boolean wingAwarded;
    private int searchCooldown;

    public EagleFollowOwnerGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = presentOwner();
        if (owner == null || eagle.isOrderedToSit()) return false;
        return eagle.getEagleState() == EagleEntity.STATE_IDLE && !eagle.wantsToRoam();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        phase = eagle.onGround() ? Phase.WAIT : Phase.ESCORT;
        stillTicks = 0;
        repath = 0;
        spot = null;
        direction = eagle.getRandom().nextBoolean() ? 1 : -1;
        LivingEntity owner = presentOwner();
        lastOwnerPos = owner != null ? owner.position() : eagle.position();
        ownerStep = Vec3.ZERO;
        landing.reset();
    }

    @Override
    public void stop() {
        eagle.getNavigation().stop();
        spot = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity owner = presentOwner();
        if (owner == null) return;
        double distance = eagle.distanceTo(owner);
        if (distance > TELEPORT) {
            teleportNear(owner);
            return;
        }

        Vec3 ownerPos = owner.position();
        // A player's velocity isn't tracked on the server, so read it from the step it took.
        ownerStep = ownerStep.lerp(ownerPos.subtract(lastOwnerPos), 0.2);
        boolean moving = ownerPos.distanceToSqr(lastOwnerPos) > 0.0025;
        lastOwnerPos = ownerPos;
        stillTicks = moving ? 0 : stillTicks + 1;
        boolean indoors = !eagle.level().canSeeSky(owner.blockPosition().above());

        if (owner.isFallFlying()) {
            wingman(owner);
            phase = Phase.ESCORT;
            return;
        }

        if (eagle.isRecalled()) {
            if (phase != Phase.SETTLE || spot == null) {
                spot = besideOwner(owner);
                phase = Phase.SETTLE;
                landing.reset();
            }
        }

        switch (phase) {
            case WAIT -> {
                if (distance > LEAVE_PERCH) {
                    phase = Phase.ESCORT;
                } else if (distance > WALK_UP && eagle.onGround()
                        && eagle.getMoveControl() instanceof EagleFlightControl flight) {
                    // Close by and the ground connects: walk over rather than take
                    // wing. Up on a lookout, it just keeps waiting there.
                    Vec3 to = eagle.position().subtract(ownerPos).multiply(1, 0, 1);
                    Vec3 stop = to.lengthSqr() > 1.0E-4 ? ownerPos.add(to.normalize().scale(WALK_UP - 0.5)) : ownerPos;
                    stop = new Vec3(stop.x, ownerPos.y, stop.z);
                    if (flight.canWalkTo(stop)) {
                        eagle.getMoveControl().setWantedPosition(stop.x, stop.y, stop.z, 1.0);
                    }
                }
            }
            case SETTLE -> {
                if (spot == null || (moving && !eagle.isRecalled())) {
                    phase = Phase.ESCORT;
                    spot = null;
                } else if (landing.tick(eagle, spot, eagle.isRecalled() ? 1.6 : 1.0)) {
                    phase = Phase.WAIT;
                    // A call to say it's here.
                    if (eagle.isRecalled() || distance < 4.0) {
                        eagle.playSound(net.frostytrix.fletcherstrestle.sound.ModSounds.EAGLE_AMBIENT.get(), 0.8f, 1.15f);
                    }
                    eagle.clearRecall();
                }
            }
            case ESCORT -> {
                int settleAfter = indoors ? SETTLE_AFTER_INDOORS : SETTLE_AFTER;
                if (stillTicks > settleAfter && distance < CATCH_UP && --searchCooldown <= 0) {
                    searchCooldown = 20;
                    spot = EagleLanding.findPerch(eagle, owner.blockPosition(), 6, eagle.getRandom());
                    if (spot != null) {
                        phase = Phase.SETTLE;
                        landing.reset();
                        break;
                    }
                }
                escort(owner, distance, indoors);
            }
        }
    }

    /** Circle above the owner, a little ahead of where they're heading. */
    private void escort(LivingEntity owner, double distance, boolean indoors) {
        if (indoors || distance > CATCH_UP) {
            if (--repath <= 0 || eagle.getNavigation().isDone()) {
                double up = indoors ? 1.5 : 5.0;
                eagle.getNavigation().moveTo(owner.getX(), owner.getY() + up, owner.getZ(),
                        distance > CATCH_UP ? 1.5 : 1.0);
                repath = 10;
            }
            return;
        }
        Vec3 lead = ownerStep.multiply(10.0, 0.0, 10.0);
        Vec3 center = owner.position().add(lead);
        double altitude = owner.getY() + 6.0 + Mth.sin(eagle.tickCount * 0.02f) * 1.5f;
        Vec3 point = EagleFlightControl.orbitPoint(eagle.position(), center, 7.0, altitude, direction);
        eagle.getNavigation().stop();
        eagle.getMoveControl().setWantedPosition(point.x, point.y, point.z, 1.0);
    }

    /**
     * Elytra flight: hold station a little behind and off the right wingtip,
     * at whatever speed the owner is making.
     */
    private void wingman(LivingEntity owner) {
        Vec3 forward = ownerStep.lengthSqr() > 1.0E-4 ? ownerStep.normalize() : owner.getLookAngle();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        Vec3 slot = owner.position().add(right.normalize().scale(WING_OFFSET)).add(forward.scale(-1.0)).add(ownerStep.scale(4.0));
        if (eagle.distanceToSqr(slot) > WING_LOST * WING_LOST) {
            Vec3 behind = owner.position().subtract(forward.scale(4.0)).add(right.scale(WING_OFFSET));
            if (eagle.level().noCollision(eagle, eagle.getDimensions(eagle.getPose()).makeBoundingBox(behind))) {
                eagle.moveTo(behind.x, behind.y, behind.z, eagle.getYRot(), 0.0f);
                eagle.setDeltaMovement(ownerStep);
            }
            return;
        }
        if (!wingAwarded && eagle.distanceToSqr(slot) < 10.0 * 10.0) {
            wingDistance += ownerStep.length();
            if (wingDistance >= 1000.0 && owner instanceof net.minecraft.server.level.ServerPlayer player) {
                net.frostytrix.fletcherstrestle.progression.ModCriteria.EAGLE.get().trigger(player,
                        net.frostytrix.fletcherstrestle.progression.EagleTrigger.WINGMAN);
                wingAwarded = true;
            }
        }
        // Speed to match: cruise is 0.42 blocks a tick at modifier 1, plus some to close up.
        double speed = Mth.clamp(ownerStep.length() / 0.42 * 1.25, 1.0, 6.0);
        eagle.getNavigation().stop();
        eagle.getMoveControl().setWantedPosition(slot.x, slot.y, slot.z, speed);
    }

    /** A spot right next to the owner, for a recall. Falls back to their feet's level beside them. */
    private Vec3 besideOwner(LivingEntity owner) {
        Vec3 near = EagleLanding.findPerch(eagle, owner.blockPosition(), 2, eagle.getRandom());
        if (near != null) return near;
        Vec3 side = owner.getLookAngle().yRot((float) Math.PI / 2).multiply(1.2, 0, 1.2);
        return owner.position().add(side);
    }

    private void teleportNear(LivingEntity owner) {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 12; i++) {
            int dx = eagle.getRandom().nextInt(7) - 3;
            int dz = eagle.getRandom().nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
            BlockPos at = base.offset(dx, 2 + eagle.getRandom().nextInt(3), dz);
            Vec3 pos = Vec3.atBottomCenterOf(at);
            if (eagle.level().noCollision(eagle, eagle.getDimensions(eagle.getPose()).makeBoundingBox(pos))
                    && eagle.level().getFluidState(at).isEmpty()) {
                eagle.moveTo(pos.x, pos.y, pos.z, eagle.getYRot(), 0.0f);
                eagle.setDeltaMovement(Vec3.ZERO);
                eagle.getNavigation().stop();
                phase = Phase.ESCORT;
                return;
            }
        }
    }

    @Nullable
    private LivingEntity presentOwner() {
        if (!eagle.isTame()) return null;
        LivingEntity owner = eagle.getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) return null;
        if (owner.level().dimension() != eagle.level().dimension()) return null;
        return owner;
    }
}
