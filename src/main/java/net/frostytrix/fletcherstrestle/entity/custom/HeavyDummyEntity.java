package net.frostytrix.fletcherstrestle.entity.custom;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// TODO(port-26.1): Big entity-API rewrite needed.
//
// What changed in 26.1:
//   - ArmorItem class removed; equipment slot info lives on the
//     Equippable data component now (DataComponents.EQUIPPABLE).
//   - LivingEntity.getArmorSlots() / setItemSlot(EquipmentSlot, ItemStack)
//     are no longer override hooks — the equipment system uses
//     EntityEquipment / DataComponents directly.
//   - Entity.hurt(DamageSource, float) is final; override
//     hurtServer(ServerLevel, DamageSource, float) instead.
//   - Entity.interact(Player, InteractionHand) → (Player, InteractionHand, Vec3).
//   - EntityDataSerializers.OPTIONAL_UUID removed.
//   - LivingEntity.actuallyHurt signature changed.
//   - CompoundTag.getString returns Optional<String>; hasUUID may also be gone.
//
// Until rewritten, the dummy compiles as a barely-functional LivingEntity:
//   - Spawns and stands.
//   - Does NOT accept armor swapping (would need EQUIPPABLE component logic).
//   - Does NOT report hit-zone damage to the shooter.
//   - Does NOT drop itself when sneak-hit.
//   - Stays at full HP via tick() heal-back.
public class HeavyDummyEntity extends LivingEntity {

    private static final EntityDataAccessor<String> DATA_SKIN_NAME =
            SynchedEntityData.defineId(HeavyDummyEntity.class, EntityDataSerializers.STRING);

    // Kept around for future re-port; currently unused.
    @SuppressWarnings("unused")
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);

    public HeavyDummyEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity entityIn) {}
    @Override public boolean isNoGravity() { return false; }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    // 26.1: hurt() is final on Entity, override hurtServer() instead.
    // TODO(port-26.1): port sneak-hit-removal + arrow drop logic here.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Default behaviour — accept damage, heal back in tick().
        return super.hurtServer(level, source, amount);
    }

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
        // TODO(port-26.1): OPTIONAL_UUID serializer removed; use String form
        // of the UUID for now (or migrate to a custom serializer later).
    }

    public void setSkin(String name) {
        this.entityData.set(DATA_SKIN_NAME, name);
    }

    public String getSkinName() { return this.entityData.get(DATA_SKIN_NAME); }


    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("SkinName", this.getSkinName());
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        // CompoundTag.getString returns Optional<String> in 26.1
        this.setSkin(compound.getString("SkinName").orElse(""));
    }
}
