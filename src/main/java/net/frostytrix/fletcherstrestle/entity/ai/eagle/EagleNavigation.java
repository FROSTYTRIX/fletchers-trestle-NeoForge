package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Navigation for a bird that mostly has open sky to work with.
 *
 * <p>Vanilla's flying navigation runs the grid pathfinder for every trip, then
 * follows the result node by node, which is both costly and why flying mobs
 * zigzag. Here a trip with a clear line of flight skips the pathfinder and hands
 * the destination straight to {@link EagleFlightControl}, whose look-ahead deals
 * with the odd tree or hill on the way. Only a blocked line (a cave, a roofed
 * yard) gets a real path, and that path is smoothed: the eagle always heads for
 * the furthest node it can see, so it cuts corners instead of tracing the grid.</p>
 */
public class EagleNavigation extends FlyingPathNavigation {

    /** Closer than this to a direct destination counts as there. */
    private static final double ARRIVED = 0.5;

    @Nullable
    private Vec3 direct;
    private double directSpeed;

    public EagleNavigation(EagleEntity eagle, Level level) {
        super(eagle, level);
        this.setCanOpenDoors(false);
        this.setCanFloat(true);
        this.setCanPassDoors(false);
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        return moveTo(x, y, z, 1, speed);
    }

    @Override
    public boolean moveTo(double x, double y, double z, int accuracy, double speed) {
        Vec3 goal = new Vec3(x, y, z);
        if (lineOfFlight(goal)) {
            this.path = null;
            this.direct = goal;
            this.directSpeed = speed;
            return true;
        }
        this.direct = null;
        return super.moveTo(x, y, z, accuracy, speed);
    }

    @Override
    public boolean moveTo(Entity entity, double speed) {
        return moveTo(entity.getX(), entity.getY(), entity.getZ(), speed);
    }

    @Override
    public boolean moveTo(@Nullable Path path, double speed) {
        this.direct = null;
        return super.moveTo(path, speed);
    }

    @Override
    public void tick() {
        if (this.direct != null) {
            this.tick++;
            if (this.mob.position().distanceTo(this.direct) < ARRIVED) {
                this.direct = null;
                return;
            }
            this.mob.getMoveControl().setWantedPosition(this.direct.x, this.direct.y, this.direct.z, this.directSpeed);
            return;
        }
        super.tick();
    }

    /** Head for the furthest upcoming node in sight, so a path is flown as a curve. */
    @Override
    protected void followThePath() {
        Path current = this.path;
        if (current != null) {
            for (int i = current.getNodeCount() - 1; i > current.getNextNodeIndex(); i--) {
                Node node = current.getNode(i);
                if (lineOfFlight(new Vec3(node.x + 0.5, node.y, node.z + 0.5))) {
                    current.setNextNodeIndex(i);
                    break;
                }
            }
        }
        super.followThePath();
    }

    @Override
    public boolean isDone() {
        return this.direct == null && super.isDone();
    }

    @Override
    public void stop() {
        this.direct = null;
        super.stop();
    }

    /** Whether the eagle can fly straight there: rays from its middle, top and feet all reach. */
    private boolean lineOfFlight(Vec3 goal) {
        Vec3 pos = this.mob.position();
        double height = this.mob.getBbHeight();
        double[] heights = {height * 0.5, 0.1, height - 0.1};
        double distance = pos.distanceTo(goal);
        for (double h : heights) {
            Vec3 start = pos.add(0, h, 0);
            Vec3 end = goal.add(0, h, 0);
            var hit = this.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this.mob));
            if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(start) < distance - 0.6) {
                return false;
            }
        }
        return true;
    }
}
