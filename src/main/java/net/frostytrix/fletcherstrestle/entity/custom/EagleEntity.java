package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.block.entity.EagleNestBlockEntity;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleFlightControl;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleFollowOwnerGoal;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleLanding;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleNavigation;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EaglePanicGoal;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleSoarGoal;
import net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleStoopGoal;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class EagleEntity extends TamableAnimal {

    // ---------------------------------------------------------------
    // Synced data: declaration order is critical.
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

    // Set while the eagle comes in to land, so the model flares its wings.
    private static final EntityDataAccessor<Boolean> LANDING =
            SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.BOOLEAN);

    // What it's holding in its talons, for the renderer: prey, a fish, fetched arrows.
    private static final EntityDataAccessor<ItemStack> CARRIED =
            SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.ITEM_STACK);

    // Asleep for the night: head tucked back.
    private static final EntityDataAccessor<Boolean> ROOSTING =
            SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.BOOLEAN);

    // Feathers set with honeycomb wax: no moulting until rain or water washes it out.
    private static final EntityDataAccessor<Boolean> WAXED =
            SynchedEntityData.defineId(EagleEntity.class, EntityDataSerializers.BOOLEAN);

    // Eagle states (stored as int for sync simplicity)
    public static final int STATE_IDLE = 0;
    public static final int STATE_PERCHED = 1;
    public static final int STATE_FETCHING = 2;
    public static final int STATE_HUNTING = 3;
    public static final int STATE_RETURNING = 4;

    // Hunt target, not synced, server-only logic
    @Nullable
    private LivingEntity huntTarget = null;

    // Position of the block this eagle is bound to as its "home perch".
    // Server-side only; saved to NBT. Null when the eagle has no perch.
    @Nullable
    private BlockPos perchPos = null;

    // Ticks left on the landing flag; landing code refreshes it every tick it's flaring.
    private int landingTicks = 0;

    // The middle of the territory a roaming eagle soars over: where a tame one
    // last saw its owner, or where a wild one first appeared. Saved.
    @Nullable
    private Vec3 home = null;

    // Ticks left on a whistle recall, during which it comes straight in to its owner.
    private int recallTicks = 0;

    // Game time before which a wild eagle won't hunt again. Server only.
    private long nextWildHunt = 0L;

    // A wild eagle's kill, carried back to the nest. Server only, not saved:
    // a reload mid-flight just means the prey was eaten on the way.
    private ItemStack carriedPrey = ItemStack.EMPTY;

    // Owner's order: stoop on whatever attacks them. Saved.
    private boolean guarding = true;

    // Ticks spent standing still; a still eagle at night roosts.
    private int stillTicks = 0;

    // Game time of its next moult, when it sheds a feather. Server only.
    private long nextMoult = 0L;

    // Whether the owner has been told it's too hurt to fly, so it's said once.
    private boolean injuryReported = false;

    /** Below this share of its health it can't fly. */
    private static final float INJURED_HEALTH = 0.3f;
    private static final int TAME_MOULT_MIN = 6000;
    private static final int TAME_MOULT_SPREAD = 6000;
    private static final int WILD_MOULT_MIN = 12000;
    private static final int WILD_MOULT_SPREAD = 12000;

    // Client-side smoothed horizontal speed. Drives the wing-flap amplitude in
    // the model; smoothing keeps it from jittering frame-to-frame (raw client
    // deltaMovement is noisy). Not saved, client-only.
    private float flapAmount = 0f;

    // Client-side accumulated flap phase. Advanced each tick by a speed-scaled
    // frequency; because it's accumulated (not ageInTicks * freq), the phase
    // never jumps when the frequency changes, so the flap can speed up with
    // motion without the jitter that frequency-modulating a shared sine caused.
    // The "O" copies hold last tick's values so the renderer can interpolate.
    private float flapPhase = 0f;
    private float flapPhaseO = 0f;

    // Client-side flight attitude, smoothed per tick, in degrees: pitch follows
    // the flight path (+ = nose down), bank rolls into turns (+ = right wing
    // down). Flare is 0..1, rising as the eagle drops near-vertically to land.
    private float flightPitch = 0f;
    private float flightPitchO = 0f;
    private float bank = 0f;
    private float bankO = 0f;
    private float flare = 0f;
    private float flareO = 0f;

    // Position of the nest this eagle considers its breeding ground.
    // Server-side only; saved to NBT. Null when not bound to a nest.
    @Nullable
    private BlockPos nestPos = null;

    // Fetch inventory: 16 slots so the eagle can carry up to 16 arrows in
    // a single trip, with room for up to 16 different arrow types (one type
    // per slot if needed; matching types still stack). Capacity is enforced
    // by total item count via hasFetchSpace(), not by full slots.
    private static final int FETCH_INVENTORY_SIZE = 16;
    private static final int FETCH_CAPACITY = 16;
    private final SimpleContainer fetchInventory = new SimpleContainer(FETCH_INVENTORY_SIZE);

    // Hysteresis counters for the IS_FLYING flag. With no gravity, the eagle
    // grazes the ground each tick and onGround() flickers true/false, which
    // would otherwise re-trigger the flying animation every frame.
    private int groundedTicks = 0;
    private int airborneTicks = 0;

    public EagleEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        // Steered flight: see EagleFlightControl and EagleNavigation.
        this.moveControl = new EagleFlightControl(this);
        // Gravity off: the flight control owns all motion, gliding down when
        // it wants to come down. Real birds don't drop like rocks.
        this.setNoGravity(true);
        // Eagles never despawn once tamed
        this.setPersistenceRequired();
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER, -1f);
        this.setPathfindingMalus(PathType.OPEN, 0f);
    }

    // ---------------------------------------------------------------
    // Spawn rules: keeps wild eagles to high, sunlit mountain ridges
    // rather than spawning under leaves at sea level. Wired up in
    // FletcherTrestle.onRegisterSpawnPlacements.
    // ---------------------------------------------------------------
    public static boolean checkEagleSpawnRules(
            EntityType<EagleEntity> type,
            net.minecraft.world.level.LevelAccessor level,
            net.minecraft.world.entity.MobSpawnType reason,
            BlockPos pos,
            net.minecraft.util.RandomSource random) {
        // Natural spawning is config-gated (off by default while the model is
        // WIP). This predicate runs at spawn-attempt time, when the SERVER
        // config is loaded: unlike RegisterSpawnPlacementsEvent, so it's the
        // correct place to read the flag.
        if (!net.frostytrix.fletcherstrestle.config.FletcherConfig.EAGLES_NATURAL_SPAWNING.get()) return false;
        // Bottom layers / cave biomes are off-limits regardless of biome tag.
        if (pos.getY() < 80) return false;
        if (!level.canSeeSky(pos)) return false;
        // Daylight only: keeps them from popping in at night.
        return level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) >= 12;
    }

    // ---------------------------------------------------------------
    // Attributes
    // ---------------------------------------------------------------
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.17)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    // ---------------------------------------------------------------
    // Synced data registration
    // ---------------------------------------------------------------
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); // TamableAnimal registers its own fields first
        builder.define(EAGLE_STATE, STATE_IDLE);
        builder.define(IS_FLYING, false);
        builder.define(FETCH_MODE_ENABLED, true);
        builder.define(LANDING, false);
        builder.define(CARRIED, ItemStack.EMPTY);
        builder.define(ROOSTING, false);
        builder.define(WAXED, false);
    }

    // ---------------------------------------------------------------
    // AI Goal stack
    // ---------------------------------------------------------------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EaglePanicGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new EagleFetchGoal(this));
        this.goalSelector.addGoal(3, new net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleDefendNestGoal(this));
        this.goalSelector.addGoal(4, new EagleStoopGoal(this));
        this.goalSelector.addGoal(4, new EagleBreedGoal(this));
        this.goalSelector.addGoal(5, new EaglePerchGoal(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.0,
                Ingredient.of(Items.RABBIT, Items.COD, Items.SALMON), false));
        this.goalSelector.addGoal(5, new net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleFishingGoal(this));
        this.goalSelector.addGoal(6, new EagleFollowOwnerGoal(this));
        // Down for the night or out of the weather, then thermals in daylight:
        // wild eagles over their territory, tame ones whose owner is away.
        this.goalSelector.addGoal(7, new net.frostytrix.fletcherstrestle.entity.ai.eagle.EagleRoostGoal(this));
        this.goalSelector.addGoal(8, new EagleSoarGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    // ---------------------------------------------------------------
    // Taming
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
                        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            net.minecraft.advancements.CriteriaTriggers.TAME_ANIMAL.trigger(sp, this);
                        }
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

        // Honeycomb sets its feathers, like waxing copper: no more moulting.
        if (stack.is(Items.HONEYCOMB) && this.isOwnedBy(player)) {
            if (this.isWaxed()) return InteractionResult.PASS;
            if (!this.level().isClientSide) {
                stack.consume(1, player);
                this.entityData.set(WAXED, true);
                this.playSound(net.minecraft.sounds.SoundEvents.HONEYCOMB_WAX_ON, 1.0f, 1.0f);
                waxParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    net.frostytrix.fletcherstrestle.progression.ModCriteria.EAGLE.get().trigger(sp,
                            net.frostytrix.fletcherstrestle.progression.EagleTrigger.WAXED);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // A splash from a water bucket rinses the wax out again.
        if (stack.is(Items.WATER_BUCKET) && this.isWaxed() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide) {
                washOff();
                this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Feeding a hurt eagle heals it, before any thought of breeding.
        if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
            if (!this.level().isClientSide) {
                stack.consume(1, player);
                this.heal(stack.is(Items.RABBIT) ? 6.0f : 4.0f);
                this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 0.8f, 1.2f);
                this.level().broadcastEntityEvent(this, (byte) 7);
                if (this.isBaby() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    net.frostytrix.fletcherstrestle.progression.ModCriteria.EAGLE.get().trigger(sp,
                            net.frostytrix.fletcherstrestle.progression.EagleTrigger.FED_EAGLET);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (this.isFood(stack) && this.isBaby() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.frostytrix.fletcherstrestle.progression.ModCriteria.EAGLE.get().trigger(sp,
                    net.frostytrix.fletcherstrestle.progression.EagleTrigger.FED_EAGLET);
        }

        // Owner empty-hand interactions:
        //  - sneak + right-click → toggle fetch mode (auto-fetch on/off)
        //  - right-click          → toggle sit
        if (this.isOwnedBy(player) && stack.isEmpty()) {
            if (!this.level().isClientSide) {
                if (player.isShiftKeyDown()) {
                    boolean newMode = !this.isFetchModeEnabled();
                    this.setFetchModeEnabled(newMode);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    newMode ? "message.fletcherstrestle.eagle_fetch_on" : "message.fletcherstrestle.eagle_fetch_off"),
                            true);
                    this.playSound(ModSounds.EAGLE_AMBIENT.get(), 0.5f,
                            newMode ? 1.4f : 0.9f);
                } else {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.playSound(ModSounds.EAGLE_AMBIENT.get(), 0.6f,
                            1.2f + this.random.nextFloat() * 0.4f);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    // ---------------------------------------------------------------
    // Save/Load
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
        tag.putBoolean("FetchMode", this.isFetchModeEnabled());
        tag.putBoolean("Guard", this.guarding);
        tag.putBoolean("Waxed", this.isWaxed());

        if (perchPos != null) {
            tag.putInt("PerchX", perchPos.getX());
            tag.putInt("PerchY", perchPos.getY());
            tag.putInt("PerchZ", perchPos.getZ());
        }
        if (nestPos != null) {
            tag.putInt("NestX", nestPos.getX());
            tag.putInt("NestY", nestPos.getY());
            tag.putInt("NestZ", nestPos.getZ());
        }
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setEagleState(tag.getInt("EagleState"));
        // Default to true for eagles tamed before this field existed.
        this.setFetchModeEnabled(!tag.contains("FetchMode") || tag.getBoolean("FetchMode"));
        this.guarding = !tag.contains("Guard") || tag.getBoolean("Guard");
        this.entityData.set(WAXED, tag.getBoolean("Waxed"));

        fetchInventory.clearContent();
        ListTag items = tag.getList("FetchInventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag slotTag = items.getCompound(i);
            int slot = slotTag.getByte("Slot") & 0xFF;
            if (slot >= fetchInventory.getContainerSize()) continue;
            ItemStack.parse(this.registryAccess(), slotTag)
                    .ifPresent(s -> fetchInventory.setItem(slot, s));
        }

        if (tag.contains("PerchX")) {
            this.perchPos = new BlockPos(tag.getInt("PerchX"), tag.getInt("PerchY"), tag.getInt("PerchZ"));
        } else {
            this.perchPos = null;
        }
        if (tag.contains("NestX")) {
            this.nestPos = new BlockPos(tag.getInt("NestX"), tag.getInt("NestY"), tag.getInt("NestZ"));
        } else {
            this.nestPos = null;
        }
        this.home = tag.contains("HomeX")
                ? new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"))
                : null;
    }

    @Nullable
    public BlockPos getPerchPos() {
        return this.perchPos;
    }

    public void setPerchPos(@Nullable BlockPos pos) {
        this.perchPos = pos;
    }

    @Nullable
    public BlockPos getNestPos() {
        return this.nestPos;
    }

    public void setNestPos(@Nullable BlockPos pos) {
        this.nestPos = pos;
    }

    // ---------------------------------------------------------------
    // Flying navigation
    // ---------------------------------------------------------------
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new EagleNavigation(this, level);
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
    // Travel & gravity suppression
    // ---------------------------------------------------------------

    // The flight control sets the eagle's velocity outright every tick (steering,
    // braking, walking), so travel() only has to apply it: no vanilla gravity,
    // no input-driven acceleration, no blanket drag fighting the steering.
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater() || this.isInLava()) {
                this.moveRelative(0.02f, input);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(this.isInWater() ? 0.8 : 0.5));
            } else {
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.onGround()) {
                    // Glanced off something: lose speed and lift over it.
                    Vec3 v = this.getDeltaMovement();
                    this.setDeltaMovement(v.x * 0.5, Math.max(v.y, 0.08), v.z * 0.5);
                }
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
    // Tick: manage IS_FLYING synced flag
    // ---------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();

        // Gravity stays off: travel() handles all motion. If a vanilla code
        // path flipped it back on (rare but possible), force it off again.
        if (!this.isNoGravity()) {
            this.setNoGravity(true);
        }

        // Smooth the horizontal speed the flap animation reads from (client),
        // and advance the accumulated flap phase by a speed-scaled frequency.
        if (this.level().isClientSide) {
            tickFlightAnimation();
            // A waxed eagle catches the light now and then.
            if (this.isWaxed() && this.random.nextInt(80) == 0) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                        this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0, 0);
            }
        }

        // Robust ground check: vanilla onGround() flickers because with no
        // gravity the eagle barely loads against the block. We extend the
        // bounding box ~0.15 blocks down and test for any collision,
        // that stays stable through small vertical nudges.
        boolean grounded = isGroundedRobust();
        boolean airborne = !grounded;

        if (!this.level().isClientSide) {
            tickServerState();
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
            // Was flying: only switch to "not flying" after 4+ ticks grounded
            shouldFly = groundedTicks < 4;
        } else {
            // Was grounded: require 8+ ticks airborne before flipping to
            // flying so a brief hop doesn't trigger
            // the flap animation while the eagle is essentially standing still.
            shouldFly = airborneTicks >= 8;
        }
        if (shouldFly != currentlyFlying) {
            this.entityData.set(IS_FLYING, shouldFly);
        }

        // Auto-wake: if perched and the owner returns to close range, stand up
        // so the follow goal can take over. Without this the eagle would stay
        // sitting forever and need a manual right-click to rejoin.
        if (!this.level().isClientSide
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
        if (!this.level().isClientSide) {
            int curState = this.getEagleState();
            if (curState == STATE_HUNTING && huntTarget == null) {
                this.setEagleState(STATE_IDLE);
            } else if (curState == STATE_RETURNING && isFetchInventoryEmpty()) {
                this.setEagleState(STATE_IDLE);
            }
        }
    }

    private void tickServerState() {
        if (this.landingTicks > 0 && --this.landingTicks == 0) {
            this.entityData.set(LANDING, false);
        }
        if (this.recallTicks > 0) {
            this.recallTicks--;
        }
        if (this.nextWildHunt == 0L) {
            this.nextWildHunt = this.level().getGameTime() + 1200 + this.random.nextInt(3600);
        }

        // Moulting: now and then a feather comes loose, for the fletcher.
        long now = this.level().getGameTime();
        if (this.nextMoult == 0L) {
            this.nextMoult = now + nextMoultDelay();
        } else if (now >= this.nextMoult) {
            if (!this.isBaby() && !this.isWaxed()) {
                this.spawnAtLocation(new ItemStack(net.frostytrix.fletcherstrestle.item.ModItems.BROWN_FEATHER.get()));
            }
            this.nextMoult = now + nextMoultDelay();
        }

        // Wax doesn't survive rain or a swim.
        if (this.isWaxed() && this.isInWaterRainOrBubble()) {
            washOff();
        }

        // Too hurt to fly: tell the owner once. Wild eagles mend on their own,
        // slowly; a tame one waits to be fed.
        if (this.isInjured()) {
            if (!this.injuryReported && this.isTame() && this.getOwner() instanceof Player owner) {
                owner.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.fletcherstrestle.eagle_injured", this.getName()), true);
                this.injuryReported = true;
            }
            if (!this.isTame() && this.tickCount % 600 == 0) {
                this.heal(1.0f);
            }
        } else {
            this.injuryReported = false;
        }

        // Roosting: standing still at night, it tucks its head in and sleeps.
        Vec3 v = this.getDeltaMovement();
        boolean still = !this.isFlying() && v.x * v.x + v.z * v.z < 1.0E-4;
        this.stillTicks = still ? this.stillTicks + 1 : 0;
        boolean roosting = this.stillTicks > 60 && this.level().isNight();
        if (roosting != this.entityData.get(ROOSTING)) {
            this.entityData.set(ROOSTING, roosting);
        }

        if (this.tickCount % 5 == 0) {
            ItemStack shown = !this.carriedPrey.isEmpty() ? this.carriedPrey : firstCarried();
            if (!ItemStack.matches(shown, this.entityData.get(CARRIED))) {
                this.entityData.set(CARRIED, shown.copyWithCount(1));
            }
        }
        // A tame eagle's territory follows its owner, so if they log off it
        // keeps to the area where it last saw them.
        LivingEntity owner = this.isTame() ? this.getOwner() : null;
        if (owner != null && owner.level() == this.level()) {
            this.home = owner.position();
        } else if (this.home == null) {
            this.home = this.position();
        }
    }

    // Returns true if the eagle's hitbox is within ~0.3 blocks of a solid
    // surface below it. More reliable than onGround() when gravity is off,
    // because it doesn't depend on the entity having had downward motion
    // that got capped on the same tick. The 0.3 buffer also tolerates the
    // brief upward nudges near the ground.
    private boolean isGroundedRobust() {
        if (this.onGround()) return true;
        net.minecraft.world.phys.AABB bb = this.getBoundingBox();
        net.minecraft.world.phys.AABB probe = new net.minecraft.world.phys.AABB(
                bb.minX, bb.minY - 0.3, bb.minZ,
                bb.maxX, bb.minY, bb.maxZ);
        return !this.level().noCollision(this, probe);
    }

    @Override
    protected boolean isFlapping() {
        return this.isFlying();
    }

    // ---------------------------------------------------------------
    // Public API used by goals and renderer
    // ---------------------------------------------------------------
    public int getEagleState() {
        return this.entityData.get(EAGLE_STATE);
    }

    public void setEagleState(int state) {
        this.entityData.set(EAGLE_STATE, state);
    }

    public boolean isFlying() {
        return this.entityData.get(IS_FLYING);
    }

    /** Flags a landing in progress for the next few ticks, for the wing flare. */
    public void setLanding(boolean landing) {
        if (landing) {
            this.landingTicks = 3;
            if (!this.entityData.get(LANDING)) this.entityData.set(LANDING, true);
        }
    }

    public boolean isLanding() {
        return this.entityData.get(LANDING);
    }

    /** Centre of the territory it soars over: its nest if it has one. */
    public Vec3 getHome() {
        if (this.nestPos != null) return Vec3.atCenterOf(this.nestPos);
        return this.home != null ? this.home : this.position();
    }

    /**
     * Whether it should range over its territory rather than keep company:
     * always for a wild eagle; for a tame one, only while its owner is away
     * and it has no perch to wait on.
     */
    public boolean wantsToRoam() {
        if (!this.isTame()) return true;
        LivingEntity owner = this.getOwner();
        boolean ownerHere = owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level().dimension() == this.level().dimension();
        return !ownerHere && this.perchPos == null;
    }

    /** A whistle recall: drop what it's doing and come in to land by its owner. */
    public void recall() {
        this.recallTicks = 300;
        this.setHuntTarget(null);
        if (this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
        int state = this.getEagleState();
        // Perched or out fetching: come now. Anything it was carrying still gets delivered.
        if (state == STATE_PERCHED || state == STATE_FETCHING) {
            this.setEagleState(STATE_IDLE);
        }
    }

    public boolean isRecalled() {
        return this.recallTicks > 0;
    }

    public void clearRecall() {
        this.recallTicks = 0;
    }

    /** Hurt badly enough that it can't fly: it hops on the ground until it's fed back to health. */
    public boolean isInjured() {
        return this.isAlive() && this.getHealth() < this.getMaxHealth() * INJURED_HEALTH;
    }

    private int nextMoultDelay() {
        return this.isTame()
                ? TAME_MOULT_MIN + this.random.nextInt(TAME_MOULT_SPREAD)
                : WILD_MOULT_MIN + this.random.nextInt(WILD_MOULT_SPREAD);
    }

    /** Hit in the air, it lets go of whatever it was carrying. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && amount > 0.0f && !this.level().isClientSide && this.isFlying()) {
            if (!this.carriedPrey.isEmpty()) {
                this.spawnAtLocation(this.carriedPrey);
                this.carriedPrey = ItemStack.EMPTY;
            }
            this.dropFetchInventoryHere();
        }
        return hurt;
    }

    public boolean isWaxed() {
        return this.entityData.get(WAXED);
    }

    private void washOff() {
        this.entityData.set(WAXED, false);
        this.playSound(net.minecraft.sounds.SoundEvents.AXE_WAX_OFF, 0.8f, 1.0f);
        waxParticles(net.minecraft.core.particles.ParticleTypes.WAX_OFF);
    }

    private void waxParticles(net.minecraft.core.particles.SimpleParticleType type) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(type, this.getX(), this.getY(0.5), this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** The item to draw in its talons. */
    public ItemStack getCarried() {
        return this.entityData.get(CARRIED);
    }

    public ItemStack getCarriedPrey() {
        return this.carriedPrey;
    }

    public void setCarriedPrey(ItemStack prey) {
        this.carriedPrey = prey;
    }

    private ItemStack firstCarried() {
        for (int i = 0; i < fetchInventory.getContainerSize(); i++) {
            ItemStack stack = fetchInventory.getItem(i);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    public boolean isRoosting() {
        return this.entityData.get(ROOSTING);
    }

    /** Night, a storm, or rain where it is: no weather for soaring. */
    public boolean shouldShelter() {
        return this.level().isNight() || this.level().isThundering() || this.level().isRainingAt(this.blockPosition());
    }

    public boolean isGuarding() {
        return this.guarding;
    }

    public void setGuarding(boolean guarding) {
        this.guarding = guarding;
    }

    public long getNextWildHunt() {
        return this.nextWildHunt;
    }

    public void setNextWildHunt(long gameTime) {
        this.nextWildHunt = gameTime;
    }

    /**
     * Client side, once per tick: works out how hard the eagle is flapping and
     * how it's angled in the air. Motion is read from the change in position,
     * which the client interpolates smoothly, rather than from the synced
     * velocity, which is noisy for a mob.
     */
    private void tickFlightAnimation() {
        this.flapPhaseO = this.flapPhase;
        this.flightPitchO = this.flightPitch;
        this.bankO = this.bank;
        this.flareO = this.flare;

        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
        float climb = (float) dy;

        if (this.isFlying()) {
            // Climbing takes real effort, so it counts toward the flap; a dive is a glide.
            float effort = horizontal + Math.max(0f, climb) * 1.5f;
            this.flapAmount += (effort - this.flapAmount) * 0.12f;
            float moveN = Math.min(1f, this.flapAmount / 0.30f);
            this.flapPhase += 0.15f + moveN * 0.20f;   // 0.15 soaring → 0.35 max

            // Nose follows the flight path, plus a little lean into speed.
            float path = horizontal > 0.02f
                    ? (float) Math.toDegrees(Math.atan2(-climb, horizontal)) : 0f;
            float pitchTarget = Mth.clamp(path * 0.6f + Math.min(horizontal * 40f, 15f), -30f, 35f);
            this.flightPitch += (pitchTarget - this.flightPitch) * 0.15f;

            // Roll into turns, in proportion to how fast the heading is changing.
            float turn = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            this.bank += (Mth.clamp(turn * 3.0f, -35f, 35f) - this.bank) * 0.15f;

            // Dropping almost straight down: wings up and forward, feet out.
            boolean flaring = this.isLanding() || (climb < -0.03f && horizontal < 0.06f);
            this.flare += ((flaring ? 1f : 0f) - this.flare) * 0.2f;
        } else {
            this.flapAmount += (0f - this.flapAmount) * 0.2f;
            this.flightPitch += (0f - this.flightPitch) * 0.3f;
            this.bank += (0f - this.bank) * 0.3f;
            this.flare += (0f - this.flare) * 0.3f;
        }
    }

    /** Client-side smoothed flapping effort, for the wing-flap animation. */
    public float getFlapAmount() {
        return this.flapAmount;
    }

    /** Client-side accumulated flap phase (radians), interpolated to the frame. */
    public float getFlapPhase(float partialTick) {
        return Mth.lerp(partialTick, this.flapPhaseO, this.flapPhase);
    }

    /** Flight pitch in degrees, + = nose down, interpolated to the frame. */
    public float getFlightPitch(float partialTick) {
        return Mth.lerp(partialTick, this.flightPitchO, this.flightPitch);
    }

    /** Bank in degrees, + = right wing down, interpolated to the frame. */
    public float getBank(float partialTick) {
        return Mth.lerp(partialTick, this.bankO, this.bank);
    }

    /** Landing flare, 0..1, interpolated to the frame. */
    public float getFlare(float partialTick) {
        return Mth.lerp(partialTick, this.flareO, this.flare);
    }

    public boolean isFetchModeEnabled() {
        return this.entityData.get(FETCH_MODE_ENABLED);
    }

    public void setFetchModeEnabled(boolean enabled) {
        this.entityData.set(FETCH_MODE_ENABLED, enabled);
    }

    public void setHuntTarget(@Nullable LivingEntity target) {
        this.huntTarget = target;
        this.setEagleState(target != null ? STATE_HUNTING : STATE_IDLE);
    }

    @Nullable
    public LivingEntity getHuntTarget() {
        return huntTarget;
    }

    // ---------------------------------------------------------------
    // Fetch inventory API: used by EagleFetchGoal.
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

    // Drop the carried inventory at the eagle's feet: used on death.
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
        // Eagles don't spawn a baby directly when breeding: instead the
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
        if (this.getOwnerUUID() == null) return false;
        if (!this.getOwnerUUID().equals(other.getOwnerUUID())) return false;
        return this.isInLove() && other.isInLove();
    }

    // Babies are smaller: keeps eaglets visually distinct from adults.
    @Override
    public float getScale() {
        return this.isBaby() ? 0.55f : 1.0f;
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
    protected float getSoundVolume() {
        return 0.8f;
    }

    // ---------------------------------------------------------------
    // Inner Goal classes (Steps 5 & 6 from roadmap)
    // ---------------------------------------------------------------

    /**
     * EagleFetchGoal: scans for arrows on the ground near the owner, flies
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
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canUse() {
            if (!eagle.isTame() || eagle.isOrderedToSit()) return false;
            if (!(eagle.getOwner() instanceof Player owner) || !owner.isAlive()) return false;
            // Owner in a different dimension: we can't path there, don't even try.
            if (owner.level().dimension() != eagle.level().dimension()) return false;

            int state = eagle.getEagleState();

            // Cold-start return is always allowed, even with fetch mode OFF,
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
                // Return always continues: deliver what we have even with mode off.
                return !eagle.isFetchInventoryEmpty();
            }
            return false;
        }

        @Override
        public void tick() {
            giveUpTimer++;
            // 20-second budget per phase. The timer resets when we transition
            // FETCH→RETURN or chain to a new arrow, so a long full cycle is fine.
            if (giveUpTimer > 400) {
                stop();
                return;
            }

            int state = eagle.getEagleState();
            if (state == STATE_FETCHING) {
                tickFetch();
            } else if (state == STATE_RETURNING) {
                tickReturn();
            } else {
                stop();
            }
        }

        // FETCH phase: fly out to the arrow, grab it, then either chain to
        // the next nearby arrow or transition to RETURNING.
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
            if (!eagle.level().isLoaded(targetArrow.blockPosition())) {
                stop();
                return;
            }

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
                    // Shouldn't normally happen: canUse checked hasFetchSpace,
                    // but drop anything that didn't fit so it's not lost.
                    eagle.spawnAtLocation(leftover);
                }

                targetArrow.discard();
                targetArrow = null;

                // Decide whether to chain to another arrow or head home.
                Player owner = (Player) eagle.getOwner();
                net.minecraft.world.entity.projectile.AbstractArrow next =
                        (eagle.isFetchModeEnabled() && eagle.hasFetchSpace() && owner != null)
                                ? findNearestArrow(eagle, owner)
                                : null;

                if (next != null) {
                    targetArrow = next;
                    eagle.getNavigation().stop();
                    repathCooldown = 0;
                    giveUpTimer = 0;
                } else {
                    transitionToReturn();
                }
            }
        }

        private void transitionToReturn() {
            eagle.setEagleState(STATE_RETURNING);
            eagle.getNavigation().stop();
            repathCooldown = 0;
            giveUpTimer = 0;
        }

        // RETURN phase: carry everything back to the owner and deposit it.
        private void tickReturn() {
            if (eagle.isFetchInventoryEmpty()) {
                stop();
                return;
            }
            if (!(eagle.getOwner() instanceof Player owner) || !owner.isAlive()) {
                stop();
                return;
            }
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
            // Keep any carried items in the inventory: next canUse cycle will
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
     * EaglePerchGoal: when the owner is far away (or offline / in another
     * dimension), the eagle flies to its claimed perch block and settles there
     * instead of hovering aimlessly. On arrival it enters STATE_PERCHED and
     * sits, reusing the existing sit animation.
     */
    static class EaglePerchGoal extends Goal {
        // Owner must be at least this far (or absent) for the eagle to leave
        // their side and go home. Well outside the follow goal's catch-up range
        // so the two don't fight.
        private static final double OWNER_FAR_THRESHOLD_SQR = 32.0 * 32.0;
        // Y of the top of the crossbar in the perch model (14 / 16 of a block
        // above the perch's base position). Eagle's feet should land here.
        private static final double CROSSBAR_Y = 14.0 / 16.0;

        private final EagleEntity eagle;
        private final EagleLanding landing = new EagleLanding();
        // Set once it's down; canContinueToUse uses this to release the goal cleanly.
        private boolean landed = false;

        EaglePerchGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!eagle.isTame() || eagle.isOrderedToSit() || eagle.isRecalled()) return false;
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
            if (!eagle.isTame() || eagle.isOrderedToSit() || eagle.isRecalled()) return false;
            BlockPos perch = eagle.getPerchPos();
            if (perch == null) return false;
            if (!eagle.level().isLoaded(perch)) return false;
            // Owner came back: yield to the follow goal mid-flight.
            return ownerFarOrAbsent();
        }

        @Override
        public void start() {
            landed = false;
            landing.reset();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos perch = eagle.getPerchPos();
            if (perch == null) return;
            Vec3 spot = new Vec3(perch.getX() + 0.5, perch.getY() + CROSSBAR_Y, perch.getZ() + 0.5);
            // In on a glide and down onto the crossbar, then settle in.
            if (landing.tick(eagle, spot, 1.2)) {
                eagle.setEagleState(STATE_PERCHED);
                eagle.setOrderedToSit(true);
                landed = true;
            }
        }

        @Override
        public void stop() {
            eagle.getNavigation().stop();
        }

        // Owner is "far" if absent (offline / null) or in another dimension
        // or beyond OWNER_FAR_THRESHOLD blocks. Inside that threshold, the
        // follow goal handles them.
        private boolean ownerFarOrAbsent() {
            var owner = eagle.getOwner();
            if (owner == null) return true;
            if (!owner.isAlive()) return true;
            if (owner.level().dimension() != eagle.level().dimension()) return true;
            return eagle.distanceToSqr(owner) > OWNER_FAR_THRESHOLD_SQR;
        }
    }

    /**
     * EagleBreedGoal: replaces vanilla BreedGoal for eagles. When two of the
     * same owner's adult eagles enter love mode and a claimed nest is in
     * range, both fly to the nest, perform a short ritual, and add an egg.
     * The egg incubates inside the nest and hatches into a baby eagle.
     * <p>
     * The "leader" eagle (the one with the lower UUID) is responsible for
     * actually depositing the egg, so we don't double-lay when both partners
     * arrive on the same tick.
     */
    static class EagleBreedGoal extends Goal {
        private static final int RITUAL_TICKS_REQUIRED = 40;          // 2 seconds at the nest
        private static final double NEST_SEARCH_RADIUS = 24.0;        // blocks
        private static final double PARTNER_SEARCH_RADIUS = 24.0;     // blocks
        private static final double NEST_ARRIVAL_DIST_SQR = 25.0;     // 5 blocks: generous so both birds count
        private static final int AGE_COOLDOWN_AFTER_BREED = 6000;     // 5 minutes

        // Cooldown applied to both birds if the ritual ends without an egg.
        // Stops the player from spam-feeding the same pair every 30 seconds
        // when the ritual can't complete (e.g., navigation problems).
        private static final int FAILED_ATTEMPT_COOLDOWN_TICKS = 1200;  // 1 minute

        private final EagleEntity eagle;
        @Nullable
        private EagleEntity partner;
        @Nullable
        private BlockPos nestPos;
        private int ritualTicks = 0;
        private final EagleLanding landing = new EagleLanding();
        private boolean eggLaid = false;

        EagleBreedGoal(EagleEntity eagle) {
            this.eagle = eagle;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
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
            landing.reset();
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
            double offsetX = rightSide ? 0.3 : -0.3;

            double centerX = nestPos.getX() + 0.5;
            double landY = nestPos.getY() + 4.0 / 16.0;  // top of the nest rim
            double centerZ = nestPos.getZ() + 0.5;

            // Both birds land side by side on the nest.
            landing.tick(eagle, new Vec3(centerX + offsetX, landY, centerZ), 1.0);

            boolean selfAtNest = eagle.distanceToSqr(centerX, landY, centerZ) < NEST_ARRIVAL_DIST_SQR;
            boolean partnerAtNest = partner.distanceToSqr(centerX, landY, centerZ) < NEST_ARRIVAL_DIST_SQR;
            if (selfAtNest && partnerAtNest) {
                ritualTicks++;
                if (ritualTicks >= RITUAL_TICKS_REQUIRED) {
                    layEgg();
                }
            }
        }

        // Either partner can deposit. The early isInLove check prevents
        // a double-lay because layEgg also resets both birds' love mode,
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

            if (eagle.getOwner() instanceof Player owner) {
                owner.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.fletcherstrestle.eagle_laid_egg"),
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
                                    && e.getOwnerUUID() != null
                                    && e.getOwnerUUID().equals(eagle.getOwnerUUID()))
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
            if (eagle.getOwnerUUID() == null) return false;
            if (!eagle.getOwnerUUID().equals(nest.getOwnerUUID())) return false;
            return nest.hasEggSpace();
        }
    }
}
