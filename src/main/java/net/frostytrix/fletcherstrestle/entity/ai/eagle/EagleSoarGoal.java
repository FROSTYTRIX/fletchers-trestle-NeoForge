package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * What an eagle does with nothing to do: ride thermals. It circles wide and
 * slow, climbing as it goes, until it's high over its territory; then it glides
 * off in a long straight line to the next thermal; and now and then it comes
 * down to rest a while, on its nest if it has one, otherwise on a high lookout.
 * Over a steep mountainside it rides the updraft instead, beating back and
 * forth along the slope just above the crest.
 *
 * <p>Runs for wild eagles, and for tame ones whose owner isn't around and that
 * have no perch to go to. The territory is centred on the nest, or on the
 * eagle's home.</p>
 */
public class EagleSoarGoal extends Goal {

    private enum Phase { THERMAL, RIDGE, GLIDE, REST }

    /** Height difference across a slope, in blocks, that makes it worth ridge soaring. */
    private static final double RIDGE_RELIEF = 7.0;
    private static final double RIDGE_SPAN = 16.0;

    private static final double TERRITORY = 32.0;
    private static final int MIN_THERMAL_TICKS = 300;
    private static final int MAX_THERMAL_TICKS = 700;
    private static final int MIN_REST_TICKS = 200;
    private static final int MAX_REST_TICKS = 700;
    /** Chance, at the end of a thermal, of going down to rest instead of gliding on. */
    private static final float REST_CHANCE = 0.35f;

    private final EagleEntity eagle;
    private final EagleLanding landing = new EagleLanding();
    private Phase phase = Phase.THERMAL;
    private Vec3 center = Vec3.ZERO;
    private double radius;
    private double altitude;
    private int direction = 1;
    private int phaseTicks;
    @Nullable
    private Vec3 restSpot;
    private boolean resting;
    private int approachTicks;
    /** Ridge soaring: the line along the slope, and how high above the crest. */
    private Vec3 ridgeAlong = Vec3.ZERO;
    private Vec3 ridgeDownhill = Vec3.ZERO;
    private double ridgeAltitude;
    private int ridgeSign = 1;

    public EagleSoarGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !eagle.isOrderedToSit()
                && eagle.getEagleState() == EagleEntity.STATE_IDLE
                && eagle.wantsToRoam();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        startThermal(eagle.position());
    }

    @Override
    public void stop() {
        resting = false;
        restSpot = null;
        eagle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        phaseTicks--;
        switch (phase) {
            case THERMAL -> tickThermal();
            case RIDGE -> tickRidge();
            case GLIDE -> tickGlide();
            case REST -> tickRest();
        }
    }

    private void tickThermal() {
        Vec3 pos = eagle.position();
        // Climb as it circles: aim a little above where it is, up to the ceiling.
        double y = Math.min(altitude, pos.y + 1.2);
        if (pos.y > altitude + 2.0) y = altitude;
        Vec3 point = EagleFlightControl.orbitPoint(pos, center, radius, y, direction);
        eagle.getMoveControl().setWantedPosition(point.x, point.y, point.z, 0.8);

        if (phaseTicks <= 0) {
            endSoaring();
        }
    }

    /**
     * Back and forth along the slope, a few blocks over the crest and a little
     * out over the downhill side where the wind is pushed up.
     */
    private void tickRidge() {
        Vec3 pos = eagle.position();
        double along = pos.subtract(center).dot(ridgeAlong);
        if (along > RIDGE_SPAN) ridgeSign = -1;
        if (along < -RIDGE_SPAN) ridgeSign = 1;
        Vec3 point = center.add(ridgeAlong.scale(along + ridgeSign * 8.0)).add(ridgeDownhill.scale(3.0));
        double y = ridgeAltitude + Math.sin(eagle.tickCount * 0.05) * 1.0;
        eagle.getMoveControl().setWantedPosition(point.x, y, point.z, 0.65);
        if (phaseTicks <= 0) {
            endSoaring();
        }
    }

    /** End of a thermal or a ridge: rest a while, or glide on to the next one. */
    private void endSoaring() {
        if (eagle.getRandom().nextFloat() < REST_CHANCE && (restSpot = findRestSpot()) != null) {
            phase = Phase.REST;
            resting = false;
            approachTicks = 0;
            landing.reset();
        } else {
            center = pickThermalCenter();
            phase = Phase.GLIDE;
            phaseTicks = 400;
        }
    }

    /**
     * Whether the ground around here falls away steeply enough to ride; if so,
     * sets up the ridge: the contour line to fly, and the downhill side.
     */
    private boolean tryRidge(Vec3 around) {
        double highest = Double.NEGATIVE_INFINITY;
        double lowest = Double.POSITIVE_INFINITY;
        Vec3 uphill = Vec3.ZERO;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            double h = groundAt(around.add(dir.scale(10.0)));
            uphill = uphill.add(dir.scale(h));
            highest = Math.max(highest, h);
            lowest = Math.min(lowest, h);
        }
        if (highest - lowest < RIDGE_RELIEF || uphill.lengthSqr() < 1.0E-4) return false;
        Vec3 up = uphill.normalize();
        ridgeDownhill = up.scale(-1);
        ridgeAlong = new Vec3(-up.z, 0, up.x);
        ridgeAltitude = highest + 5.0;
        ridgeSign = eagle.getRandom().nextBoolean() ? 1 : -1;
        return true;
    }

    /** A long, straight glide to the next thermal, losing a little height. */
    private void tickGlide() {
        Vec3 pos = eagle.position();
        double flat = Math.hypot(center.x - pos.x, center.z - pos.z);
        double y = Math.max(groundAt(center) + 8.0, pos.y - 0.3);
        eagle.getMoveControl().setWantedPosition(center.x, y, center.z, 1.0);
        if (flat < radius || phaseTicks <= 0) {
            startThermal(center);
        }
    }

    private void tickRest() {
        if (restSpot == null) {
            startThermal(eagle.position());
            return;
        }
        if (!resting) {
            if (landing.tick(eagle, restSpot, 1.0)) {
                resting = true;
                phaseTicks = MIN_REST_TICKS + eagle.getRandom().nextInt(MAX_REST_TICKS - MIN_REST_TICKS);
            } else if (++approachTicks > 600) {
                // Couldn't get down: give up and go back up.
                startThermal(eagle.position());
            }
            return;
        }
        // Down and resting: sit tight until it's time to go.
        if (phaseTicks <= 0) {
            startThermal(eagle.position());
        }
    }

    private void startThermal(Vec3 around) {
        RandomSource random = eagle.getRandom();
        phase = Phase.THERMAL;
        resting = false;
        restSpot = null;
        center = new Vec3(around.x + random.nextGaussian() * 4.0, 0, around.z + random.nextGaussian() * 4.0);
        radius = 9.0 + random.nextDouble() * 7.0;
        direction = random.nextBoolean() ? 1 : -1;
        altitude = groundAt(center) + 16.0 + random.nextDouble() * 14.0;
        BlockPos nest = eagle.getNestPos();
        if (nest != null) {
            altitude = Math.max(altitude, nest.getY() + 12.0);
        }
        phaseTicks = MIN_THERMAL_TICKS + random.nextInt(MAX_THERMAL_TICKS - MIN_THERMAL_TICKS);
        // On a mountainside, ride the slope instead of circling.
        if (random.nextFloat() < 0.6f && tryRidge(center)) {
            phase = Phase.RIDGE;
        }
    }

    /** Somewhere else over the territory, so the eagle ranges rather than parks. */
    private Vec3 pickThermalCenter() {
        Vec3 home = eagle.getHome();
        RandomSource random = eagle.getRandom();
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 8.0 + random.nextDouble() * TERRITORY;
            Vec3 candidate = new Vec3(home.x + Math.cos(angle) * distance, 0, home.z + Math.sin(angle) * distance);
            if (eagle.level().isLoaded(BlockPos.containing(candidate))) {
                return candidate;
            }
        }
        return new Vec3(home.x, 0, home.z);
    }

    /** The nest rim if it has a nest, otherwise a high point near home. */
    @Nullable
    private Vec3 findRestSpot() {
        BlockPos nest = eagle.getNestPos();
        if (nest != null && eagle.level().isLoaded(nest)) {
            // The nest is a shallow bowl: it lands in it, on the rim's height.
            Vec3 rim = new Vec3(nest.getX() + 0.5, nest.getY() + 4.0 / 16.0, nest.getZ() + 0.5);
            if (eagle.level().noCollision(eagle, eagle.getDimensions(eagle.getPose()).makeBoundingBox(rim).move(0, 0.01, 0))) {
                return rim;
            }
        }
        return EagleLanding.findPerch(eagle, BlockPos.containing(eagle.getHome()), 12, eagle.getRandom());
    }

    private double groundAt(Vec3 point) {
        BlockPos column = BlockPos.containing(point);
        if (!eagle.level().isLoaded(column)) return eagle.getY() - 16.0;
        return eagle.level().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
    }
}
