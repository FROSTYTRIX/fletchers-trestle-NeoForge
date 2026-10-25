package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.block.entity.EmplacementBlockEntity;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Off duty, a warden potters about its garrison: now and then it walks up to
 * one of its armed posts and fiddles with the crossbow for a while, as if
 * checking it over.
 */
public class TinkerGoal extends Goal {

    /** Chance per check (every other tick) of going to tinker: about every half minute. */
    private static final int ODDS = 300;
    private static final int GIVE_UP = 200;
    private static final double ARRIVED = 1.3;

    private final GarrisonGolemEntity golem;
    @Nullable
    private BlockPos postPos;
    private int ticks;
    private int working;
    private int workFor;

    public TinkerGoal(GarrisonGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (golem.getGarrisonState() != GarrisonGolemEntity.IDLE || golem.getRandom().nextInt(ODDS) != 0) {
            return false;
        }
        if (!(golem.level() instanceof ServerLevel level)) return false;
        List<EmplacementBlockEntity> posts = CrewPostGoal.nearbyPosts(golem, level).stream()
                .filter(post -> !post.getCrossbow().isEmpty() && !post.isCrewedByOther(golem))
                .toList();
        if (posts.isEmpty()) return false;
        postPos = posts.get(golem.getRandom().nextInt(posts.size())).getBlockPos();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return post() != null && ticks < GIVE_UP + workFor && working < workFor;
    }

    @Override
    public void start() {
        ticks = 0;
        working = 0;
        workFor = 60 + golem.getRandom().nextInt(80);
        Vec3 spot = spot();
        if (spot != null) {
            golem.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.6);
        }
    }

    @Override
    public void stop() {
        if (golem.getGarrisonState() == GarrisonGolemEntity.TINKERING) {
            golem.setGarrisonState(GarrisonGolemEntity.IDLE);
        }
        golem.getNavigation().stop();
        postPos = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        EmplacementBlockEntity post = post();
        Vec3 spot = spot();
        if (post == null || spot == null) return;
        if (golem.position().distanceToSqr(spot.x, golem.getY(), spot.z) > ARRIVED * ARRIVED) {
            if (golem.getNavigation().isDone()) {
                golem.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.6);
            }
            return;
        }
        golem.getNavigation().stop();
        golem.setGarrisonState(GarrisonGolemEntity.TINKERING);
        Vec3 crossbow = post.muzzle();
        golem.getLookControl().setLookAt(crossbow.x, crossbow.y, crossbow.z);
        working++;
    }

    @Nullable
    private EmplacementBlockEntity post() {
        if (postPos == null) return null;
        return golem.level().getBlockEntity(postPos) instanceof EmplacementBlockEntity post ? post : null;
    }

    @Nullable
    private Vec3 spot() {
        EmplacementBlockEntity post = post();
        return post != null ? post.standingSpot() : null;
    }
}
