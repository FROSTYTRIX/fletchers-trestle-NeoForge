package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.effect.ModEffects;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularArrowItem;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    private Entity resonanceTarget = null;
    private float resonanceDamage = 0f;

    //Jungle Shaft Bounces
    private int bounceCount = 0;
    private static final int MAX_BOUNCES = 3;

    //Grappling
    private int hookTicks = 0;
    private static final int MAX_HOOK_DURATION = 100; // 5 seconds

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

    public ItemStack getSyncedItemStack() {
        return this.entityData.get(SYNCED_ITEM);
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
            // Read the enums
            ModularArrowItem.ShaftStats shaft = ModularArrowItem.ShaftStats.fromString(assembly.shaft());
            ModularArrowItem.HeadStats head = ModularArrowItem.HeadStats.fromString(assembly.head());

            // Apply base damage multiplier from the head
            this.setBaseDamage(this.getBaseDamage() * head.getDamageMult());

            // Note: Adjusting actual gravity and velocity happens differently in 1.21,
            // but we can scale the motion here for the initial shot speed.
            this.setDeltaMovement(this.getDeltaMovement().scale(shaft.getVelocityMult()));
        }
    }

    @Override
    protected double getDefaultGravity() {
        // Fetch the item data while the arrow is in flight
        ItemStack ammo = this.getPickupItem();
        ArrowAssembly assembly = ammo.get(ModDataComponents.ARROW_ASSEMBLY.get());

        if (assembly != null) {
            // Read your Shaft enum
            ModularArrowItem.ShaftStats shaft = ModularArrowItem.ShaftStats.fromString(assembly.shaft());

            // Multiply vanilla gravity by your config (e.g., Birch = 0.9, Spruce = 1.1)
            return super.getDefaultGravity() * shaft.getGravityMult();
        }

        // Fallback to normal vanilla gravity if no data is found
        return super.getDefaultGravity();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        ArrowAssembly assembly = this.getAssembly();

        // WEIGHTED BLUNT: Calculate distance traveled and increase base damage before the hit resolves
        if ("weighted_blunt".equals(assembly.head()) && this.startPos != null) {
            double distance = this.position().distanceTo(this.startPos);
            double bonusMultiplier = 1.0 + ((distance / 10.0) * 0.05);
            this.setBaseDamage(this.getBaseDamage() * bonusMultiplier);
        }



        // Resolve the hit
        super.onHitEntity(result);

        ModularArrowItem.HeadStats head = ModularArrowItem.HeadStats.fromString(assembly.head());

        if (result.getEntity() instanceof LivingEntity target) {
            // BROADHEAD: Bleeding (Poison) for 3s (60 ticks)
            if ("broadhead".equals(assembly.head())) {
                target.addEffect(new MobEffectInstance(ModEffects.BLEED_EFFECT, 60, 0));
            }

            if (head.isArmorPiercing()) {
                this.setBaseDamage(this.getBaseDamage() * 1.25);
            }

            // RESONANCE TIP: Setup the delayed echo hit
            if ("resonance_tip".equals(assembly.head())) {
                this.resonanceTicks = 30; // 0.5 seconds
                this.resonanceTarget = target;
                this.resonanceDamage = (float) this.getBaseDamage() * 0.3f;
            }

            // BOUND (Fletching): 25% chance to drop the arrow on impact instead of breaking
            if ("bound".equals(assembly.fletching()) && this.random.nextFloat() < 0.25f) {
                this.spawnAtLocation(this.getPickupItem());
                this.discard(); // Remove the entity so it doesn't get stuck in the target
            }

            // CRIMSON: Executioner (+50% damage if target is below half health)
            if ("crimson".equals(assembly.shaft())) {
                if (target.getHealth() < target.getMaxHealth() * 0.5f) {
                    this.setBaseDamage(this.getBaseDamage() * 1.5);
                }
            }

            // PALE OAK: Vengeful (Backstab - +40% damage if hitting from behind)
            if ("pale_oak".equals(assembly.shaft())) {
                Vec3 targetView = target.getViewVector(1.0F);
                Vec3 arrowDir = this.getDeltaMovement().normalize();
                // If dot product is > 0.5, they are looking in the same direction = Backstab
                if (targetView.dot(arrowDir) > 0.5) {
                    this.setBaseDamage(this.getBaseDamage() * 1.4);
                    // Play a ghostly sound
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, this.getSoundSource(), 1.0f, 1.5f);
                }
            }

            // MANGROVE: Slowness III for 1 second
            if ("mangrove".equals(assembly.shaft())) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            }

            // CHERRY: Petal Burst (Heal shooter 1 heart)
            if ("cherry".equals(assembly.shaft())) {
                if (this.getOwner() instanceof LivingEntity shooter) {
                    shooter.heal(2.0f); // 2.0f = 1 Heart
                    // Pink particles
                    for(int i = 0; i < 5; ++i) {
                        this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
                                target.getRandomX(0.5D), target.getRandomY(), target.getRandomZ(0.5D), 0, 0, 0);
                    }
                }
            }

            // WARPED: Translocation (50% chance to swap positions)
            if ("warped".equals(assembly.shaft()) && this.random.nextFloat() < 1f) {
                Entity shooter = this.getOwner();
                if (shooter != null && shooter.isAlive()) {
                    Vec3 sPos = shooter.position();
                    Vec3 tPos = target.position();

                    // Swap them
                    shooter.teleportTo(tPos.x, tPos.y, tPos.z);
                    target.teleportTo(sPos.x, sPos.y, sPos.z);

                    this.playSound(net.minecraft.sounds.SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
                }
            }

            if (this.getPersistentData().getBoolean("fletcherstrestle:conductive") && this.level().isThundering()) {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
                if (lightning != null) {
                    lightning.moveTo(target.position());
                    this.level().addFreshEntity(lightning);
                }
            }

            if (assembly != null && "bodkin_point".equals(assembly.head())) {
                float armorValue = target.getArmorValue();
                if (armorValue > 0) {
                    // Calculate bonus damage manually since we are inside the impact call
                    double baseDamage = this.getBaseDamage() * this.getDeltaMovement().length();
                    float bonus = (armorValue * 0.25f) * ((float)baseDamage * 0.04f);

                    // Temporarily increase damage for this specific hit
                    this.setBaseDamage(this.getBaseDamage() + (bonus / this.getDeltaMovement().length()));
                }
            }
        }
    }


    @Override
    public void tick() {
        super.tick();
        ArrowAssembly assembly = this.getAssembly();

        // Flight modifiers only apply when flying
        if (!this.inGround) {
            // ACACIA: Speed boost mid-flight (at 10 ticks)
            if ("acacia".equals(assembly.shaft()) && this.tickCount == 10) {
                this.setDeltaMovement(this.getDeltaMovement().scale(1.4));
                this.hasImpulse = true; // Tell the server to sync the sudden movement
            }

            // SPRUCE: Heavy/Stable (Adds extra gravity)
            if ("spruce".equals(assembly.shaft())) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.01, 0));
            }

            // SERRATED: Magnetism (Subtle homing)
            if ("serrated".equals(assembly.fletching()) && this.tickCount > 2) {
                AABB searchBox = this.getBoundingBox().inflate(5.0D);
                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != this.getOwner() && e.isAlive());

                if (!entities.isEmpty()) {
                    LivingEntity target = entities.get(0); // Get closest target
                    Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
                    // Create a small pull vector towards the target
                    Vec3 pull = targetCenter.subtract(this.position()).normalize().scale(1);
                    this.setDeltaMovement(this.getDeltaMovement().add(pull));
                }
            }
        }

        // RESONANCE TIP: Trigger delayed damage
        if (this.resonanceTicks > 0) {
            this.resonanceTicks--;
            if (this.resonanceTicks == 0 && this.resonanceTarget != null && this.resonanceTarget.isAlive()) {
                // Apply 0.3x damage. In 1.21, use level().damageSources()
                this.resonanceTarget.hurt(this.damageSources().arrow(this, this.getOwner()), this.resonanceDamage);
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

    private void breakHook() {
        this.entityData.set(IS_HOOKED, false);
        this.discard(); // Arrow breaks after use
    }


    @Override
    protected void onHitBlock(BlockHitResult result) {
        ArrowAssembly assembly = this.getAssembly();

        if ("weighted_hook".equals(assembly.head())) {
            this.entityData.set(IS_HOOKED, true);
            this.setSoundEvent(SoundEvents.TRIPWIRE_ATTACH); // Mechanical "clink"
            // We don't call super.onHitBlock yet if we want to keep it from "embedding" fully
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            super.onHitBlock(result);
        }

        if (assembly != null && "jungle".equals(assembly.shaft()) && this.bounceCount < MAX_BOUNCES && this.random.nextFloat() < 0.85f) { // Keep at 1.0f for testing

            // 1. SNAP TO SURFACE: Prevent the arrow from embedding in the wall and slingshotting out!
            Vec3 hitPos = result.getLocation();
            this.setPos(hitPos.x, hitPos.y, hitPos.z);

            Direction face = result.getDirection();
            Vec3 motion = this.getDeltaMovement();

            // 2. HIGH DAMPENING: Arrows lose a lot of energy when bouncing. 30% retention is much more realistic.
            double dampen = 0.3;
            double bounceX = motion.x * dampen;
            double bounceY = motion.y * dampen;
            double bounceZ = motion.z * dampen;

            // Invert the impacted axis
            if (face.getAxis() == Direction.Axis.X) bounceX = -bounceX;
            if (face.getAxis() == Direction.Axis.Y) bounceY = -bounceY;
            if (face.getAxis() == Direction.Axis.Z) bounceZ = -bounceZ;

            Vec3 newMovement = new Vec3(bounceX, bounceY, bounceZ);
            this.setDeltaMovement(newMovement);

            // 3. VISUAL FIXES: Instantly rotate the arrow and disable crit particles
            double d0 = newMovement.horizontalDistance();
            this.setYRot((float)(Math.atan2(newMovement.x, newMovement.z) * (double)(180F / (float)Math.PI)));
            this.setXRot((float)(Math.atan2(newMovement.y, d0) * (double)(180F / (float)Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            this.setCritArrow(false); // Stop the critical hit particles after it bounces

            this.hasImpulse = true;
            this.bounceCount++;

            this.playSound(net.minecraft.sounds.SoundEvents.SLIME_BLOCK_FALL, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));

            return;
        }

        super.onHitBlock(result);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.MODULAR_ARROW.get());
    }

    @Override
    protected ItemStack getPickupItem() {
        return getSyncedItemStack();
    }
}