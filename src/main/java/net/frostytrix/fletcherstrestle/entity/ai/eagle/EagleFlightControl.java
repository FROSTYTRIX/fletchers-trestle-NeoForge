package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Flies the eagle by steering its velocity, the way a bird actually moves: it
 * keeps its momentum, can only change velocity by so much each tick (which is
 * what turns a change of heading into a smooth banked curve), dives far faster
 * than it climbs, and brakes as it closes on where it's going so it arrives
 * instead of overshooting.
 *
 * <p>Before each step it looks ahead along its course; if a block is in the way
 * it tries flying over, then around, before giving up and rising straight up.
 * That handles hills, trees and walls without a path, so the navigation only
 * needs the grid pathfinder for genuinely enclosed routes.</p>
 *
 * <p>Like vanilla's flying control, a wanted position lasts one tick: goals and
 * the navigation set it every tick they want to be moving. With none set, the
 * eagle glides on and slows down, or settles if it has been told to sit.</p>
 */
public class EagleFlightControl extends MoveControl {

    /** Cruising speed in blocks per tick at speed modifier 1 and the default flying speed. */
    private static final double CRUISE = 0.42;
    /** The flying speed attribute the numbers here are tuned for. */
    private static final double BASE_FLYING_SPEED = 0.17;
    private static final double MAX_CLIMB = 0.24;
    private static final double MAX_DIVE = 0.95;
    /** Velocity change per tick. Sets the turning circle: about 5 blocks at cruise. */
    private static final double ACCELERATION = 0.035;
    /** How far ahead to look for obstacles, in ticks of travel at the current speed. */
    private static final double LOOKAHEAD_TICKS = 12.0;
    /** Walking speed along the ground. */
    private static final double WALK = 0.13;
    /** Closer than this along the ground, with ground all the way, a landed eagle walks rather than flies. */
    private static final double WALK_RANGE = 8.0;
    /** Highest step it hops up, and deepest drop it steps down, while walking. */
    private static final double STEP_UP = 1.1;
    private static final double STEP_DOWN = 1.6;
    private static final double TAKEOFF_LIFT = 0.42;

    private final EagleEntity eagle;

    public EagleFlightControl(EagleEntity eagle) {
        super(eagle);
        this.eagle = eagle;
    }

    @Override
    public void tick() {
        if (eagle.isInjured()) {
            tickInjured();
            return;
        }
        if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT;
            Vec3 target = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
            // A landed eagle walks short distances; hopping a step or dropping
            // off a ledge mid-walk still counts as walking.
            boolean onFoot = eagle.onGround() || (!eagle.isFlying() && groundWithin(STEP_DOWN));
            if (onFoot && walkable(target)) {
                walkTo(target);
            } else if (eagle.onGround()) {
                takeOff(target);
            } else {
                steerTo(target);
            }
        } else {
            coast();
        }
    }

    // ---------------------------------------------------------------- flight

    private void steerTo(Vec3 target) {
        Vec3 pos = eagle.position();
        Vec3 to = target.subtract(pos);
        double distance = to.length();

        double cruise = CRUISE * this.speedModifier
                * eagle.getAttributeValue(Attributes.FLYING_SPEED) / BASE_FLYING_SPEED;
        // Brake on the way in: the speed it can still shed before arriving.
        double speed = Math.min(cruise, Math.max(0.04, distance * 0.18));
        Vec3 desired = distance > 1.0E-4 ? to.scale(speed / distance) : Vec3.ZERO;
        double dive = MAX_DIVE * Math.max(1.0, this.speedModifier * 0.6);
        desired = new Vec3(desired.x, Mth.clamp(desired.y, -dive, MAX_CLIMB), desired.z);
        desired = avoidObstacles(pos, desired, distance);

        Vec3 velocity = eagle.getDeltaMovement();
        Vec3 steer = desired.subtract(velocity);
        // Gravity helps a dive: it can pick up speed downward twice as fast.
        double accel = ACCELERATION * (desired.y < -0.2 ? 2.0 : 1.0) * Math.max(1.0, this.speedModifier * 0.7);
        if (steer.lengthSqr() > accel * accel) {
            steer = steer.normalize().scale(accel);
        }
        velocity = velocity.add(steer);
        eagle.setDeltaMovement(velocity);
        face(velocity, to);
    }

    /**
     * The desired velocity, bent around anything solid ahead. Hits beyond the
     * target don't count: a landing spot sits on a block by definition.
     */
    private Vec3 avoidObstacles(Vec3 pos, Vec3 desired, double distance) {
        double speed = desired.length();
        if (speed < 0.03) return desired;
        Vec3 direction = desired.scale(1.0 / speed);
        double reach = Mth.clamp(eagle.getDeltaMovement().length() * LOOKAHEAD_TICKS, 2.0, 8.0);
        reach = Math.min(reach, distance + 0.5);
        if (clear(pos, direction, reach, distance)) return desired;

        float yaw = (float) Math.atan2(direction.z, direction.x);
        double flat = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float pitch = (float) Math.atan2(direction.y, flat);
        float[][] tries = {
                {0f, 0.6f}, {0.6f, 0.35f}, {-0.6f, 0.35f}, {1.2f, 0f}, {-1.2f, 0f}, {0f, 1.1f}, {1.8f, 0.2f}, {-1.8f, 0.2f}
        };
        for (float[] t : tries) {
            Vec3 candidate = fromAngles(yaw + t[0], Math.min(1.45f, pitch + t[1]));
            if (clear(pos, candidate, reach, Double.MAX_VALUE)) {
                return candidate.scale(Math.max(speed, 0.12));
            }
        }
        return new Vec3(0, MAX_CLIMB, 0);
    }

    /** Whether the eagle's body, swept along {@code direction} for {@code reach} blocks, stays clear. */
    private boolean clear(Vec3 pos, Vec3 direction, double reach, double targetDistance) {
        Vec3 offset = direction.scale(reach);
        double half = eagle.getBbWidth() * 0.5;
        Vec3[] starts = {
                pos.add(0, eagle.getBbHeight() * 0.5, 0),
                pos.add(0, 0.1, 0),
                pos.add(0, eagle.getBbHeight() - 0.1, 0),
                pos.add(half, eagle.getBbHeight() * 0.5, 0),
                pos.add(-half, eagle.getBbHeight() * 0.5, 0),
                pos.add(0, eagle.getBbHeight() * 0.5, half),
                pos.add(0, eagle.getBbHeight() * 0.5, -half)
        };
        for (Vec3 start : starts) {
            var hit = eagle.level().clip(new ClipContext(start, start.add(offset),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, eagle));
            if (hit.getType() != HitResult.Type.MISS
                    && hit.getLocation().distanceTo(start) < targetDistance - 0.6) {
                return false;
            }
        }
        return true;
    }

    private static Vec3 fromAngles(float yaw, float pitch) {
        double flat = Math.cos(pitch);
        return new Vec3(Math.cos(yaw) * flat, Math.sin(pitch), Math.sin(yaw) * flat);
    }

    /** No order this tick: glide on, bleeding speed, or settle if told to sit. */
    private void coast() {
        Vec3 v = eagle.getDeltaMovement();
        if (eagle.onGround()) {
            eagle.setDeltaMovement(v.x * 0.5, 0, v.z * 0.5);
            return;
        }
        if (eagle.getEagleState() == EagleEntity.STATE_PERCHED) {
            // On a perch's crossbar there's no floor to stand on: hold still.
            eagle.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (eagle.isOrderedToSit()) {
            // Told to sit in mid-air: drop gently to whatever is below.
            eagle.setDeltaMovement(v.x * 0.8, Math.max(v.y - 0.03, -0.22), v.z * 0.8);
            eagle.setLanding(true);
            return;
        }
        eagle.setDeltaMovement(v.x * 0.94, v.y * 0.85 - 0.004, v.z * 0.94);
        face(v, v);
    }

    // ---------------------------------------------------------------- injured

    /**
     * Too hurt to fly: in the air it glides down, on the ground it limps along
     * at half speed where the ground allows, and never takes off.
     */
    private void tickInjured() {
        boolean moving = this.operation == Operation.MOVE_TO;
        this.operation = Operation.WAIT;
        Vec3 target = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
        Vec3 v = eagle.getDeltaMovement();
        if (!eagle.onGround()) {
            eagle.setDeltaMovement(v.x * 0.85, Math.max(v.y - 0.04, -0.3), v.z * 0.85);
            eagle.setLanding(true);
            return;
        }
        if (moving && walkable(target)) {
            double before = this.speedModifier;
            this.speedModifier = 0.5;
            walkTo(target);
            this.speedModifier = before;
        } else {
            eagle.setDeltaMovement(v.x * 0.5, 0, v.z * 0.5);
        }
    }

    // ---------------------------------------------------------------- ground

    /**
     * Whether the target can be reached on foot: close, near the eagle's own
     * level, and with ground under every step of the way (no gap to fall into,
     * no wall taller than a step).
     */
    public boolean canWalkTo(Vec3 target) {
        return walkable(target);
    }

    private boolean walkable(Vec3 target) {
        Vec3 pos = eagle.position();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double dy = target.y - pos.y;
        if (flat > WALK_RANGE || dy > STEP_UP || dy < -STEP_DOWN) return false;
        int steps = Mth.ceil(flat);
        double y = pos.y;
        for (int i = 1; i <= steps; i++) {
            double t = Math.min(1.0, i / flat);
            double ground = groundHeight(pos.x + dx * t, y, pos.z + dz * t);
            if (Double.isNaN(ground) || ground - y > STEP_UP || y - ground > STEP_DOWN) return false;
            y = ground;
        }
        return true;
    }

    /**
     * The height of the surface a walker would stand on at (x, z), searching from
     * a step above {@code fromY} down to a drop below it. NaN if there's none in range.
     */
    private double groundHeight(double x, double fromY, double z) {
        var level = eagle.level();
        int top = Mth.floor(fromY + STEP_UP);
        int bottom = Mth.floor(fromY - STEP_DOWN) - 1;
        net.minecraft.core.BlockPos.MutableBlockPos cursor = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int by = top; by >= bottom; by--) {
            cursor.set(Mth.floor(x), by, Mth.floor(z));
            var shape = level.getBlockState(cursor).getCollisionShape(level, cursor);
            if (!shape.isEmpty()) {
                double surface = by + shape.max(net.minecraft.core.Direction.Axis.Y);
                // Two blocks of headroom over it, or it's a wall, not a floor.
                cursor.set(Mth.floor(x), Mth.floor(surface), Mth.floor(z));
                boolean room = level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty();
                return room ? surface : Double.NaN;
            }
        }
        return Double.NaN;
    }

    /** Whether there's something solid no more than {@code depth} below its feet. */
    private boolean groundWithin(double depth) {
        var box = eagle.getBoundingBox();
        var probe = new net.minecraft.world.phys.AABB(box.minX, box.minY - depth, box.minZ, box.maxX, box.minY, box.maxZ);
        return !eagle.level().noCollision(eagle, probe);
    }

    private void walkTo(Vec3 target) {
        Vec3 to = target.subtract(eagle.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        double distance = flat.length();
        double fall = eagle.onGround() ? 0.0 : Math.max(eagle.getDeltaMovement().y - 0.08, -0.4);
        if (distance < 0.15) {
            eagle.setDeltaMovement(0, fall, 0);
            return;
        }
        Vec3 velocity = flat.scale(Math.min(WALK * this.speedModifier, distance * 0.5) / distance);
        // Hop up onto a step; otherwise drop with gravity if it walked off an edge.
        double vertical = eagle.horizontalCollision && eagle.onGround() ? 0.42 : fall;
        eagle.setDeltaMovement(velocity.x, vertical, velocity.z);
        face(velocity, to);
    }

    private void takeOff(Vec3 target) {
        Vec3 to = target.subtract(eagle.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        double distance = flat.length();
        Vec3 push = distance > 1.0E-4 ? flat.scale(0.12 / distance) : Vec3.ZERO;
        eagle.setDeltaMovement(push.x, TAKEOFF_LIFT, push.z);
        face(push, to);
    }

    // ---------------------------------------------------------------- orbits

    /**
     * A point a little ahead of the eagle on a circle round {@code center}, at
     * height {@code y}. Chasing it every tick flies the circle; {@code direction}
     * is 1 or -1 for which way round.
     */
    public static Vec3 orbitPoint(Vec3 eaglePos, Vec3 center, double radius, double y, int direction) {
        double angle = Math.atan2(eaglePos.z - center.z, eaglePos.x - center.x) + direction * 0.5;
        return new Vec3(center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius);
    }

    // ---------------------------------------------------------------- facing

    /** Faces the way it's moving, or toward the target when nearly still. */
    private void face(Vec3 velocity, Vec3 fallback) {
        Vec3 heading = velocity.x * velocity.x + velocity.z * velocity.z > 0.0016 ? velocity : fallback;
        if (heading.x * heading.x + heading.z * heading.z < 1.0E-6) return;
        float yaw = (float) (Mth.atan2(heading.z, heading.x) * Mth.RAD_TO_DEG) - 90.0f;
        float turned = this.rotlerp(eagle.getYRot(), yaw, 20.0f);
        eagle.setYRot(turned);
        eagle.yBodyRot = turned;
    }
}
