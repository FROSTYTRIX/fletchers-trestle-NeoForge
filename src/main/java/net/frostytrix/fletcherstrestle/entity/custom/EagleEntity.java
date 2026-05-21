package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.block.entity.EagleNestBlockEntity;
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

    // Owner-toggled: when true, the eagle auto-fetches arrows shot by its
    // owner. When false, the eagle stays idle / hunts on spyglass command
    // but does not chase down arrows. Synced so a future HUD can show it.
    private static final EntityDataAccessor<Boolean> FETCH_MODE_ENABLED =
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

    // Position of the block this eagle is bound to as its "home perch".
    // Server-side only; saved to NBT. Null when the eagle has no perch.
    @Nullable
    private BlockPos perchPos = null;

    // Position of the nest this eagle considers its breeding ground.
    // Server-side only; saved to NBT. Null when not bound to a nest.
    @Nullable
    private BlockPos nestPos = null;

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
        this.setPathfindingMalus(PathType.FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER,        -1f);
        this.setPathfindingMalus(PathType.OPEN,          0f);
    }

    // ---------------------------------------------------------------
    // Spawn rules — keeps wild eagles to high, sunlit mountain ridges
    // rather than spawning under leaves at sea level. Wired up in
    // FletcherTrestle.onRegisterSpawnPlacements.
    // ---------------------------------------------------------------
    public static boolean checkEagleSpawnRules(
            EntityType<EagleEntity> type,
            net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.EntitySpawnReason reason,
            BlockPos pos,
            net.minecraft.util.RandomSource random) {
        // Bottom layers / cave biomes are off-limits regardless of biome tag.
        if (pos.getY() < 80) return false;
        if (!level.canSeeSky(pos)) return false;
        // Daylight only — keeps them from popping in at night.
        if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) < 12) return false;
        return true;
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
        builder.define(EAGLE_STATE,         STATE_IDLE);
        builder.define(IS_FLYING,           false);
        builder.define(FETCH_MODE_ENABLED,  true);
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
        this.goalSelector.addGoal(4, new EagleBreedGoal(this));
        this.goalSelector.addGoal(5, new EaglePerchGoal(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.0,
                Ingredient.of(Items.RABBIT, Items.COD, Items.SALMON), false));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        // Wild nest patrol: untamed eagles bound to a nest (via worldgen)
        // pick random points around the nest. Combines "stay near home" with
        // "wander around" so they don't just hover on the same spot forever.
        this.goalSelector.addGoal(7, new EagleNestPatrolGoal(this));
        // Random flying fallback: only active for UNTAMED eagles WITHOUT a
        // nest (e.g., spawn-egged in by an admin). Tamed eagles stay perched
        // near their owner via FollowOwnerGoal.
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                if (EagleEntity.this.isTame()) return false;
                // Nest-bound eagles use the patrol goal instead.
                if (EagleEntity.this.getNestPos() != null) return false;
                // Throttle: ~5% chance per evaluation, vanilla checks at
                // ~once-per-second so this means a wander attempt every ~20s.
                if (EagleEntity.this.getRandom().nextInt(20) != 0) return false;
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
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
                if (!this.level().isClientSide()) {
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
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }

        // Owner empty-hand interactions:
        //  - sneak + right-click → toggle fetch mode (auto-fetch on/off)
        //  - right-click          → toggle sit
        if (this.isOwnedBy(player) && stack.isEmpty()) {
            if (!this.level().isClientSide()) {
                if (player.isShiftKeyDown()) {
                    boolean newMode = !this.isFetchModeEnabled();
                    this.setFetchModeEnabled(newMode);
                    // 26.1: Player.displayClientMessage removed. Use
                    // ServerPlayer.sendSystemMessage(msg, true) for action-bar.
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        sp.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                        newMode ? "Eagle: fetch mode ON" : "Eagle: fetch mode OFF"),
                                true);
                    }
                    this.playSound(ModSounds.EAGLE_AMBIENT.get(), 0.5f,
                            newMode ? 1.4f : 0.9f);
                } else {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.playSound(ModSounds.EAGLE_AMBIENT.get(), 0.6f,
                            1.2f + this.random.nextFloat() * 0.4f);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    // ---------------------------------------------------------------
    // Step 9 — Save/Load
    // 26.1: Entity save API uses ValueOutput / ValueInput instead of
    // CompoundTag. Primitive helpers exist; for items we use the Codec
    // dispatch (output.store / input.read).
    // ---------------------------------------------------------------
    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);

        // Sanitize state on save; transient states (FETCHING etc.) depend on
        // goal-local data that doesn't survive a reload.
        int state = this.getEagleState();
        boolean persistent = (state == STATE_IDLE || state == STATE_PERCHED);
        output.putInt("EagleState", persistent ? state : STATE_IDLE);
        output.putBoolean("FetchMode", this.isFetchModeEnabled());

        // Fetch inventory: store via Codec list. Empty stacks dropped so
        // the saved blob stays tight.
        var inv = output.list("FetchInventory", SlotStack.CODEC);
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            ItemStack stack = fetchInventory.getItem(i);
            if (!stack.isEmpty()) inv.add(new SlotStack(i, stack));
        }

        if (perchPos != null) {
            output.putInt("PerchX", perchPos.getX());
            output.putInt("PerchY", perchPos.getY());
            output.putInt("PerchZ", perchPos.getZ());
        }
        if (nestPos != null) {
            output.putInt("NestX", nestPos.getX());
            output.putInt("NestY", nestPos.getY());
            output.putInt("NestZ", nestPos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);

        this.setEagleState(input.getInt("EagleState").orElse(STATE_IDLE));
        this.setFetchModeEnabled(input.getBooleanOr("FetchMode", true));

        fetchInventory.clearContent();
        input.list("FetchInventory", SlotStack.CODEC).ifPresent(list -> {
            for (SlotStack ss : list) {
                if (ss.slot() >= 0 && ss.slot() < fetchInventory.getContainerSize()) {
                    fetchInventory.setItem(ss.slot(), ss.stack());
                }
            }
        });

        java.util.Optional<Integer> px = input.getInt("PerchX");
        java.util.Optional<Integer> nx = input.getInt("NestX");
        this.perchPos = px.<BlockPos>map(x -> new BlockPos(x,
                input.getInt("PerchY").orElse(0),
                input.getInt("PerchZ").orElse(0))).orElse(null);
        this.nestPos = nx.<BlockPos>map(x -> new BlockPos(x,
                input.getInt("NestY").orElse(0),
                input.getInt("NestZ").orElse(0))).orElse(null);
    }

    // Helper record so we can serialise the inventory via Codec.list().
    private record SlotStack(int slot, ItemStack stack) {
        static final com.mojang.serialization.Codec<SlotStack> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(
                        com.mojang.serialization.Codec.INT.fieldOf("Slot").forGetter(SlotStack::slot),
                        ItemStack.CODEC.fieldOf("Stack").forGetter(SlotStack::stack)
                ).apply(inst, SlotStack::new));
    }

    @Nullable
    public BlockPos getPerchPos() { return this.perchPos; }
    public void setPerchPos(@Nullable BlockPos pos) { this.perchPos = pos; }

    @Nullable
    public BlockPos getNestPos() { return this.nestPos; }
    public void setNestPos(@Nullable BlockPos pos) { this.nestPos = pos; }

    // ---------------------------------------------------------------
    // Step 12 — Flying navigation
    // ---------------------------------------------------------------
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        /* TODO(port-26.1): setCanPassDoors removed in 26.1 */
        return nav;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
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
        if (this.isLocalInstanceAuthoritative()) {
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

        // Auto-wake: if perched and the owner returns to close range, stand up
        // so FollowOwnerGoal can take over. Without this the eagle would stay
        // sitting forever and need a manual right-click to rejoin.
        if (!this.level().isClientSide()
                && this.getEagleState() == STATE_PERCHED
                && this.isOrderedToSit()) {
            var owner = this.getOwner();
            if (owner != null
                    && owner.isAlive()
                    && owner.level().dimension() == this.level().dimension()
                    && this.distanceToSqr(owner) < 144.0) {  // 12 blocks
                this.setOrderedToSit(false);
                this.setEagleState(STATE_IDLE);
            }
        }

        // Watchdog: clear stale non-IDLE state when no goal is actually running.
        // STATE_HUNTING is owned by the hunt goal; if huntTarget is gone, the
        // state is dead. STATE_RETURNING with no inventory is similarly stale.
        // STATE_FETCHING is harder to validate here (target lives on the goal)
        // so we rely on the goal's own giveUpTimer + canContinueToUse to clear it.
        if (!this.level().isClientSide()) {
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
    public int getEagleState()                       { return this.entityData.get(EAGLE_STATE); }
    public void setEagleState(int state)             { this.entityData.set(EAGLE_STATE, state); }
    public boolean isFlying()                        { return this.entityData.get(IS_FLYING); }
    public boolean isFetchModeEnabled()              { return this.entityData.get(FETCH_MODE_ENABLED); }
    public void setFetchModeEnabled(boolean enabled) { this.entityData.set(FETCH_MODE_ENABLED, enabled); }

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
    // 26.1: spawnAtLocation now requires ServerLevel + ItemStack overload.
    public void dropFetchInventoryHere() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            ItemStack stack = fetchInventory.getItem(i);
            if (stack.isEmpty()) continue;
            this.spawnAtLocation(sl, stack);
            fetchInventory.setItem(i, ItemStack.EMPTY);
        }
    }

    // 26.1: dropEquipment now takes ServerLevel.
    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        dropFetchInventoryHere();
    }

    // ---------------------------------------------------------------
    // Required overrides
    // ---------------------------------------------------------------
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // Eagles don't spawn a baby directly when breeding — instead the
        // breeding ritual deposits an egg in their nest, and the egg later
        // hatches into an eaglet. See EagleBreedGoal + EagleNestBlockEntity.
        return null;
    }

    // Only mate with another tamed eagle owned by the same player. Without
    // this, any two in-love eagles in the world could breed.
    @Override
    public boolean canMate(Animal otherAnimal) {
        if (otherAnimal == this) return false;
        if (!(otherAnimal instanceof EagleEntity other)) return false;
        if (!this.isTame() || !other.isTame()) return false;
        if ((this.getOwner() == null ? null : this.getOwner().getUUID()) == null) return false;
        if (!(this.getOwner() == null ? null : this.getOwner().getUUID()).equals((other.getOwner() == null ? null : other.getOwner().getUUID()))) return false;
        return this.isInLove() && other.isInLove();
    }

    // TODO(port-26.1): LivingEntity.getScale() is final in 26.1. Baby
    // scaling is now driven by an Attributes.SCALE attribute and/or
    // setBaby() handling. The 0.55× shrink for eaglets is lost until
    // re-implemented via the new path.
    // @Override public float getScale() { return this.isBaby() ? 0.55f : 1.0f; }

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
        private net.minecraft.world.entity.projectile.arrow.AbstractArrow targetArrow;
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

            // Cold-start return is always allowed, even with fetch mode OFF —
            // this lets a mid-fetch abort still deliver what was already collected.
            if (!eagle.isFetchInventoryEmpty() && state == STATE_IDLE) {
                return true;
            }

            // Fresh fetches require fetch mode to be on.
            if (!eagle.isFetchModeEnabled()) return false;
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
                // Mode flipped off mid-fetch: abort the FETCH leg. If we've
                // already collected items, stop() exits to IDLE and the next
                // canUse cycle cold-starts a return so they aren't lost.
                if (!eagle.isFetchModeEnabled()) return false;
                return targetArrow != null && targetArrow.isAlive();
            }
            if (state == STATE_RETURNING) {
                // Return always continues — deliver what we have even with mode off.
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
                if (!leftover.isEmpty() && eagle.level() instanceof ServerLevel sl) {
                    // Shouldn't normally happen — canUse checked hasFetchSpace —
                    // but drop anything that didn't fit so it's not lost.
                    eagle.spawnAtLocation(sl, leftover);
                }

                targetArrow.discard();
                targetArrow = null;

                // Decide whether to chain to another arrow or head home.
                Player owner = (Player) eagle.getOwner();
                net.minecraft.world.entity.projectile.arrow.AbstractArrow next =
                        (eagle.isFetchModeEnabled() && eagle.hasFetchSpace() && owner != null)
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

        private static net.minecraft.world.entity.projectile.arrow.AbstractArrow findNearestArrow(EagleEntity eagle, Player owner) {
            double range = 24.0;
            return eagle.level().getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,
                    owner.getBoundingBox().inflate(range),
                    arrow -> arrow.tickCount > 5
                            && arrow.isInGround()
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
        // Give-up timeout: if the player can't finish the target within this
        // window, the eagle disengages so it isn't permanently locked.
        private static final int HUNT_MAX_TICKS = 1200; // 60 seconds

        private final EagleEntity eagle;
        private int orbitTick = 0;
        private int repathCooldown = 0;
        private int huntTicks = 0;

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
            huntTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = eagle.getHuntTarget();
            if (target == null) { stop(); return; }
            // Skip orbit work if the target's chunk is unloaded — wait until
            // it comes back into range rather than pathing into nowhere.
            if (!eagle.level().isLoaded(target.blockPosition())) return;

            // Hunt give-up: don't orbit forever if the kill never lands.
            if (++huntTicks > HUNT_MAX_TICKS) {
                if (eagle.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("Your eagle disengages."),
                            true);
                }
                stop();
                return;
            }

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

    /**
     * EaglePerchGoal — when the owner is far away (or offline / in another
     * dimension), the eagle flies to its claimed perch block and settles there
     * instead of hovering aimlessly. On arrival it enters STATE_PERCHED and
     * sits, reusing the existing sit animation.
     */
    static class EaglePerchGoal extends Goal {
        // Owner must be at least this far (or absent) for the eagle to leave
        // their side and go home. Sits comfortably outside FollowOwnerGoal's
        // 10-block follow distance so the two don't fight.
        private static final double OWNER_FAR_THRESHOLD_SQR = 32.0 * 32.0;
        // Y of the top of the crossbar in the perch model (14 / 16 of a block
        // above the perch's base position). Eagle's feet should land here.
        private static final double CROSSBAR_Y               = 14.0 / 16.0;
        // Once within this radius of the exact landing spot, we stop using
        // pathfinding and drive the eagle directly — flying nav can't path
        // to a thin decorative crossbar.
        private static final double APPROACH_RANGE_SQR       = 16.0;  // 4 blocks
        // Inside this radius the eagle snaps to position and sits.
        private static final double LAND_SNAP_RANGE_SQR      = 0.36;  // 0.6 blocks

        private final EagleEntity eagle;
        private int repathCooldown = 0;
        // Set true after the snap; canContinueToUse uses this to release the
        // goal cleanly without re-entering the landing logic.
        private boolean landed = false;

        EaglePerchGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!eagle.isTame() || eagle.isOrderedToSit()) return false;
            if (eagle.getEagleState() != STATE_IDLE) return false;
            BlockPos perch = eagle.getPerchPos();
            if (perch == null) return false;
            if (!eagle.level().isLoaded(perch)) return false;
            // Stop pointing at a perch that's been broken.
            if (!eagle.level().getBlockState(perch)
                    .is(net.frostytrix.fletcherstrestle.block.ModBlocks.EAGLE_PERCH.get())) {
                eagle.setPerchPos(null);
                return false;
            }
            return ownerFarOrAbsent();
        }

        @Override
        public boolean canContinueToUse() {
            if (landed) return false;
            if (!eagle.isTame() || eagle.isOrderedToSit()) return false;
            BlockPos perch = eagle.getPerchPos();
            if (perch == null) return false;
            if (!eagle.level().isLoaded(perch)) return false;
            // Owner came back — yield to FollowOwnerGoal mid-flight.
            return ownerFarOrAbsent();
        }

        @Override
        public void start() {
            repathCooldown = 0;
            landed = false;
        }

        @Override
        public void tick() {
            BlockPos perch = eagle.getPerchPos();
            if (perch == null) return;

            double landX = perch.getX() + 0.5;
            double landY = perch.getY() + CROSSBAR_Y;
            double landZ = perch.getZ() + 0.5;
            double distSqr = eagle.distanceToSqr(landX, landY, landZ);

            if (distSqr <= LAND_SNAP_RANGE_SQR) {
                // Close enough to land — snap the eagle's position exactly
                // onto the crossbar and put it to sleep.
                // 26.1: Entity.moveTo(x,y,z,yaw,pitch) was removed; use setPos
                // for the position and the existing rotation stays put.
                eagle.setPos(landX, landY, landZ);
                eagle.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                eagle.setEagleState(STATE_PERCHED);
                eagle.setOrderedToSit(true);
                eagle.getNavigation().stop();
                landed = true;
                return;
            }

            if (distSqr > APPROACH_RANGE_SQR) {
                // Far — let FlyingPathNavigation get us close.
                eagle.getMoveControl().setWantedPosition(landX, landY + 1.0, landZ, 1.2);
                if (--repathCooldown <= 0) {
                    eagle.getNavigation().moveTo(landX, landY + 1.0, landZ, 1.2);
                    repathCooldown = 12;
                }
            } else {
                // Close — pathfinding struggles with the thin crossbar target,
                // so drive directly via move control instead.
                eagle.getNavigation().stop();
                eagle.getMoveControl().setWantedPosition(landX, landY, landZ, 0.8);
            }
        }

        @Override
        public void stop() {
            // Landing side-effects happen in tick() now, so stop() is just
            // cleanup. If the goal was aborted (owner came back, perch broken),
            // we leave the eagle wherever it was — FollowOwnerGoal can take over.
            eagle.getNavigation().stop();
        }

        // Owner is "far" if absent (offline / null) or in another dimension
        // or beyond OWNER_FAR_THRESHOLD blocks. Inside that threshold,
        // FollowOwnerGoal handles them.
        private boolean ownerFarOrAbsent() {
            var owner = eagle.getOwner();
            if (owner == null) return true;
            if (!owner.isAlive()) return true;
            if (owner.level().dimension() != eagle.level().dimension()) return true;
            return eagle.distanceToSqr(owner) > OWNER_FAR_THRESHOLD_SQR;
        }
    }

    /**
     * EagleBreedGoal — replaces vanilla BreedGoal for eagles. When two of the
     * same owner's adult eagles enter love mode and a claimed nest is in
     * range, both fly to the nest, perform a short ritual, and add an egg.
     * The egg incubates inside the nest and hatches into a baby eagle.
     *
     * The "leader" eagle (the one with the lower UUID) is responsible for
     * actually depositing the egg, so we don't double-lay when both partners
     * arrive on the same tick.
     */
    static class EagleBreedGoal extends Goal {
        private static final int RITUAL_TICKS_REQUIRED = 40;          // 2 seconds at the nest
        private static final double NEST_SEARCH_RADIUS = 24.0;        // blocks
        private static final double PARTNER_SEARCH_RADIUS = 24.0;     // blocks
        private static final double NEST_ARRIVAL_DIST_SQR = 25.0;     // 5 blocks — generous so both birds count
        private static final int AGE_COOLDOWN_AFTER_BREED = 6000;     // 5 minutes

        // Cooldown applied to both birds if the ritual ends without an egg.
        // Stops the player from spam-feeding the same pair every 30 seconds
        // when the ritual can't complete (e.g., navigation problems).
        private static final int FAILED_ATTEMPT_COOLDOWN_TICKS = 1200;  // 1 minute

        private final EagleEntity eagle;
        @Nullable private EagleEntity partner;
        @Nullable private BlockPos nestPos;
        private int ritualTicks = 0;
        private int repathCooldown = 0;
        private boolean eggLaid = false;

        EagleBreedGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!eagle.isTame()) return false;
            if (eagle.isOrderedToSit()) return false;
            if (eagle.getEagleState() != STATE_IDLE) return false;
            if (!eagle.isInLove()) return false;
            if (eagle.isBaby()) return false;

            partner = findPartner();
            if (partner == null) return false;

            nestPos = findNest();
            return nestPos != null;
        }

        @Override
        public void start() {
            ritualTicks = 0;
            repathCooldown = 0;
            eggLaid = false;
            if (nestPos != null) eagle.setNestPos(nestPos);
        }

        @Override
        public boolean canContinueToUse() {
            if (partner == null || !partner.isAlive()) return false;
            if (nestPos == null) return false;
            if (!eagle.isInLove() || !partner.isInLove()) return false;
            if (!isValidNest(nestPos)) return false;
            // Run until the ritual succeeds; tick() ends us via stop() on lay.
            return ritualTicks < RITUAL_TICKS_REQUIRED;
        }

        @Override
        public void tick() {
            if (nestPos == null || partner == null) return;

            // Stagger the two birds so they don't fight for the same point.
            // UUID compare picks a deterministic side per bird.
            boolean rightSide = eagle.getUUID().compareTo(partner.getUUID()) < 0;
            double offsetX = rightSide ? 1.0 : -1.0;

            double centerX = nestPos.getX() + 0.5;
            double landY   = nestPos.getY() + 4.0 / 16.0;  // top of the nest rim
            double centerZ = nestPos.getZ() + 0.5;
            double navX    = centerX + offsetX;

            eagle.getMoveControl().setWantedPosition(navX, landY, centerZ, 1.0);
            if (--repathCooldown <= 0) {
                // Aim slightly above so nav has a target it can reach.
                eagle.getNavigation().moveTo(navX, landY + 0.5, centerZ, 1.0);
                repathCooldown = 8;
            }

            boolean selfAtNest    = eagle.distanceToSqr(centerX, landY, centerZ)   < NEST_ARRIVAL_DIST_SQR;
            boolean partnerAtNest = partner.distanceToSqr(centerX, landY, centerZ) < NEST_ARRIVAL_DIST_SQR;
            if (selfAtNest && partnerAtNest) {
                ritualTicks++;
                if (ritualTicks >= RITUAL_TICKS_REQUIRED) {
                    layEgg();
                }
            }
        }

        // Either partner can deposit. The early isInLove check prevents
        // a double-lay because layEgg also resets both birds' love mode —
        // the partner's next call short-circuits before adding a second egg.
        private void layEgg() {
            if (nestPos == null || partner == null) return;
            if (!eagle.isInLove() || !partner.isInLove()) return;
            if (!(eagle.level().getBlockEntity(nestPos) instanceof EagleNestBlockEntity nest)) return;
            if (!nest.hasEggSpace()) return;

            nest.addEgg(eagle.level().getGameTime());
            nest.addBreedingEagle(eagle.getUUID());
            nest.addBreedingEagle(partner.getUUID());

            eagle.resetLove();
            partner.resetLove();
            eagle.setAge(AGE_COOLDOWN_AFTER_BREED);
            partner.setAge(AGE_COOLDOWN_AFTER_BREED);
            eggLaid = true;

            if (eagle.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("Your eagles laid an egg."),
                        true);
            }
        }

        @Override
        public void stop() {
            // Failed attempt: apply a short cooldown to both birds so the
            // player can't just immediately spam-feed the same pair into
            // another doomed ritual. Successful attempts already get the
            // longer AGE_COOLDOWN_AFTER_BREED from layEgg.
            if (!eggLaid) {
                eagle.resetLove();
                eagle.setAge(FAILED_ATTEMPT_COOLDOWN_TICKS);
                if (partner != null && partner.isAlive()) {
                    partner.resetLove();
                    partner.setAge(FAILED_ATTEMPT_COOLDOWN_TICKS);
                }
            }
            partner = null;
            nestPos = null;
            eagle.getNavigation().stop();
        }

        @Nullable
        private EagleEntity findPartner() {
            return eagle.level().getEntitiesOfClass(
                            EagleEntity.class,
                            eagle.getBoundingBox().inflate(PARTNER_SEARCH_RADIUS),
                            e -> e != eagle
                                    && e.isTame()
                                    && !e.isBaby()
                                    && e.isInLove()
                                    && (e.getOwner() == null ? null : e.getOwner().getUUID()) != null
                                    && (e.getOwner() == null ? null : e.getOwner().getUUID())
                                            .equals(eagle.getOwner() == null ? null : eagle.getOwner().getUUID()))
                    .stream()
                    .min((a, b) -> Double.compare(eagle.distanceToSqr(a), eagle.distanceToSqr(b)))
                    .orElse(null);
        }

        @Nullable
        private BlockPos findNest() {
            // Prefer the eagle's bound nestPos if still valid.
            if (eagle.getNestPos() != null && isValidNest(eagle.getNestPos())) {
                return eagle.getNestPos();
            }
            // Otherwise scan a small box around the eagle for an owner-claimed nest.
            int r = (int) NEST_SEARCH_RADIUS;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            BlockPos eaglePos = eagle.blockPosition();
            BlockPos best = null;
            double bestDistSqr = Double.MAX_VALUE;
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        cursor.set(eaglePos.getX() + dx, eaglePos.getY() + dy, eaglePos.getZ() + dz);
                        if (isValidNest(cursor)) {
                            double d = cursor.distSqr(eaglePos);
                            if (d < bestDistSqr) {
                                best = cursor.immutable();
                                bestDistSqr = d;
                            }
                        }
                    }
                }
            }
            return best;
        }

        private boolean isValidNest(BlockPos pos) {
            if (!eagle.level().isLoaded(pos)) return false;
            if (!eagle.level().getBlockState(pos)
                    .is(net.frostytrix.fletcherstrestle.block.ModBlocks.EAGLE_NEST.get())) return false;
            if (!(eagle.level().getBlockEntity(pos) instanceof EagleNestBlockEntity nest)) return false;
            if (!nest.isClaimed()) return false;
            java.util.UUID ownerId = eagle.getOwner() == null ? null : eagle.getOwner().getUUID();
            if (ownerId == null) return false;
            if (!ownerId.equals(nest.getOwnerUUID())) return false;
            return nest.hasEggSpace();
        }
    }

    /**
     * EagleNestPatrolGoal — wild eagles that worldgen bound to a nest pick
     * random points within a "territory" around the nest and fly to them.
     * Combines "stay near home" with "wander around" so they don't just
     * hover in one spot.
     *
     * Only active for UNTAMED eagles with a nestPos. Tamed eagles have their
     * own perch/follow behaviors.
     */
    static class EagleNestPatrolGoal extends Goal {
        // Territory radius. Eagles pick random patrol points inside this disc
        // around their nest (XZ) with a small vertical range.
        private static final double PATROL_RADIUS    = 20.0;
        private static final double VERTICAL_RANGE   = 6.0;
        // Ticks between picking new patrol points. Random within this range.
        private static final int    MIN_REST_TICKS   = 80;   // 4 sec
        private static final int    MAX_REST_TICKS   = 240;  // 12 sec

        private final EagleEntity eagle;
        private double tx, ty, tz;
        private int restTicks = 0;
        private int repathCooldown = 0;

        EagleNestPatrolGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Wild eagles only — tamed eagles use the perch/follow goals.
            if (eagle.isTame()) return false;
            if (eagle.isOrderedToSit()) return false;
            if (eagle.getNestPos() == null) return false;
            // Wait between patrol legs so they don't constantly recompute.
            if (restTicks > 0) { restTicks--; return false; }
            pickNextTarget();
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (eagle.isTame()) return false;
            if (eagle.getNestPos() == null) return false;
            // Reached the patrol point — let stop() reset rest timer.
            return eagle.distanceToSqr(tx, ty, tz) > 9.0;  // 3 blocks
        }

        @Override
        public void start() {
            repathCooldown = 0;
        }

        @Override
        public void tick() {
            eagle.getMoveControl().setWantedPosition(tx, ty, tz, 1.0);
            if (--repathCooldown <= 0) {
                eagle.getNavigation().moveTo(tx, ty, tz, 1.0);
                repathCooldown = 16;
            }
        }

        @Override
        public void stop() {
            eagle.getNavigation().stop();
            restTicks = MIN_REST_TICKS
                    + eagle.getRandom().nextInt(MAX_REST_TICKS - MIN_REST_TICKS);
        }

        private void pickNextTarget() {
            BlockPos nest = eagle.getNestPos();
            if (nest == null) return;
            double angle = eagle.getRandom().nextDouble() * Math.PI * 2.0;
            double dist  = 3.0 + eagle.getRandom().nextDouble() * PATROL_RADIUS;
            tx = nest.getX() + 0.5 + Math.cos(angle) * dist;
            tz = nest.getZ() + 0.5 + Math.sin(angle) * dist;
            // Patrol mostly above the nest with some altitude variation.
            ty = nest.getY() + 2.0
                    + eagle.getRandom().nextDouble() * VERTICAL_RANGE;
        }
    }
}