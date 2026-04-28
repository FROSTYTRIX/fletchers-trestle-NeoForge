package net.frostytrix.fletcherstrestle.entity;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularArrowItem;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public class ModularArrowEntity extends AbstractArrow {
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
    }

    public ItemStack getSyncedItemStack() {
        return this.entityData.get(SYNCED_ITEM);
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
        // Read our parts right before we hit the entity
        ItemStack ammo = this.getPickupItem();
        ArrowAssembly assembly = ammo.get(ModDataComponents.ARROW_ASSEMBLY.get());

        if (assembly != null && result.getEntity() instanceof LivingEntity target) {
            ModularArrowItem.HeadStats head = ModularArrowItem.HeadStats.fromString(assembly.head());

            // Let the arrow do its normal vanilla damage first
            super.onHitEntity(result);

            // Bodkin Armor Piercing: Deal bonus unmitigated damage based on the target's armor
            if (head.isArmorPiercing() && !target.isDeadOrDying()) {
                float armor = target.getArmorValue();
                if (armor > 0) {
                    // Magic damage naturally ignores armor in Minecraft
                    target.hurt(this.damageSources().magic(), armor * 0.4f);
                }
            }

            // Broadhead Bleed: Apply Poison/Wither
            if (head.causesBleed() && !target.isDeadOrDying()) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
            }
        } else {
            super.onHitEntity(result);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.MODULAR_ARROW.get());
    }
}