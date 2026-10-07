package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.block.entity.EmplacementBlockEntity;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The golem's job: find a loaded emplacement that can see a hostile mob, run to
 * it, and work the crossbow until nothing is left in its cone.
 *
 * <p>Loading takes the crossbow's own charge time, so tuning and Quick Charge
 * make a faster post. A magazine loads several bolts and fires them in a burst.
 * The golem holds fire while anything that isn't hostile stands in the line.</p>
 */
public class CrewPostGoal extends Goal {

    private static final int SCAN_INTERVAL = 10;
    /** A post with nothing to shoot at is left after this long. */
    private static final int STAND_DOWN_TICKS = 60;
    /** How close to the post the golem has to be to work it. */
    private static final double REACH = 1.9;
    /** Bolt speed before the string's multiplier: a player's crossbow shot. */
    private static final float BOLT_SPEED = 3.15f;
    private static final float INACCURACY = 1.0f;
    /** Ticks between bolts of a magazine burst. */
    private static final int BURST_INTERVAL = 8;
    /** Arrow gravity per tick, for the drop the aim makes up for. */
    private static final double GRAVITY = 0.05;

    private final GarrisonGolemEntity golem;
    @Nullable
    private BlockPos postPos;
    @Nullable
    private LivingEntity target;
    private int idle;
    private int charge;
    private int cooldown;
    private int repath;
    private int scanDelay;

    public CrewPostGoal(GarrisonGolemEntity golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // canUse runs every other tick, offset by the entity id, so a tickCount
        // modulo would never fire for half the golems; a countdown always does.
        if (--scanDelay > 0 || !(golem.level() instanceof ServerLevel level)) {
            return false;
        }
        scanDelay = SCAN_INTERVAL / 2;
        BlockPos best = null;
        LivingEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        for (EmplacementBlockEntity post : nearbyPosts(level)) {
            if (!post.isReady() || post.isCrewedByOther(golem)) continue;
            LivingEntity found = findTarget(post);
            if (found == null) continue;
            double distance = golem.distanceToSqr(Vec3.atCenterOf(post.getBlockPos()));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = post.getBlockPos();
                bestTarget = found;
            }
        }
        postPos = best;
        target = bestTarget;
        return best != null;
    }

    @Override
    public boolean canContinueToUse() {
        EmplacementBlockEntity post = post();
        return post != null && post.isReady() && !post.isCrewedByOther(golem) && idle < STAND_DOWN_TICKS;
    }

    @Override
    public void start() {
        idle = 0;
        charge = 0;
        cooldown = 0;
        repath = 0;
    }

    @Override
    public void stop() {
        EmplacementBlockEntity post = post();
        if (post != null) {
            post.release(golem);
        }
        postPos = null;
        target = null;
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        EmplacementBlockEntity post = post();
        if (post == null) return;
        post.claim(golem);

        if (target != null && !stillValid(post, target)) {
            target = null;
        }
        if (target == null && golem.tickCount % SCAN_INTERVAL == 0) {
            target = findTarget(post);
        }

        // Get to the post first, whether or not there's anything to shoot yet.
        Vec3 center = Vec3.atBottomCenterOf(post.getBlockPos());
        double dx = golem.getX() - center.x;
        double dz = golem.getZ() - center.z;
        if (dx * dx + dz * dz > REACH * REACH || Math.abs(golem.getY() - center.y) > 1.5) {
            if (--repath <= 0) {
                Vec3 spot = Vec3.atBottomCenterOf(post.getBlockPos().relative(post.facing().getOpposite()));
                golem.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
                repath = 10;
            }
            charge = 0;
            return;
        }
        golem.getNavigation().stop();

        if (target == null) {
            idle++;
            return;
        }
        idle = 0;

        ItemStack weapon = post.getCrossbow();
        float[] aim = aim(post, weapon, target);
        post.setAim(aim[0], aim[1]);
        golem.getLookControl().setLookAt(target, 30.0f, 30.0f);

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (!CrossbowItem.isCharged(weapon)) {
            if (++charge >= chargeTicks(weapon)) {
                load(post, weapon);
                charge = 0;
            }
            return;
        }
        if (clearShot(post, target)) {
            fire(post, weapon, aim);
            cooldown = CrossbowItem.isCharged(post.getCrossbow()) ? BURST_INTERVAL : 0;
        }
    }

    // ---------------- posts and targets ----------------

    private List<EmplacementBlockEntity> nearbyPosts(ServerLevel level) {
        List<EmplacementBlockEntity> posts = new ArrayList<>();
        level.getPoiManager().findAll(type -> type.is(ModPoiTypes.EMPLACEMENT.getKey()), pos -> true,
                        golem.home(), GarrisonGolemEntity.homeRadius(), PoiManager.Occupancy.ANY)
                .forEach(pos -> {
                    if (level.getBlockEntity(pos) instanceof EmplacementBlockEntity post) {
                        posts.add(post);
                    }
                });
        return posts;
    }

    @Nullable
    private EmplacementBlockEntity post() {
        if (postPos == null) return null;
        return golem.level().getBlockEntity(postPos) instanceof EmplacementBlockEntity post ? post : null;
    }

    /** The nearest hostile mob the post covers and can see. */
    @Nullable
    private LivingEntity findTarget(EmplacementBlockEntity post) {
        Vec3 muzzle = post.muzzle();
        AABB area = new AABB(post.getBlockPos()).inflate(post.range());
        List<LivingEntity> candidates = golem.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> isTarget(e) && e.isAlive() && !e.isInvisible() && post.covers(center(e)));
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle)));
        for (LivingEntity candidate : candidates) {
            if (canSee(post, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean stillValid(EmplacementBlockEntity post, LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && post.covers(center(entity)) && canSee(post, entity);
    }

    private boolean canSee(EmplacementBlockEntity post, LivingEntity entity) {
        Vec3 muzzle = post.muzzle();
        return clear(muzzle, entity.getEyePosition()) || clear(muzzle, center(entity));
    }

    private boolean clear(Vec3 from, Vec3 to) {
        return golem.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    /** Nothing that isn't hostile may stand between the post and its target. */
    private boolean clearShot(EmplacementBlockEntity post, LivingEntity target) {
        Vec3 from = post.muzzle();
        Vec3 to = center(target);
        AABB path = new AABB(from, to).inflate(1.0);
        for (Entity entity : golem.level().getEntities(golem, path,
                e -> e instanceof LivingEntity living && e != target && !isTarget(living) && !e.isSpectator())) {
            if (entity.getBoundingBox().inflate(0.3).clip(from, to).isPresent()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hostile mobs, plus anything in {@code #fletcherstrestle:garrison_targets},
     * minus anything in {@code #fletcherstrestle:garrison_ignores}.
     */
    private static boolean isTarget(LivingEntity entity) {
        if (entity.getType().is(ModTags.EntityTypes.GARRISON_IGNORES)) return false;
        return entity instanceof Enemy || entity.getType().is(ModTags.EntityTypes.GARRISON_TARGETS);
    }

    private static Vec3 center(LivingEntity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    // ---------------- loading and firing ----------------

    private int chargeTicks(ItemStack weapon) {
        return weapon.getItem() instanceof ModularCrossbowItem modular
                ? modular.requiredChargeTicks(weapon, golem)
                : CrossbowItem.getChargeDuration(weapon, golem);
    }

    private void load(EmplacementBlockEntity post, ItemStack weapon) {
        int bolts = weapon.getItem() instanceof ModularCrossbowItem
                ? ModularCrossbowItem.magazineSize(weapon, golem) : 1;
        List<ItemStack> loaded = new ArrayList<>();
        for (int i = 0; i < bolts; i++) {
            ItemStack arrow = ModularQuiverItem.takeOne(post.getQuiver());
            if (arrow.isEmpty()) break;
            loaded.add(arrow);
        }
        if (loaded.isEmpty()) return;
        weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(loaded));
        post.contentsChanged();
        golem.level().playSound(null, post.getBlockPos(), SoundEvents.CROSSBOW_LOADING_END.value(),
                SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    private void fire(EmplacementBlockEntity post, ItemStack weapon, float[] aim) {
        // The crossbow shoots along the shooter's view, so the golem looks down the
        // post's aim for the instant it fires; the bolt then starts at the muzzle.
        golem.setYRot(aim[0]);
        golem.setYHeadRot(aim[0]);
        golem.setXRot(aim[1]);
        golem.setMuzzle(post.muzzle());
        golem.setFiring(weapon);
        try {
            ((CrossbowItem) weapon.getItem()).performShooting(golem.level(), golem, InteractionHand.MAIN_HAND,
                    weapon, BOLT_SPEED, INACCURACY, null);
        } finally {
            golem.setFiring(ItemStack.EMPTY);
            golem.setMuzzle(null);
        }
        if (weapon.isEmpty()) {
            post.setCrossbow(ItemStack.EMPTY);
        } else {
            post.contentsChanged();
        }
    }

    /**
     * Yaw and pitch from the muzzle to where the target will be when the bolt
     * arrives, raised by the distance the bolt falls on the way.
     */
    private static float[] aim(EmplacementBlockEntity post, ItemStack weapon, LivingEntity target) {
        Vec3 from = post.muzzle();
        double speed = BOLT_SPEED * velocityMultiplier(weapon);
        Vec3 point = center(target);
        double ticks = from.distanceTo(point) / speed;
        Vec3 drift = target.getDeltaMovement();
        point = point.add(drift.x * ticks, 0.0, drift.z * ticks);
        ticks = from.distanceTo(point) / speed;

        double dx = point.x - from.x;
        double dz = point.z - from.z;
        double dy = point.y - from.y + 0.5 * GRAVITY * ticks * ticks;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -60.0f, 60.0f)};
    }

    private static float velocityMultiplier(ItemStack weapon) {
        BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());
        return assembly != null ? Materials.bowString(assembly.stringMaterial()).stats().velocityMultiplier() : 1.0f;
    }
}
