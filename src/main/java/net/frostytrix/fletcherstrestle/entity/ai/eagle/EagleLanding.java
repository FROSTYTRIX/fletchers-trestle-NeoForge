package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bringing an eagle down onto a spot the way a bird does it: in on a descending
 * glide from a distance, then a slow final approach with the wings flared, and
 * an exact touchdown. One instance per goal, since it keeps its own re-path timer.
 */
public class EagleLanding {

    /** Beyond this, along the ground, it's still the cruise toward the glide slope. */
    private static final double FINAL_APPROACH = 10.0;
    /** Inside this it's flaring: the animation's cue. */
    private static final double FLARE_RANGE = 5.0;
    private static final double TOUCHDOWN = 0.3;

    private int repath;

    public void reset() {
        repath = 0;
    }

    /**
     * Flies one tick toward landing with its feet at {@code spot}.
     *
     * @return true once it's down, snapped onto the spot and stopped
     */
    public boolean tick(EagleEntity eagle, Vec3 spot, double speed) {
        Vec3 pos = eagle.position();
        double dx = spot.x - pos.x;
        double dy = spot.y - pos.y;
        double dz = spot.z - pos.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(flat * flat + dy * dy);

        if (distance < TOUCHDOWN || (eagle.onGround() && flat < 0.6 && Math.abs(dy) < 0.6)) {
            eagle.moveTo(spot.x, spot.y, spot.z, eagle.getYRot(), 0.0f);
            eagle.setDeltaMovement(Vec3.ZERO);
            eagle.getNavigation().stop();
            return true;
        }

        if (flat > FINAL_APPROACH) {
            // Cruise to a point above the spot; the higher the further out, so it
            // comes down on a slope rather than dropping in at the end.
            double above = Mth.clamp(2.0 + flat * 0.2, 2.0, 8.0);
            if (--repath <= 0 || eagle.getNavigation().isDone()) {
                eagle.getNavigation().moveTo(spot.x, spot.y + above, spot.z, speed);
                repath = 10;
            }
            return false;
        }

        // Final approach: straight at the spot. The flight control brakes on
        // the way in, so this ends slow and exact.
        eagle.getNavigation().stop();
        eagle.setLanding(distance < FLARE_RANGE);
        double lift = flat > 2.0 ? Math.min(1.0, flat * 0.15) : 0.0;
        eagle.getMoveControl().setWantedPosition(spot.x, spot.y + lift, spot.z, Math.min(speed, 0.9));
        return false;
    }

    /**
     * A good place to land near {@code around}: the top of something solid with
     * room to stand and nothing liquid, preferring high points (a fence post, a
     * tree top, a ridge) the way a raptor picks a lookout. Null if nothing fits.
     */
    @Nullable
    public static Vec3 findPerch(EagleEntity eagle, BlockPos around, int radius, RandomSource random) {
        Level level = eagle.level();
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 20; i++) {
            int x = around.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = around.getZ() + random.nextInt(radius * 2 + 1) - radius;
            // Not right on top of whoever it's landing near.
            if (radius > 2 && Math.abs(x - around.getX()) <= 1 && Math.abs(z - around.getZ()) <= 1) continue;
            BlockPos column = new BlockPos(x, around.getY(), z);
            if (!level.isLoaded(column)) continue;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            Vec3 spot = standOn(eagle, new BlockPos(x, top - 1, z));
            if (spot == null) continue;
            // Higher is better, but not so far above the target that it's out of touch.
            double height = spot.y - around.getY();
            double score = Math.min(height, 6.0) - Math.max(0.0, height - 12.0) + random.nextDouble();
            if (score > bestScore) {
                bestScore = score;
                best = spot;
            }
        }
        return best;
    }

    /** Where the eagle's feet go standing on top of {@code ground}, or null if it can't stand there. */
    @Nullable
    public static Vec3 standOn(EagleEntity eagle, BlockPos ground) {
        Level level = eagle.level();
        BlockState state = level.getBlockState(ground);
        if (!state.getFluidState().isEmpty()) return null;
        VoxelShape shape = state.getCollisionShape(level, ground);
        if (shape.isEmpty()) return null;
        if (!state.isFaceSturdy(level, ground, Direction.UP) && shape.max(Direction.Axis.Y) < 1.0) return null;
        Vec3 spot = new Vec3(ground.getX() + 0.5, ground.getY() + shape.max(Direction.Axis.Y), ground.getZ() + 0.5);
        AABB body = eagle.getDimensions(eagle.getPose()).makeBoundingBox(spot).move(0, 0.01, 0);
        return level.noCollision(eagle, body) ? spot : null;
    }
}
