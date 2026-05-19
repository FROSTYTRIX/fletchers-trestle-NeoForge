package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EagleEntity extends TamableAnimal {

    // ---------------------------------------------------------------
    // Synced data — declaration order is critical!
    // These IDs are assigned sequentially at class load time.
    // NEVER reorder these fields.
    // ---------------------------------------------------------------
    private static final EntityDataAccessor<Integer> EAGLE_STATE =
            SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_FLYING =
            SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.BOOLEAN);

    // Eagle states (stored as int for sync simplicity)
    public static final int STATE_IDLE     = 0;
    public static final int STATE_PERCHED  = 1;
    public static final int STATE_FETCHING = 2;
    public static final int STATE_HUNTING  = 3;
    public static final int STATE_RETURNING = 4;

    // Hunt target — not synced, server-only logic
    @Nullable
    private LivingEntity huntTarget = null;

    public EagleEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        // Use flying move control and allow path-finding in air
        this.moveControl = new FlyingMoveControl(this, 10, false);
        // Eagles never despawn once tamed
        this.setPersistenceRequired();
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER,        -1f);
        this.setPathfindingMalus(PathType.OPEN,          0f);
    }

    // ---------------------------------------------------------------
    // Step 2 — Attributes
    // ---------------------------------------------------------------
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH,     20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED,   0.6)
                .add(Attributes.FOLLOW_RANGE,   32.0)
                .add(Attributes.ATTACK_DAMAGE,  3.0);
    }

    // ---------------------------------------------------------------
    // Step 3 — Synced data registration
    // ---------------------------------------------------------------
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); // TamableAnimal registers its own fields first
        builder.define(EAGLE_STATE, STATE_IDLE);
        builder.define(IS_FLYING,   false);
    }

    // ---------------------------------------------------------------
    // Step 4 — AI Goal stack
    // ---------------------------------------------------------------
    @Override
    protected void registerGoals() {
        // Priority 1: float above water so it doesn't drown
        this.goalSelector.addGoal(1, new FloatGoal(this));

        // Priority 2: obey sit command
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));

        // Priority 3: fetch arrows (custom — Phase 2, Step 5)
        this.goalSelector.addGoal(3, new EagleFetchGoal(this));

        // Priority 4: hunt mode (custom — Phase 2, Step 6, triggered externally)
        this.goalSelector.addGoal(4, new EagleHuntGoal(this));

        // Priority 5: follow owner when not doing anything else
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));

        // Priority 6 & 7: idle look behaviours
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ---------------------------------------------------------------
    // Step 7 — Taming
    // ---------------------------------------------------------------
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame()) {
            // Only raw rabbit or fish can tame
            if (stack.is(Items.RABBIT) || stack.is(Items.COD) || stack.is(Items.SALMON)) {
                if (!this.level().isClientSide) {
                    stack.consume(1, player);
                    // 30% chance to tame (same as vanilla wolf)
                    if (this.random.nextInt(10) < 3) {
                        this.tame(player);
                        this.setOrderedToSit(false);
                        this.level().broadcastEntityEvent(this, (byte) 7); // tame success particles
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6); // tame fail smoke
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }

        // Step 8 — Sit/unsit toggle for owner with empty hand
        if (this.isOwnedBy(player) && stack.isEmpty()) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.playSound(SoundEvents.PARROT_AMBIENT, 0.6f,
                        1.2f + this.random.nextFloat() * 0.4f);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    // ---------------------------------------------------------------
    // Step 9 — Save/Load
    // ---------------------------------------------------------------
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); // saves tame/owner data
        tag.putInt("EagleState", this.getEagleState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setEagleState(tag.getInt("EagleState"));
    }

    // ---------------------------------------------------------------
    // Step 12 — Flying navigation
    // ---------------------------------------------------------------
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(false);
        return nav;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false; // Eagles don't take fall damage
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage logic needed
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    // ---------------------------------------------------------------
    // Tick — manage IS_FLYING synced flag
    // ---------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();
        // Keep IS_FLYING in sync so the renderer and model know which animation to use
        boolean flying = !this.onGround() && !this.isOrderedToSit();
        if (flying) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < 0 && !this.getNavigation().isDone()) {
                this.setDeltaMovement(motion.x, motion.y * 0.6, motion.z);
            }
        }

        if (flying != this.isFlying()) {
            this.entityData.set(IS_FLYING, flying);
        }
    }

    @Override
    protected boolean isFlapping() {
        return this.isFlying();
    }

    // ---------------------------------------------------------------
    // Public API used by goals and renderer
    // ---------------------------------------------------------------
    public int getEagleState()               { return this.entityData.get(EAGLE_STATE); }
    public void setEagleState(int state)     { this.entityData.set(EAGLE_STATE, state); }
    public boolean isFlying()                { return this.entityData.get(IS_FLYING); }

    public void setHuntTarget(@Nullable LivingEntity target) {
        this.huntTarget = target;
        this.setEagleState(target != null ? STATE_HUNTING : STATE_IDLE);
    }

    @Nullable
    public LivingEntity getHuntTarget() { return huntTarget; }

    // ---------------------------------------------------------------
    // Required overrides
    // ---------------------------------------------------------------
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // Eagles don't breed in this mod — return null
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.RABBIT) || stack.is(Items.COD) || stack.is(Items.SALMON);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        // Placeholder — replace with ModSounds.EAGLE_AMBIENT when you add sounds
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    protected float getSoundVolume() { return 0.8f; }

    // ---------------------------------------------------------------
    // Inner Goal classes (Steps 5 & 6 from roadmap)
    // ---------------------------------------------------------------

    /**
     * EagleFetchGoal — scans for arrows on the ground near the owner
     * and flies to pick them up, returning them to the owner's inventory.
     */
    static class EagleFetchGoal extends Goal {
        private final EagleEntity eagle;
        @Nullable
        private net.minecraft.world.entity.projectile.AbstractArrow targetArrow;

        EagleFetchGoal(EagleEntity eagle) {
            this.eagle = eagle;
        }

        @Override
        public boolean canUse() {
            if (!eagle.isTame() || eagle.isOrderedToSit()) return false;
            if (eagle.getEagleState() != STATE_IDLE) return false;
            if (!(eagle.getOwner() instanceof Player owner)) return false;

            // Scan for arrows stuck in the ground within 24 blocks of owner
            double range = 24.0;
            targetArrow = eagle.level().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.AbstractArrow.class,
                    owner.getBoundingBox().inflate(range),
                    arrow -> arrow.onGround() &&
                            arrow.getOwner() != null &&
                            arrow.getOwner().getUUID().equals(owner.getUUID())
            ).stream().findFirst().orElse(null);

            return targetArrow != null;
        }

        @Override
        public void start() {
            eagle.setEagleState(STATE_FETCHING);
        }

        @Override
        public boolean canContinueToUse() {
            return targetArrow != null
                    && targetArrow.isAlive()
                    && eagle.getEagleState() == STATE_FETCHING
                    && eagle.isTame()
                    && !eagle.isOrderedToSit();
        }

        @Override
        public void tick() {
            if (targetArrow == null) { stop(); return; }

            // Navigate toward the arrow
            eagle.getNavigation().moveTo(
                    targetArrow.getX(), targetArrow.getY(), targetArrow.getZ(), 1.2);

            // Close enough — pick it up
            if (eagle.distanceToSqr(targetArrow) < 2.25) { // 1.5 blocks squared
                eagle.playSound(SoundEvents.PARROT_FLY, 0.5f, 1.4f);

                // Give arrow item to owner or drop at their feet
                if (eagle.getOwner() instanceof Player owner) {
                    ItemStack pickup = targetArrow.getPickResult().copy();
                    if (!owner.getInventory().add(pickup)) {
                        // Inventory full — drop at owner feet
                        owner.drop(pickup, false);
                    }
                }
                targetArrow.discard();
                targetArrow = null;

                // Return to idle so the goal re-evaluates for more arrows
                eagle.setEagleState(STATE_IDLE);
            }
        }

        @Override
        public void stop() {
            targetArrow = null;
            if (eagle.getEagleState() == STATE_FETCHING) {
                eagle.setEagleState(STATE_IDLE);
            }
        }
    }

    /**
     * EagleHuntGoal — circles above a target entity.
     * Activated externally via EagleEntity.setHuntTarget().
     */
    static class EagleHuntGoal extends Goal {
        private final EagleEntity eagle;
        private int orbitTick = 0;

        EagleHuntGoal(EagleEntity eagle) {
            this.eagle = eagle;
        }

        @Override
        public boolean canUse() {
            return eagle.isTame()
                    && !eagle.isOrderedToSit()
                    && eagle.getHuntTarget() != null
                    && eagle.getHuntTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = eagle.getHuntTarget();
            return target != null && target.isAlive() && eagle.getEagleState() == STATE_HUNTING;
        }

        @Override
        public void start() {
            eagle.setEagleState(STATE_HUNTING);
            orbitTick = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = eagle.getHuntTarget();
            if (target == null) { stop(); return; }

            // Orbit 5 blocks above the target in a circle
            orbitTick++;
            double angle  = orbitTick * 0.08; // radians per tick
            double radius = 5.0;
            double orbitX = target.getX() + Math.sin(angle) * radius;
            double orbitY = target.getY() + 5.0;
            double orbitZ = target.getZ() + Math.cos(angle) * radius;

            eagle.getNavigation().moveTo(orbitX, orbitY, orbitZ, 1.0);
        }

        @Override
        public void stop() {
            eagle.setHuntTarget(null); // clears hunt target and resets state to IDLE
        }
    }
}