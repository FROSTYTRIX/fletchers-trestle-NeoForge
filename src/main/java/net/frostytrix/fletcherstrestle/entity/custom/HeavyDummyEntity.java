package net.frostytrix.fletcherstrestle.entity.custom;

import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class HeavyDummyEntity extends LivingEntity {
    private static final EntityDataAccessor<String> DATA_SKIN_NAME =
            SynchedEntityData.defineId(HeavyDummyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> DATA_SKIN_UUID =
            SynchedEntityData.defineId(HeavyDummyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // Internal inventory for the dummy's armor
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);

    public HeavyDummyEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D) // High health so it doesn't break easily
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D); // So it doesn't slide around when punched
    }


    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entityIn) {
    }


    @Override
    public boolean canBeCollidedWith() {
        return true; // Allows the player and arrows to hit it
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }


    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return this.armorItems;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot.isArmor()) {
            return this.armorItems.get(slot.getIndex());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        if (slot.isArmor()) {
            this.armorItems.set(slot.getIndex(), stack);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);



        // 1. Equip Armor
        if (heldItem.getItem() instanceof ArmorItem armorItem) {
            EquipmentSlot slot = armorItem.getEquipmentSlot();

            // NEW FILTER: Only allow HEAD, CHEST, LEGS, or FEET
            // This effectively rejects the 'BODY' slot used by Horse and Wolf armor.
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.BODY) {
                return super.interact(player, hand);
            }

            ItemStack currentArmor = this.getItemBySlot(slot);

            // Swap or equip logic...
            this.setItemSlot(slot, heldItem.copy());
            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
            if (!currentArmor.isEmpty() && !player.isCreative()) {
                player.getInventory().placeItemBackInInventory(currentArmor);
            }

            this.level().playSound(null, this.blockPosition(), SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 2. Unequip Armor (Shift-Right-Click with empty hand)
        if (player.isShiftKeyDown() && heldItem.isEmpty()) {
            // Find highest armor piece and pop it off
            for (int i = 3; i >= 0; i--) {
                ItemStack stack = this.armorItems.get(i);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack.copy());
                    this.armorItems.set(i, ItemStack.EMPTY);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            }
        }

        return super.interact(player, hand);
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.playSound(SoundEvents.ARMOR_STAND_HIT, 1.0f, 1.0f);
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    @Override
    public boolean shouldShowName() {
        return this.hasCustomName() && super.shouldShowName();
    }

    @Override
    public boolean isCustomNameVisible() {
        return this.hasCustomName() && super.isCustomNameVisible();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Check if the source is a player
        if (source.getEntity() instanceof Player player) {
            // If the player is sneaking and hits it with an Axe (or empty hand), remove it
            if (player.isShiftKeyDown()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

                this.getArmorSlots().forEach(stack -> {
                    if (!stack.isEmpty()) this.spawnAtLocation(stack);
                });
                if (!this.level().isClientSide() && !player.isCreative()) {
                    this.spawnAtLocation(ModItems.HEAVY_DUMMY_ITEM.get());
                }

                this.discard();
                return true;
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float damageAmount) {
        // 1. Record health before Minecraft applies the damage
        float healthBefore = this.getHealth();

        // 2. Let the game calculate armor reduction and apply the hit
        super.actuallyHurt(source, damageAmount);

        // 3. Calculate exactly how much health was lost
        float actualDamageTaken = healthBefore - this.getHealth();

        // 4. If the hit did damage and came from a projectile (like your Modular Arrow)
        if (actualDamageTaken > 0 && source.getDirectEntity() != null) {
            Entity projectile = source.getDirectEntity();

            if (projectile instanceof AbstractArrow arrow) {
                // Check if the arrow is a standard "pickable" arrow (not from Creative mode)
                if (arrow.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED) {
                    // Drop the arrow item at the dummy's location
                    this.spawnAtLocation(arrow.getPickResult());
                }
                // Remove the arrow entity so it doesn't stay stuck visually in the dummy
                arrow.discard();
            }

            // Calculate where the arrow hit relative to the dummy's feet (0.0 to 1.8)
            double hitHeight = projectile.getY() - this.getY();

            // Determine the zone based on height
            String zone;
            if (hitHeight >= 1.4) {
                zone = "Head";
            } else if (hitHeight >= 0.7) {
                zone = "Torso";
            } else {
                zone = "Legs";
            }

            if (source.getEntity() instanceof Player player) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                String.format("🎯 [%s Hit] - %.1f Damage", zone, actualDamageTaken)
                        ), true // 'true' puts it in the action bar above the hotbar
                );
            }
        }
    }

    // --- IMMORTALITY ---

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_NAME, "");
        builder.define(DATA_SKIN_UUID, Optional.empty());
    }

    public void setSkin(String name, UUID uuid) {
        this.entityData.set(DATA_SKIN_NAME, name);
        this.entityData.set(DATA_SKIN_UUID, Optional.ofNullable(uuid));
    }

    public String getSkinName() { return this.entityData.get(DATA_SKIN_NAME); }
    public Optional<UUID> getSkinUUID() { return this.entityData.get(DATA_SKIN_UUID); }

    // Save/Load so skins persist after a restart
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("SkinName", this.getSkinName());
        this.getSkinUUID().ifPresent(uuid -> compound.putUUID("SkinUUID", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setSkin(compound.getString("SkinName"), compound.hasUUID("SkinUUID") ? compound.getUUID("SkinUUID") : null);
    }
}