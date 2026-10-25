package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.block.entity.EmplacementBlockEntity;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The golem's job: find a loaded emplacement that can see a hostile mob, run to
 * it, and work the crossbow until nothing is left in its cone.
 *
 * <p>When several posts could take a shot it goes to the nearest, except that a
 * Scope counts for a far target and a Magazine for a crowd. It winds up for a
 * moment, runs there, and shuffles round behind the crossbow as it traverses.
 * Loading takes the crossbow's own charge time, so tuning and Quick Charge make
 * a faster post; a magazine loads several bolts and fires them in a burst. The
 * golem holds fire while anything that isn't hostile stands in the line.</p>
 *
 * <p>A target that steps out of the cone is still followed, the crossbow
 * pinned at the edge, until another post can take it, and then the golem runs
 * there; if none can, it stands down after a few seconds. A post that runs dry
 * gets a shrug and a puff from the key, and the golem moves on to the next
 * loaded one. A post it can't get to is left alone for half a minute.</p>
 */
public class CrewPostGoal extends Goal {

    private static final int SCAN_INTERVAL = 10;
    /** A post with nothing in its cone to shoot at is left after this long. */
    private static final int STAND_DOWN_TICKS = 60;
    /** Ticks it may spend getting to a post before it gives up on it for a while. */
    private static final int RUN_TIMEOUT = 160;
    /** How long a post it couldn't reach is left alone. */
    private static final long UNREACHABLE_FOR = 600;
    /** How close to the post the golem has to be to work it. */
    private static final double REACH = 1.9;
    private static final double RUN_SPEED = 1.35;
    /** Ticks it stands winding itself up before it runs to a post. */
    private static final int SPIN_UP = 8;
    /** Bolt speed before the string's multiplier: a player's crossbow shot. */
    private static final float BOLT_SPEED = 3.15f;
    private static final float INACCURACY = 1.0f;
    /** Ticks between bolts of a magazine burst. */
    private static final int BURST_INTERVAL = 8;
    private static final int CRANK_SOUND_INTERVAL = 7;
    /** Arrow gravity per tick, for the drop the aim makes up for. */
    private static final double GRAVITY = 0.05;
    /** A target this far past the post's range is still followed, though not shot. */
    private static final double TRACK_MARGIN = 4.0;
    /** How many blocks of walking a well-suited attachment is worth when choosing a post. */
    private static final double SUITED_BONUS = 6.0;
    /** How many hostiles in a cone count as a crowd, for a Magazine. */
    private static final int CROWD = 3;
    /** Ticks without a shot before it looks for another post that has one. */
    private static final int REPOSITION_AFTER = 10;

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
    private int retarget;
    private int spinUp;
    private int travel;
    private boolean abandon;
    /** Posts it couldn't get to, and the game time until which it leaves them alone. */
    private final Map<BlockPos, Long> unreachable = new HashMap<>();

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
        Choice choice = choosePost(level, null);
        if (choice == null) return false;
        postPos = choice.post().getBlockPos();
        target = choice.target();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        EmplacementBlockEntity post = post();
        return !abandon && post != null && post.isReady() && !post.isCrewedByOther(golem) && idle < STAND_DOWN_TICKS;
    }

    @Override
    public void start() {
        idle = 0;
        charge = 0;
        cooldown = 0;
        repath = 0;
        retarget = 0;
        travel = 0;
        abandon = false;
        EmplacementBlockEntity post = post();
        boolean atPost = post != null && atPost(post);
        spinUp = atPost ? 0 : SPIN_UP;
        golem.getNavigation().stop();
        golem.setGarrisonState(atPost ? GarrisonGolemEntity.MANNING : GarrisonGolemEntity.ALERT);
        if (!atPost) {
            golem.playSound(ModSounds.GARRISON_WIND_UP.get(), 1.0f, 1.0f);
        }
    }

    @Override
    public void stop() {
        EmplacementBlockEntity post = post();
        if (post != null) {
            if (!post.getCrossbow().isEmpty() && !post.hasAmmo() && !CrossbowItem.isCharged(post.getCrossbow())) {
                shrug();
            }
            post.release(golem);
        }
        if (abandon) {
            // Straight on to the post that has the shot.
            scanDelay = 0;
        }
        postPos = null;
        target = null;
        golem.setGarrisonState(GarrisonGolemEntity.IDLE);
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

        if (target != null && !tracked(post, target)) {
            target = null;
        }
        if ((target == null || !post.covers(center(target))) && --retarget <= 0) {
            retarget = SCAN_INTERVAL;
            LivingEntity found = findTarget(post);
            if (found != null) {
                target = found;
            }
        }

        if (spinUp > 0) {
            spinUp--;
            golem.getNavigation().stop();
            if (target != null) {
                golem.getLookControl().setLookAt(target, 30.0f, 30.0f);
            }
            return;
        }

        // Get to the post first, whether or not there's anything to shoot yet.
        Vec3 stand = post.standingSpot();
        if (!atPost(post)) {
            golem.setGarrisonState(GarrisonGolemEntity.ALERT);
            if (++travel > RUN_TIMEOUT) {
                unreachable.put(post.getBlockPos().immutable(), golem.level().getGameTime() + UNREACHABLE_FOR);
                abandon = true;
                return;
            }
            if (--repath <= 0) {
                golem.getNavigation().moveTo(stand.x, stand.y, stand.z, RUN_SPEED);
                repath = 10;
            }
            charge = 0;
            return;
        }
        golem.getNavigation().stop();
        // Shuffle round behind the crossbow as it traverses.
        if (golem.position().distanceToSqr(stand.x, golem.getY(), stand.z) > 0.04 && canStand(stand)) {
            golem.getMoveControl().setWantedPosition(stand.x, golem.getY(), stand.z, 0.5);
        }

        ItemStack weapon = post.getCrossbow();
        boolean loaded = CrossbowItem.isCharged(weapon);
        if (!loaded) {
            // Reload whenever it isn't loaded, target or not.
            golem.setGarrisonState(GarrisonGolemEntity.RELOADING);
            if (charge % CRANK_SOUND_INTERVAL == 0) {
                golem.playSound(ModSounds.GARRISON_CRANK.get(), 0.6f, 0.9f + golem.getRandom().nextFloat() * 0.2f);
            }
            if (++charge >= chargeTicks(weapon)) {
                load(post, weapon);
                charge = 0;
            }
        } else {
            golem.setGarrisonState(GarrisonGolemEntity.MANNING);
        }

        if (target == null) {
            idle++;
            maybeReposition(post);
            return;
        }

        float[] aim = aim(post, weapon, target);
        post.setAim(aim[0], aim[1]);
        golem.setYRot(post.getAimYaw());
        golem.yBodyRot = post.getAimYaw();
        golem.getLookControl().setLookAt(target, 30.0f, 30.0f);

        // A target followed out of the cone doesn't keep the post manned: it
        // counts toward standing down like no target at all.
        boolean inCone = post.covers(center(target));
        if (inCone) {
            idle = 0;
        } else {
            idle++;
            maybeReposition(post);
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (loaded && inCone && clearShot(post, target)) {
            fire(post, weapon, new float[]{post.getAimYaw(), post.getAimPitch()});
            golem.cue(GarrisonGolemEntity.EVENT_RECOIL);
            cooldown = CrossbowItem.isCharged(post.getCrossbow()) ? BURST_INTERVAL : 0;
        }
    }

    private boolean atPost(EmplacementBlockEntity post) {
        Vec3 center = Vec3.atBottomCenterOf(post.getBlockPos());
        double dx = golem.getX() - center.x;
        double dz = golem.getZ() - center.z;
        return dx * dx + dz * dz <= REACH * REACH && Math.abs(golem.getY() - center.y) <= 1.5;
    }

    /** Room for the golem at a spot, with something under it. */
    private boolean canStand(Vec3 spot) {
        AABB box = golem.getBoundingBox().move(spot.x - golem.getX(), 0.0, spot.z - golem.getZ());
        BlockPos below = BlockPos.containing(spot.x, golem.getY() - 0.5, spot.z);
        return golem.level().noCollision(golem, box)
                && !golem.level().getBlockState(below).getCollisionShape(golem.level(), below).isEmpty();
    }

    /** With nothing to shoot here for a moment, go to a post that has a shot. */
    private void maybeReposition(EmplacementBlockEntity here) {
        if (idle < REPOSITION_AFTER || idle % SCAN_INTERVAL != 0) return;
        if (golem.level() instanceof ServerLevel level && choosePost(level, here.getBlockPos()) != null) {
            abandon = true;
        }
    }

    private void shrug() {
        golem.cue(GarrisonGolemEntity.EVENT_SHRUG);
        golem.playSound(ModSounds.GARRISON_EMPTY.get(), 1.0f, 1.0f);
        if (golem.level() instanceof ServerLevel level) {
            // A puff from the wind-up key in its back.
            float yaw = golem.yBodyRot * Mth.DEG_TO_RAD;
            double x = golem.getX() + Mth.sin(yaw) * 0.35;
            double z = golem.getZ() - Mth.cos(yaw) * 0.35;
            level.sendParticles(ParticleTypes.SMOKE, x, golem.getY() + 0.9, z, 6, 0.05, 0.05, 0.05, 0.01);
        }
    }

    // ---------------- posts and targets ----------------

    private record Choice(EmplacementBlockEntity post, LivingEntity target) {
    }

    /** The best post that has a shot right now, other than {@code skip}. */
    @Nullable
    private Choice choosePost(ServerLevel level, @Nullable BlockPos skip) {
        Choice best = null;
        double bestScore = Double.MAX_VALUE;
        long now = level.getGameTime();
        unreachable.values().removeIf(until -> until <= now);
        for (EmplacementBlockEntity post : nearbyPosts(golem, level)) {
            if (post.getBlockPos().equals(skip) || unreachable.containsKey(post.getBlockPos())
                    || !post.isReady() || post.isCrewedByOther(golem)) continue;
            List<LivingEntity> targets = targetsInCone(post);
            LivingEntity found = firstVisible(post, targets);
            if (found == null) continue;
            double score = Math.sqrt(golem.distanceToSqr(Vec3.atCenterOf(post.getBlockPos())));
            if (post.isScoped() && found.distanceTo(golem) > FletcherConfig.GARRISON_RANGE.get() * 0.6) {
                score -= SUITED_BONUS;
            }
            if (targets.size() >= CROWD && post.getCrossbow().getItem() instanceof ModularCrossbowItem
                    && ModularCrossbowItem.magazineSize(post.getCrossbow(), golem) > 1) {
                score -= SUITED_BONUS;
            }
            if (score < bestScore) {
                bestScore = score;
                best = new Choice(post, found);
            }
        }
        return best;
    }

    /** Emplacements within the golem's home radius. */
    public static List<EmplacementBlockEntity> nearbyPosts(GarrisonGolemEntity golem, ServerLevel level) {
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
        return firstVisible(post, targetsInCone(post));
    }

    /** Hostile mobs in the post's cone and range, nearest first. */
    private List<LivingEntity> targetsInCone(EmplacementBlockEntity post) {
        Vec3 muzzle = post.muzzle();
        AABB area = new AABB(post.getBlockPos()).inflate(post.range(), EmplacementBlockEntity.VERTICAL_REACH, post.range());
        List<LivingEntity> candidates = golem.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> isTarget(e) && e.isAlive() && !e.isInvisible() && post.covers(center(e)));
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(muzzle)));
        return candidates;
    }

    @Nullable
    private LivingEntity firstVisible(EmplacementBlockEntity post, List<LivingEntity> candidates) {
        for (LivingEntity candidate : candidates) {
            if (canSee(post, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Still worth following: alive, in sight, and not far past the post's range. */
    private boolean tracked(EmplacementBlockEntity post, LivingEntity entity) {
        double reach = post.range() + TRACK_MARGIN;
        Vec3 to = entity.position().subtract(post.muzzle());
        return entity.isAlive() && !entity.isRemoved() && to.x * to.x + to.z * to.z < reach * reach
                && Math.abs(to.y) <= EmplacementBlockEntity.VERTICAL_REACH && canSee(post, entity);
    }

    private boolean canSee(EmplacementBlockEntity post, LivingEntity entity) {
        Vec3 muzzle = post.muzzle();
        return clear(post, muzzle, entity.getEyePosition()) || clear(post, muzzle, center(entity));
    }

    /**
     * Whether nothing solid lies between two points. The post's own block is
     * left out: the crossbow sits just above its turntable, so any steep shot
     * down would otherwise be blocked by the post itself.
     */
    private boolean clear(EmplacementBlockEntity post, Vec3 from, Vec3 to) {
        BlockPos own = post.getBlockPos();
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()) {
            @Override
            public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                return pos.equals(own) ? Shapes.empty() : super.getBlockShape(state, level, pos);
            }
        };
        return golem.level().clip(context).getType() == HitResult.Type.MISS;
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
        golem.setMuzzle(post.muzzle(), post.getBlockPos());
        golem.setFiring(weapon);
        try {
            ((CrossbowItem) weapon.getItem()).performShooting(golem.level(), golem, InteractionHand.MAIN_HAND,
                    weapon, BOLT_SPEED, INACCURACY, null);
        } finally {
            golem.setFiring(ItemStack.EMPTY);
            golem.setMuzzle(null, null);
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
        return new float[]{Mth.wrapDegrees(yaw),
                Mth.clamp(pitch, -EmplacementBlockEntity.MAX_PITCH, EmplacementBlockEntity.MAX_PITCH)};
    }

    private static float velocityMultiplier(ItemStack weapon) {
        BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());
        return assembly != null ? Materials.bowString(assembly.stringMaterial()).stats().velocityMultiplier() : 1.0f;
    }
}
