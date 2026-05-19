package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

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

    // Fetch inventory — 16 slots so the eagle can carry up to 16 arrows in
    // a single trip, with room for up to 16 different arrow types (one type
    // per slot if needed; matching types still stack). Capacity is enforced
    // by total item count via hasFetchSpace(), not by full slots.
    private static final int FETCH_INVENTORY_SIZE = 16;
    private static final int FETCH_CAPACITY       = 16;
    private final SimpleContainer fetchInventory = new SimpleContainer(FETCH_INVENTORY_SIZE);

    // Hysteresis counters for the IS_FLYING flag. With no gravity, the eagle
    // grazes the ground each tick and onGround() flickers true/false, which
    // would otherwise re-trigger the flying animation every frame.
    private int groundedTicks = 0;
    private int airborneTicks = 0;

    public EagleEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        // Use flying move control and allow path-finding in air
        this.moveControl = new FlyingMoveControl(this, 10, false);
        // Phase 5 — gravity off; travel() and tick() handle all motion.
        // A bird is always in control of its descent: when actively pathing
        // it glides toward the target; when idle it settles gently via the
        // descent term in tick(). Real birds don't drop like rocks.
        this.setNoGravity(true);
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
                .add(Attributes.FLYING_SPEED,   0.17)
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new EagleFetchGoal(this));
        this.goalSelector.addGoal(4, new EagleHuntGoal(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.0,
                Ingredient.of(Items.RABBIT, Items.COD, Items.SALMON), false));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        // Random flying: only active for UNTAMED eagles (so wild ones still
        // wander), and rate-limited via a long base interval so even those
        // don't constantly cycle through flight. Tamed eagles stay perched
        // near their owner via FollowOwnerGoal.
        this.goalSelector.addGoal(7, new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                if (EagleEntity.this.isTame()) return false;
                // Throttle: ~5% chance per evaluation, vanilla checks at
                // ~once-per-second so this means a wander attempt every ~20s.
                if (EagleEntity.this.getRandom().nextInt(20) != 0) return false;
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
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
                        this.playSound(ModSounds.EAGLE_TAME.get(), 0.8f, 1.0f);
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
                this.playSound(ModSounds.EAGLE_AMBIENT.get(), 0.6f,
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
        // Sanitize state on save. Transient states (FETCHING, RETURNING,
        // HUNTING) depend on goal-local data (target arrow, hunt target) that
        // doesn't survive a save/reload. Persisting them would leave the eagle
        // "stuck" busy after a load with no goal actually running. Only the
        // resting states are safe to persist.
        int state = this.getEagleState();
        boolean persistent = (state == STATE_IDLE || state == STATE_PERCHED);
        tag.putInt("EagleState", persistent ? state : STATE_IDLE);

        // Persist the fetch inventory so a save mid-fetch doesn't lose items.
        ListTag items = new ListTag();
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            ItemStack stack = fetchInventory.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag slotTag = new CompoundTag();
            slotTag.putByte("Slot", (byte) i);
            Tag saved = stack.save(this.registryAccess(), new CompoundTag());
            if (saved instanceof CompoundTag c) slotTag.merge(c);
            items.add(slotTag);
        }
        tag.put("FetchInventory", items);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setEagleState(tag.getInt("EagleState"));

        fetchInventory.clearContent();
        ListTag items = tag.getList("FetchInventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag slotTag = items.getCompound(i);
            int slot = slotTag.getByte("Slot") & 0xFF;
            if (slot >= fetchInventory.getContainerSize()) continue;
            ItemStack.parse(this.registryAccess(), slotTag)
                    .ifPresent(s -> fetchInventory.setItem(slot, s));
        }
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
    // Phase 5 — Travel & gravity suppression
    // ---------------------------------------------------------------

    // Birds are always in control of their own motion. Vanilla travel() applies
    // a hardcoded gravity step that fights any flying nav we set up, which
    // produced the "hop / fall / hop" pattern. We replace it entirely:
    //   - moveRelative with FLYING_SPEED to translate AI input into motion
    //   - move() to actually apply it (with collision)
    //   - uniform 0.91 damping so motion bleeds off instead of locking
    //   - when on ground or sitting, use a stronger ground friction
    //
    // No gravity term at all — the bird never "falls" passively. If it has
    // nowhere to go (no path), it gently settles via the descent term in
    // tick() rather than dropping like a rock.
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02f, input);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            } else if (this.isInLava()) {
                this.moveRelative(0.02f, input);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            } else {
                float speed = this.onGround() ? this.getSpeed() * 0.1f : this.getSpeed();
                float damping = this.onGround() ? 0.6f : 0.91f;
                this.moveRelative(speed, input);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(damping));
            }
        }
        this.calculateEntityAnimation(false);
    }

    // Use FLYING_SPEED rather than MOVEMENT_SPEED while airborne, so the
    // FLYING_SPEED attribute actually does something.
    @Override
    protected float getFlyingSpeed() {
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    // ---------------------------------------------------------------
    // Tick — manage IS_FLYING synced flag
    // ---------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();

        // Gravity stays off — travel() handles all motion. If a vanilla code
        // path flipped it back on (rare but possible), force it off again.
        if (!this.isNoGravity()) {
            this.setNoGravity(true);
        }

        // Robust ground check: vanilla onGround() flickers because with no
        // gravity the eagle barely loads against the block. We extend the
        // bounding box ~0.15 blocks down and test for any collision —
        // that's stable across FollowOwnerGoal nudges and other small Y jitter.
        boolean grounded = isGroundedRobust();
        boolean airborne = !grounded;

        // Bird-like settling: if airborne with no active goal/path, apply a
        // gentle downward drift so the eagle descends to land instead of
        // hovering forever.
        boolean idleAirborne = airborne
                && !this.isOrderedToSit()
                && this.getNavigation().isDone()
                && this.huntTarget == null
                && this.getEagleState() == STATE_IDLE;
        if (idleAirborne) {
            Vec3 m = this.getDeltaMovement();
            if (m.y > -0.08) {
                this.setDeltaMovement(m.x, m.y - 0.02, m.z);
            }
        }

        // Hysteresis on the IS_FLYING flag.
        if (airborne) {
            airborneTicks++;
            groundedTicks = 0;
        } else {
            groundedTicks++;
            airborneTicks = 0;
        }

        boolean currentlyFlying = this.isFlying();
        boolean shouldFly;
        if (this.isOrderedToSit()) {
            shouldFly = false;
        } else if (currentlyFlying) {
            // Was flying — only switch to "not flying" after 4+ ticks grounded
            shouldFly = groundedTicks < 4;
        } else {
            // Was grounded — require 8+ ticks airborne before flipping to
            // flying so a brief lift from FollowOwnerGoal doesn't trigger
            // the flap animation while the eagle is essentially standing still.
            shouldFly = airborneTicks >= 8;
        }
        if (shouldFly != currentlyFlying) {
            this.entityData.set(IS_FLYING, shouldFly);
        }

        // Watchdog: clear stale non-IDLE state when no goal is actually running.
        // STATE_HUNTING is owned by the hunt goal; if huntTarget is gone, the
        // state is dead. STATE_RETURNING with no inventory is similarly stale.
        // STATE_FETCHING is harder to validate here (target lives on the goal)
        // so we rely on the goal's own giveUpTimer + canContinueToUse to clear it.
        if (!this.level().isClientSide) {
            int curState = this.getEagleState();
            if (curState == STATE_HUNTING && huntTarget == null) {
                this.setEagleState(STATE_IDLE);
            } else if (curState == STATE_RETURNING && isFetchInventoryEmpty()) {
                this.setEagleState(STATE_IDLE);
            }
        }
    }

    // Returns true if the eagle's hitbox is within ~0.3 blocks of a solid
    // surface below it. More reliable than onGround() when gravity is off,
    // because it doesn't depend on the entity having had downward motion
    // that got capped on the same tick. The 0.3 buffer also tolerates the
    // brief upward nudges FollowOwnerGoal applies near the ground.
    private boolean isGroundedRobust() {
        if (this.onGround()) return true;
        net.minecraft.world.phys.AABB bb = this.getBoundingBox();
        net.minecraft.world.phys.AABB probe = new net.minecraft.world.phys.AABB(
                bb.minX, bb.minY - 0.3, bb.minZ,
                bb.maxX, bb.minY,       bb.maxZ);
        return !this.level().noCollision(this, probe);
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
    // Fetch inventory API — used by EagleFetchGoal.
    // ---------------------------------------------------------------
    // True while the eagle has carried fewer than FETCH_CAPACITY arrows.
    // Capacity is enforced by total item count, not by slot, so 16 different
    // types each occupy their own slot but still cap at 16 arrows total.
    public boolean hasFetchSpace() {
        return countFetchItems() < FETCH_CAPACITY;
    }

    private int countFetchItems() {
        int total = 0;
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            total += fetchInventory.getItem(i).getCount();
        }
        return total;
    }

    public boolean isFetchInventoryEmpty() {
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            if (!fetchInventory.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    // Returns leftover stack if the inventory couldn't hold it all.
    public ItemStack addToFetchInventory(ItemStack stack) {
        return fetchInventory.addItem(stack);
    }

    // Hand over everything in the inventory to the owner; whatever doesn't
    // fit gets dropped at the owner's feet rather than disappearing.
    public void depositFetchInventoryTo(Player owner) {
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            ItemStack stack = fetchInventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!owner.getInventory().add(stack)) {
                owner.drop(stack, false);
            }
            fetchInventory.setItem(i, ItemStack.EMPTY);
        }
    }

    // Drop the carried inventory at the eagle's feet — used on death.
    public void dropFetchInventoryHere() {
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            ItemStack stack = fetchInventory.getItem(i);
            if (stack.isEmpty()) continue;
            this.spawnAtLocation(stack);
            fetchInventory.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        dropFetchInventoryHere();
    }

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
        return ModSounds.EAGLE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.EAGLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.EAGLE_DEATH.get();
    }

    @Override
    protected float getSoundVolume() { return 0.8f; }

    // ---------------------------------------------------------------
    // Inner Goal classes (Steps 5 & 6 from roadmap)
    // ---------------------------------------------------------------

    /**
     * EagleFetchGoal — scans for arrows on the ground near the owner, flies
     * out to grab them into the eagle's fetch inventory, then returns to the
     * owner to deposit. With more than one slot the eagle chains pickups
     * until the inventory is full or no more arrows are in range.
     */
    static class EagleFetchGoal extends Goal {
        private final EagleEntity eagle;
        @Nullable
        private net.minecraft.world.entity.projectile.AbstractArrow targetArrow;
        // Throttle counter for path recomputation. Re-pathing every tick
        // makes FlyingPathNavigation stutter and never converge on a target.
        private int repathCooldown = 0;
        // Per-phase watchdog. Reset on FETCH→RETURN transitions and on
        // chained pickups so each leg has a fresh budget.
        private int giveUpTimer = 0;

        EagleFetchGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!eagle.isTame() || eagle.isOrderedToSit()) return false;
            if (!(eagle.getOwner() instanceof Player owner) || !owner.isAlive()) return false;
            // Owner in a different dimension — we can't path there, don't even try.
            if (owner.level().dimension() != eagle.level().dimension()) return false;

            int state = eagle.getEagleState();

            // Cold-start return: if we have items in the inventory (e.g.,
            // loaded mid-fetch from save, or an interrupted return), deliver
            // them. This works from IDLE or a sanitized post-load state.
            if (!eagle.isFetchInventoryEmpty() && state == STATE_IDLE) {
                return true;
            }

            // Otherwise start a fresh fetch only when idle with room to carry.
            if (state != STATE_IDLE) return false;
            if (!eagle.hasFetchSpace()) return false;

            targetArrow = findNearestArrow(eagle, owner);
            return targetArrow != null;
        }

        @Override
        public void start() {
            // Cold-start the return phase if we already have items; otherwise
            // begin a fresh fetch.
            if (!eagle.isFetchInventoryEmpty()) {
                eagle.setEagleState(STATE_RETURNING);
            } else {
                eagle.setEagleState(STATE_FETCHING);
            }
            repathCooldown = 0;
            giveUpTimer = 0;
        }

        @Override
        public boolean canContinueToUse() {
            if (!eagle.isTame() || eagle.isOrderedToSit()) return false;
            if (!(eagle.getOwner() instanceof Player owner) || !owner.isAlive()) return false;
            if (owner.level().dimension() != eagle.level().dimension()) return false;

            int state = eagle.getEagleState();
            if (state == STATE_FETCHING) {
                return targetArrow != null && targetArrow.isAlive();
            }
            if (state == STATE_RETURNING) {
                return !eagle.isFetchInventoryEmpty();
            }
            return false;
        }

        @Override
        public void tick() {
            giveUpTimer++;
            // 20-second budget per phase. The timer resets when we transition
            // FETCH→RETURN or chain to a new arrow, so a long full cycle is fine.
            if (giveUpTimer > 400) { stop(); return; }

            int state = eagle.getEagleState();
            if (state == STATE_FETCHING) {
                tickFetch();
            } else if (state == STATE_RETURNING) {
                tickReturn();
            } else {
                stop();
            }
        }

        // Phase 1 — fly out to the arrow, grab it, then either chain to the
        // next nearby arrow or transition to RETURNING.
        private void tickFetch() {
            if (targetArrow == null || !targetArrow.isAlive()) {
                // Lost the arrow. If we already collected something, head
                // home with it; otherwise just stop.
                if (!eagle.isFetchInventoryEmpty()) {
                    transitionToReturn();
                } else {
                    stop();
                }
                return;
            }
            if (!eagle.level().isLoaded(targetArrow.blockPosition())) { stop(); return; }

            double tx = targetArrow.getX();
            double ty = targetArrow.getY() + 0.3;
            double tz = targetArrow.getZ();

            eagle.getMoveControl().setWantedPosition(tx, ty, tz, 1.4);
            if (--repathCooldown <= 0) {
                eagle.getNavigation().moveTo(tx, ty, tz, 1.4);
                repathCooldown = 10;
            }

            if (eagle.distanceToSqr(targetArrow) < 4.0) {
                eagle.playSound(ModSounds.EAGLE_FLAP.get(), 0.5f, 1.4f);

                ItemStack pickup = targetArrow.getPickupItemStackOrigin().copy();
                if (pickup.isEmpty()) pickup = new ItemStack(Items.ARROW);

                ItemStack leftover = eagle.addToFetchInventory(pickup);
                if (!leftover.isEmpty()) {
                    // Shouldn't normally happen — canUse checked hasFetchSpace —
                    // but drop anything that didn't fit so it's not lost.
                    eagle.spawnAtLocation(leftover);
                }

                targetArrow.discard();
                targetArrow = null;

                // Decide whether to chain to another arrow or head home.
                Player owner = (Player) eagle.getOwner();
                net.minecraft.world.entity.projectile.AbstractArrow next =
                        (eagle.hasFetchSpace() && owner != null)
                                ? findNearestArrow(eagle, owner)
                                : null;

                if (next != null) {
                    targetArrow = next;
                    eagle.getNavigation().stop();
                    repathCooldown = 0;
                    giveUpTimer  = 0;
                } else {
                    transitionToReturn();
                }
            }
        }

        private void transitionToReturn() {
            eagle.setEagleState(STATE_RETURNING);
            eagle.getNavigation().stop();
            repathCooldown = 0;
            giveUpTimer    = 0;
        }

        // Phase 2 — carry everything back to the owner and deposit it.
        private void tickReturn() {
            if (eagle.isFetchInventoryEmpty()) { stop(); return; }
            if (!(eagle.getOwner() instanceof Player owner) || !owner.isAlive()) { stop(); return; }
            if (!eagle.level().isLoaded(owner.blockPosition())) return;

            double tx = owner.getX();
            double ty = owner.getY() + 1.2;
            double tz = owner.getZ();

            eagle.getMoveControl().setWantedPosition(tx, ty, tz, 1.4);
            if (--repathCooldown <= 0) {
                eagle.getNavigation().moveTo(tx, ty, tz, 1.4);
                repathCooldown = 10;
            }

            if (eagle.distanceToSqr(owner) < 6.25) {
                eagle.playSound(ModSounds.EAGLE_FLAP.get(), 0.6f, 1.2f);
                eagle.depositFetchInventoryTo(owner);
                eagle.setEagleState(STATE_IDLE);
            }
        }

        @Override
        public void stop() {
            // Keep any carried items in the inventory — next canUse cycle will
            // re-attempt delivery (cold-start return). This avoids dropping
            // items every time the eagle gets briefly interrupted (panic,
            // sit, etc.). Death drops the inventory via dropEquipment.
            targetArrow = null;
            int s = eagle.getEagleState();
            if (s == STATE_FETCHING || s == STATE_RETURNING) {
                eagle.setEagleState(STATE_IDLE);
            }
        }

        private static net.minecraft.world.entity.projectile.AbstractArrow findNearestArrow(EagleEntity eagle, Player owner) {
            double range = 24.0;
            return eagle.level().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.AbstractArrow.class,
                    owner.getBoundingBox().inflate(range),
                    arrow -> arrow.tickCount > 5
                            && arrow.inGround
                            && arrow.getOwner() != null
                            && arrow.getOwner().getUUID().equals(owner.getUUID())
                            && eagle.level().isLoaded(arrow.blockPosition())
            ).stream()
                    .min((a, b) -> Double.compare(eagle.distanceToSqr(a), eagle.distanceToSqr(b)))
                    .orElse(null);
        }
    }

    /**
     * EagleHuntGoal — circles above a target entity.
     * Activated externally via EagleEntity.setHuntTarget().
     */
    static class EagleHuntGoal extends Goal {
        private final EagleEntity eagle;
        private int orbitTick = 0;
        private int repathCooldown = 0;

        EagleHuntGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
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
            repathCooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = eagle.getHuntTarget();
            if (target == null) { stop(); return; }
            // Skip orbit work if the target's chunk is unloaded — wait until
            // it comes back into range rather than pathing into nowhere.
            if (!eagle.level().isLoaded(target.blockPosition())) return;

            // Orbit 5 blocks above the target in a circle.
            orbitTick++;
            double angle  = orbitTick * 0.08; // radians per tick
            double radius = 5.0;
            double orbitX = target.getX() + Math.sin(angle) * radius;
            double orbitY = target.getY() + 5.0;
            double orbitZ = target.getZ() + Math.cos(angle) * radius;

            // Drive the move control every tick (the orbit point moves
            // continuously), but re-issue pathfinding only periodically.
            eagle.getMoveControl().setWantedPosition(orbitX, orbitY, orbitZ, 1.2);
            if (--repathCooldown <= 0) {
                eagle.getNavigation().moveTo(orbitX, orbitY, orbitZ, 1.2);
                repathCooldown = 8;
            }
        }

        @Override
        public void stop() {
            eagle.setHuntTarget(null); // clears hunt target and resets state to IDLE
        }
    }
}