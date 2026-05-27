package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.custom.RopeBlock;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.effect.ModEffects;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularArrowItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModularArrowEntity extends AbstractArrow {
    private Vec3 startPos = null;

    // Echo Shard Head
    private int resonanceTicks = -1;
    private int resonanceTargetId = -1;
    private float resonanceDamage = 0f;

    //Jungle Shaft Bounces
    private int bounceCount = 0;
    private static final int MAX_BOUNCES = 3;

    // Vex Fletching — pass through one block of cover, once per arrow.
    private boolean hasPhased = false;

    //Grappling
    private int hookTicks = 0;
    private static final int MAX_HOOK_DURATION = 100; // 5 seconds

    // Rope Deployment
    private boolean isDeployingRope = false;
    private int ropesPlaced = 0;
    private BlockPos ropeAnchorPos = null;
    private static final int MAX_ROPE_LENGTH = 20;

    private static final EntityDataAccessor<Boolean> IS_HOOKED =
            SynchedEntityData.defineId(ModularArrowEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<ItemStack> SYNCED_ITEM =
            SynchedEntityData.defineId(ModularArrowEntity.class, EntityDataSerializers.ITEM_STACK);

    public ModularArrowEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    // This constructor is called by our Item when fired
    public ModularArrowEntity(Level level, LivingEntity shooter, ItemStack ammo, @Nullable ItemStack weapon) {
        super(ModEntities.MODULAR_ARROW.get(), shooter, level, ammo, weapon); // Replace EntityType.ARROW with your custom registered entity type later!
        this.entityData.set(SYNCED_ITEM, ammo.copyWithCount(1));
        applyFlightModifiers(ammo);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // Register it with a default empty arrow
        builder.define(SYNCED_ITEM, new ItemStack(ModItems.MODULAR_ARROW.get()));
        builder.define(IS_HOOKED, false);
    }

    public boolean isHooked() {
        return this.entityData.get(IS_HOOKED);
    }

    /** Position at which this arrow was spawned. {@code null} until set
     *  by the shooting code on the first tick. Used by distance-scaled
     *  effects (e.g. {@link net.frostytrix.fletcherstrestle.material.effect.DamageMultiplierByDistanceEffect}). */
    public @Nullable Vec3 getStartPos() {
        return this.startPos;
    }

    /** Public accessor for the protected vanilla
     *  {@link net.minecraft.world.entity.projectile.AbstractArrow#getPickupItem()}.
     *  Used by effects that need to drop the arrow stack on hit
     *  (e.g. {@link net.frostytrix.fletcherstrestle.material.effect.DropSelfOnHitEffect}). */
    public ItemStack getPickupItemPublic() {
        return this.getPickupItem();
    }

    /** True once the arrow has stuck into a block / become a pickup-able
     *  entity. Mirrors the protected {@code inGround} field on AbstractArrow.
     *  Effects use this to gate tick-time work to in-flight only. */
    public boolean isInGroundPublic() {
        return this.inGround;
    }

    /** Number of times this arrow has bounced off a block.
     *  Read by {@link net.frostytrix.fletcherstrestle.material.effect.BounceOnBlockEffect}. */
    public int getBounceCount() {
        return bounceCount;
    }

    /** Increment the bounce counter. Called from the bounce effect after a successful bounce. */
    public void incrementBounceCount() {
        this.bounceCount++;
    }

    public ArrowAssembly getAssembly() {
        ItemStack pickupItem = this.getPickupItem();

        if (!pickupItem.isEmpty() && pickupItem.has(ModDataComponents.ARROW_ASSEMBLY.get())) {
            return pickupItem.get(ModDataComponents.ARROW_ASSEMBLY.get());
        }

        return new ArrowAssembly("flint", "oak", "feather");
    }

    private void applyFlightModifiers(ItemStack ammo) {
        ArrowAssembly assembly = ammo.get(ModDataComponents.ARROW_ASSEMBLY.get());
        if (assembly != null) {
            var shaft     = net.frostytrix.fletcherstrestle.material.Materials.arrowShaft(assembly.shaft());
            var head      = net.frostytrix.fletcherstrestle.material.Materials.arrowHead(assembly.head());
            var fletching = net.frostytrix.fletcherstrestle.material.Materials.arrowFletching(assembly.fletching());

            // Stats: base damage scaled by head, initial velocity scaled by shaft.
            this.setBaseDamage(this.getBaseDamage() * head.stats().damageMultiplier());
            this.setDeltaMovement(this.getDeltaMovement().scale(shaft.stats().velocityMultiplier()));

            // Spawn-time effects: PierceLevelEffect (dark_oak),
            // DamageMultiplierEffect (any), and anything else a modpack
            // attaches with onArrowSpawn semantics.
            head.effects().forEach(e -> e.onArrowSpawn(this));
            shaft.effects().forEach(e -> e.onArrowSpawn(this));
            fletching.effects().forEach(e -> e.onArrowSpawn(this));
        }
    }

    @Override
    protected double getDefaultGravity() {
        // FIX: Don't fetch the item stack directly. Rely on our safe getAssembly() method!
        ArrowAssembly assembly = this.getAssembly();

        if (assembly != null) {
            var shaft = net.frostytrix.fletcherstrestle.material.Materials.arrowShaft(assembly.shaft());
            return super.getDefaultGravity() * shaft.stats().gravityMultiplier();
        }

        return super.getDefaultGravity();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        ArrowAssembly assembly = this.getAssembly();

        // Glass-vial arrows shatter on impact and splash whatever potion they
        // were dipped in. Resolves before everything else so it can't be
        // combined with other head effects. Still hardcoded — splashing
        // a potion needs the arrow's POTION_CONTENTS component and the
        // current stateful coordination; lifts to a "splash_potion" effect
        // in a later sub-phase.
        if ("glass_vial".equals(assembly.head())) {
            applyGlassVialEffect(result.getLocation());
            return;
        }

        // Resonance setup — head-specific multi-tick state that freezes the
        // arrow and queues delayed damage. Stays here because it needs to
        // mutate arrow state in coordinated ways the simple effect hook
        // doesn't expose. Future "stateful effect" extension can lift it.
        double velocityOnImpact = this.getDeltaMovement().length();
        if ("resonance_tip".equals(assembly.head())
                && result.getEntity() instanceof LivingEntity resonanceTarget) {
            this.resonanceTicks = 20;
            this.resonanceTargetId = resonanceTarget.getId();
            this.resonanceDamage = (float) (this.getBaseDamage() * velocityOnImpact) * 0.3f;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, this.getSoundSource(), 1.0f, 2.0f);
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
        }

        // --- PRE-HIT effects: damage modifiers that need to influence
        // the damage value vanilla is about to apply.
        var head      = net.frostytrix.fletcherstrestle.material.Materials.arrowHead(assembly.head());
        var shaft     = net.frostytrix.fletcherstrestle.material.Materials.arrowShaft(assembly.shaft());
        var fletching = net.frostytrix.fletcherstrestle.material.Materials.arrowFletching(assembly.fletching());
        head.effects().forEach(e -> e.onPreArrowHit(this, result));
        shaft.effects().forEach(e -> e.onPreArrowHit(this, result));
        fletching.effects().forEach(e -> e.onPreArrowHit(this, result));

        // Vanilla hit resolution.
        super.onHitEntity(result);

        // --- POST-HIT effects: status effects, target pull, shooter heal,
        // teleport swap, drop-on-hit, etc.
        head.effects().forEach(e -> e.onArrowHit(this, result));
        shaft.effects().forEach(e -> e.onArrowHit(this, result));
        fletching.effects().forEach(e -> e.onArrowHit(this, result));

        // Riser-driven Copper "conductive" lightning — still keyed off the
        // persistent flag set by ModularBowItem.createProjectile. Phase E
        // (a later sub-phase that adds bow-release effects) will move
        // this into a riser-attached effect.
        if (result.getEntity() instanceof LivingEntity target
                && this.getPersistentData().getBoolean("fletcherstrestle:conductive")
                && this.level().isThundering()) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
            if (lightning != null) {
                lightning.moveTo(target.position());
                this.level().addFreshEntity(lightning);
            }
        }
    }


    @Override
    public void tick() {
        super.tick();
        ArrowAssembly assembly = this.getAssembly();
        var shaft = net.frostytrix.fletcherstrestle.material.Materials.arrowShaft(assembly.shaft());

        if (this.isDeployingRope && !this.level().isClientSide) {

            // Unspool 1 block every 2 game ticks (adjust this for faster/slower drops)
            if (this.tickCount % 2 == 0) {

                // Safety check in case the block breaks while deploying
                if (this.ropeAnchorPos == null) {
                    this.isDeployingRope = false;
                    return;
                }

                // Find the coordinate for the next block down
                BlockPos nextPos = this.ropeAnchorPos.below(this.ropesPlaced + 1);
                BlockState currentState = this.level().getBlockState(nextPos);

                // If the space is empty (or tall grass/water) AND we haven't hit the limit
                if (currentState.canBeReplaced() && this.ropesPlaced < MAX_ROPE_LENGTH) {

                    // We ALWAYS spawn the new block as the "Bottom" piece.
                    // Because of the 'updateShape' method you wrote in RopeBlock earlier,
                    // the block above this one will automatically realize it's no longer
                    // the bottom and visually update its model instantly!
                    BlockState ropeState = ModBlocks.ROPE.get().defaultBlockState()
                            .setValue(RopeBlock.PERSISTENT, false)
                            .setValue(RopeBlock.BOTTOM, true);

                    // The '3' flag forces a block update to trigger the visual connections
                    this.level().setBlock(nextPos, ropeState, 3);

                    // Play a soft click for every block placed
                    this.playSound(SoundEvents.WOOL_PLACE, 0.7F, 1.2F);

                    this.ropesPlaced++;
                } else {
                    // We hit a solid floor or max length, stop deploying
                    this.isDeployingRope = false;
                }
            }
        }

        // Flight modifiers only apply when flying
        if (!this.inGround) {
            // Apply gravity Mult
            this.setDeltaMovement(this.getDeltaMovement().add(0, -(shaft.stats().gravityMultiplier()-1)/10, 0));

            // Tick-time effects: acacia speed boost, serrated homing, and
            // anything else a modpack attaches.
            var head      = net.frostytrix.fletcherstrestle.material.Materials.arrowHead(assembly.head());
            var fletching = net.frostytrix.fletcherstrestle.material.Materials.arrowFletching(assembly.fletching());
            head.effects().forEach(e -> e.onArrowTick(this));
            shaft.effects().forEach(e -> e.onArrowTick(this));
            fletching.effects().forEach(e -> e.onArrowTick(this));
        }

        // RESONANCE TIP: Trigger delayed damage
        if (this.resonanceTicks > 0) {
            this.resonanceTicks--;

            Entity target = this.level().getEntity(this.resonanceTargetId);

            // 1. STICK TO TARGET: If the target is alive, follow it!
            if (target != null && target.isAlive()) {
                // Snap the arrow to the center of the target's body
                this.setPos(target.getX(), target.getY() + (target.getBbHeight() / 2.0), target.getZ());
            } else if (!this.level().isClientSide) {
                // Failsafe: If the target dies or vanishes before detonation, delete the arrow
                this.discard();
                return;
            }

            // 2. DETONATION
            if (this.resonanceTicks == 0) {
                if (!this.level().isClientSide && target instanceof LivingEntity livingTarget) {

                    // Strip the target's i-frames
                    livingTarget.invulnerableTime = 0;

                    // Apply the delayed damage
                    livingTarget.hurt(this.damageSources().arrow(this, this.getOwner()), this.resonanceDamage);
                    if (this.level() instanceof ServerLevel serverLevel && this.resonanceTicks % 2 == 0) {
                        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                                this.getX(), this.getY(), this.getZ(),
                                1, 0, 0, 0, 0);
                    }
                    // Final Visual: The big blast
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.WARDEN_SONIC_BOOM, this.getSoundSource(), 1.0f, 1.0f);
                }

                if (!this.level().isClientSide) {
                    this.discard(); // Finally allow the arrow to vanish
                }
            }
        }

        if (!this.level().isClientSide && this.isInWater()) {
            if (this.getPersistentData().getBoolean("fletcherstrestle:amphibious")) {
                this.setDeltaMovement(this.getDeltaMovement().scale(1.65D));

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                            this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }

        if (this.isHooked() && !this.level().isClientSide) {
            Entity owner = this.getOwner();

            if (owner instanceof Player player && player.isAlive()) {
                // 1. Calculate the distance vector
                Vec3 arrowPos = this.position();
                Vec3 playerPos = player.position();
                Vec3 pullVec = arrowPos.subtract(playerPos);

                double distance = pullVec.length();

                // 2. Apply the pull if the player isn't right on top of the hook
                if (distance > 1.5 && distance < 32) {
                    double pullStrength = 0.15; // Tweak this for speed

                    // Normalize the direction and multiply by strength
                    Vec3 velocity = pullVec.normalize().scale(pullStrength);

                    // Add vertical lift to prevent dragging on the floor
                    double lift = 0.05;
                    player.addDeltaMovement(new Vec3(velocity.x, velocity.y + lift, velocity.z));
                    player.hurtMarked = true; // Forces the client to sync the new velocity

                    this.hookTicks++;
                } else {
                    // Break the hook if we arrive or get too far
                    this.breakHook();
                }

                // 3. Time-out safety
                if (this.hookTicks > MAX_HOOK_DURATION) {
                    this.breakHook();
                }
            } else {
                this.breakHook();
            }
        }
    }


    @Override
    public void remove(RemovalReason reason) {
        ArrowAssembly assembly = this.getAssembly();
        // If we are currently "resonating," we refuse to be removed
        // unless the world is closing or the entity is being discarded by a command.
        if (this.resonanceTicks > 0 && reason == RemovalReason.DISCARDED) {
            return;
        }
        super.remove(reason);
    }

    private void breakHook() {
        this.entityData.set(IS_HOOKED, false);
        this.discard(); // Arrow breaks after use
    }


    @Override
    protected void onHitBlock(BlockHitResult result) {
        ArrowAssembly assembly = this.getAssembly();

        // VEX FLETCHING: phase through one block of cover. Skips the impact
        // entirely the first time we hit a block; subsequent hits behave normally.
        // Bypasses every other head/shaft interaction with the surface.
        if (!this.hasPhased && "vex".equals(assembly.fletching())) {
            this.hasPhased = true;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY(), this.getZ(),
                        6, 0.1, 0.1, 0.1, 0.04);
            }
            this.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 0.4f, 1.6f);
            // Step the arrow forward past the block so it doesn't immediately
            // re-collide on the next tick. ~0.6 of a block in the direction
            // of motion is enough for any 1-block obstacle.
            Vec3 dir = this.getDeltaMovement().normalize();
            this.setPos(this.getX() + dir.x * 0.6,
                        this.getY() + dir.y * 0.6,
                        this.getZ() + dir.z * 0.6);
            return;
        }

        if ("glass_vial".equals(assembly.head())) {
            applyGlassVialEffect(result.getLocation());
            return;
        }

        if ("weighted_hook".equals(assembly.head())) {
            this.entityData.set(IS_HOOKED, true);
            this.setSoundEvent(SoundEvents.TRIPWIRE_ATTACH); // Mechanical "clink"
            // We don't call super.onHitBlock yet if we want to keep it from "embedding" fully
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            super.onHitBlock(result);
        }

        if ("trailing_rope".equals(assembly.head())) {

            // We ONLY deploy if the arrow hits the underside of a block (the ceiling)
            if (result.getDirection() == Direction.DOWN && !this.level().isClientSide) {
                // Start the deployment sequence!
                this.isDeployingRope = true;
                this.ropesPlaced = 0;
                this.ropeAnchorPos = result.getBlockPos();
            }

            // Let the arrow physically embed into the ceiling to act as the anchor
            this.pickup = AbstractArrow.Pickup.DISALLOWED;
            super.onHitBlock(result);
            return;
        }

        // Dispatch block-hit effects (jungle bounce, future: phase-through,
        // sticky, etc.). Effects that mutate the arrow into a deflected
        // state signal it by bumping bounceCount — if that happened,
        // skip the vanilla embed path.
        int bounceBefore = this.bounceCount;
        var head      = net.frostytrix.fletcherstrestle.material.Materials.arrowHead(assembly.head());
        var shaft     = net.frostytrix.fletcherstrestle.material.Materials.arrowShaft(assembly.shaft());
        var fletching = net.frostytrix.fletcherstrestle.material.Materials.arrowFletching(assembly.fletching());
        head.effects().forEach(e -> e.onArrowHitBlock(this, result));
        shaft.effects().forEach(e -> e.onArrowHitBlock(this, result));
        fletching.effects().forEach(e -> e.onArrowHitBlock(this, result));
        if (this.bounceCount > bounceBefore) {
            return; // a bounce effect consumed the impact
        }

        super.onHitBlock(result);
    }

    // Splash potion behavior for glass-vial arrows. Mirrors vanilla
    // ThrownPotion's distance-based dilution: full effect at impact point,
    // weaker the further you are, no effect past the radius. Glass breaks
    // regardless of whether the arrow held a potion.
    private void applyGlassVialEffect(Vec3 hitPos) {
        Level lvl = this.level();

        // Glass shatter — sound + neutral water splash particles.
        lvl.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                SoundEvents.GLASS_BREAK, this.getSoundSource(), 1.0f, 1.0f);

        if (lvl instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH,
                    hitPos.x, hitPos.y, hitPos.z,
                    8, 0.3, 0.3, 0.3, 0.1);

            // Pull POTION_CONTENTS off the synced pickup stack (the dipping
            // recipe writes it there when dipping a glass-vial arrow).
            PotionContents potion = getPickupItem().get(DataComponents.POTION_CONTENTS);
            if (potion != null) {
                final double radius = 4.0;
                AABB area = new AABB(
                        hitPos.x - radius, hitPos.y - radius, hitPos.z - radius,
                        hitPos.x + radius, hitPos.y + radius, hitPos.z + radius);
                final java.util.List<LivingEntity> entities =
                        lvl.getEntitiesOfClass(LivingEntity.class, area);

                for (LivingEntity entity : entities) {
                    double d2 = entity.distanceToSqr(hitPos.x, hitPos.y, hitPos.z);
                    if (d2 >= radius * radius) continue;
                    final double intensity = 1.0 - Math.sqrt(d2) / radius;

                    potion.forEachEffect(eff -> {
                        int dur = Math.max(20, (int) (eff.getDuration() * intensity + 0.5));
                        entity.addEffect(new MobEffectInstance(
                                eff.getEffect(),
                                eff.getEffect().value().isInstantenous() ? eff.getDuration() : dur,
                                eff.getAmplifier(),
                                eff.isAmbient(),
                                eff.isVisible()), this.getOwner());
                    });
                }

                // Coloured cloud at the impact site so the splash is visible.
                int color = potion.getColor();
                float r = ((color >> 16) & 0xFF) / 255f;
                float g = ((color >>  8) & 0xFF) / 255f;
                float b = ( color        & 0xFF) / 255f;
                sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(r, g, b), 1.0f),
                        hitPos.x, hitPos.y, hitPos.z,
                        30, 1.0, 1.0, 1.0, 0.1);
            }
        }

        this.discard();
    }

    // Spruce limb's "built-in Punch I": add an extra knockback impulse on
    // top of whatever vanilla applies. Uses the same formula vanilla uses
    // for arrow knockback (horizontal-velocity-based + KB resistance),
    // scaled to roughly match enchantment Punch I.
    @Override
    protected void doKnockback(LivingEntity entity, DamageSource damageSource) {
        super.doKnockback(entity, damageSource);

        if (!this.getPersistentData().getBoolean("fletcherstrestle:punch")) return;

        double resistance = Math.max(0.0,
                1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 push = this.getDeltaMovement()
                .multiply(1.0, 0.0, 1.0)
                .normalize()
                .scale(0.6 * 0.6 * resistance);  // strength 0.6 * vanilla's 0.6 scale
        if (push.lengthSqr() > 0) {
            entity.push(push.x, 0.1, push.z);
            entity.hurtMarked = true;
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.MODULAR_ARROW.get());
    }

    public ItemStack getSyncedItemStack() {
        return this.entityData.get(SYNCED_ITEM);
    }

    @Override
    protected ItemStack getPickupItem() {
        return getSyncedItemStack();
    }

    @Override
    public ItemStack getPickResult() {
        return this.getPickupItem().copy();
    }
}