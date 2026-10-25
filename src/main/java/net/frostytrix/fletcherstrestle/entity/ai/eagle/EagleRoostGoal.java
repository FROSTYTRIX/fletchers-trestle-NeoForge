package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Eagles fly in good daylight and nothing else. At dusk a roaming eagle heads
 * for its nest, or a high lookout near home, and roosts there till morning; in
 * the rain it comes down and waits it out; in a thunderstorm it gets under the
 * leaves of a tree.
 */
public class EagleRoostGoal extends Goal {

    private static final int SEARCH_RADIUS = 14;
    private static final int GIVE_UP_TICKS = 600;

    private final EagleEntity eagle;
    private final EagleLanding landing = new EagleLanding();
    @Nullable
    private Vec3 spot;
    private boolean down;
    private int ticks;
    private int searchCooldown;

    public EagleRoostGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (eagle.isOrderedToSit() || eagle.getEagleState() != EagleEntity.STATE_IDLE) return false;
        if (!eagle.wantsToRoam() || !eagle.shouldShelter()) return false;
        if (--searchCooldown > 0) return false;
        searchCooldown = 40;
        spot = findSpot();
        return spot != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !eagle.isOrderedToSit()
                && eagle.getEagleState() == EagleEntity.STATE_IDLE
                && eagle.wantsToRoam()
                && eagle.shouldShelter()
                && spot != null
                && (down || ticks < GIVE_UP_TICKS);
    }

    @Override
    public void start() {
        down = false;
        ticks = 0;
        landing.reset();
    }

    @Override
    public void stop() {
        spot = null;
        eagle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        if (spot == null || down) return;
        if (landing.tick(eagle, spot, 1.0)) {
            down = true;
        }
    }

    @Nullable
    private Vec3 findSpot() {
        BlockPos home = BlockPos.containing(eagle.getHome());
        if (eagle.level().isThundering()) {
            Vec3 shelter = findShelter(home);
            if (shelter != null) return shelter;
        }
        BlockPos nest = eagle.getNestPos();
        if (nest != null && eagle.level().isLoaded(nest)) {
            Vec3 rim = new Vec3(nest.getX() + 0.5, nest.getY() + 4.0 / 16.0, nest.getZ() + 0.5);
            if (eagle.level().noCollision(eagle, eagle.getDimensions(eagle.getPose()).makeBoundingBox(rim).move(0, 0.01, 0))) {
                return rim;
            }
        }
        return EagleLanding.findPerch(eagle, home, SEARCH_RADIUS, eagle.getRandom());
    }

    /** Ground under a canopy: a column whose leaves sit above where it can stand. */
    @Nullable
    private Vec3 findShelter(BlockPos around) {
        var level = eagle.level();
        for (int i = 0; i < 24; i++) {
            int x = around.getX() + eagle.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = around.getZ() + eagle.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            if (!level.isLoaded(new BlockPos(x, around.getY(), z))) continue;
            int canopy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (canopy - floor < 3) continue;
            Vec3 standing = EagleLanding.standOn(eagle, new BlockPos(x, floor - 1, z));
            if (standing != null) return standing;
        }
        return null;
    }
}
