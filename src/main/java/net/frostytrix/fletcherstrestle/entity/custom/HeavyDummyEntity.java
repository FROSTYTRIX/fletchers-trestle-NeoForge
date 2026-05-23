package net.frostytrix.fletcherstrestle.entity.custom;

import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

// 26.1: the entity-API rewrite touched a *lot* — the dummy is
// re-implemented against the new shapes here. Outstanding bits the
// 1.21.1 version did and we still haven't ported:
//   * Hit-zone damage reporting to the shooter (would need overriding
//     hurtServer + ArrowHitInfo glue).
//   * Sneak-hit drop (rebuild via a useWithoutItem-like path on the
//     entity's interact callback).
//   * Armor swap via EQUIPPABLE data component.
public class HeavyDummyEntity extends LivingEntity {

    private static final EntityDataAccessor<String> DATA_SKIN_NAME =
            SynchedEntityData.defineId(HeavyDummyEntity.class, EntityDataSerializers.STRING);
    // 26.1: EntityDataSerializers.OPTIONAL_UUID is gone. Use a synced
    // String — empty means "no resolved UUID". Disk side still uses
    // UUIDUtil.CODEC for a clean round-trip.
    private static final EntityDataAccessor<String> DATA_SKIN_UUID =
            SynchedEntityData.defineId(HeavyDummyEntity.class, EntityDataSerializers.STRING);

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

    // 26.1: Entity#hurt is final; override hurtServer instead.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Accept damage (so the hit registers + arrow sticks) but the tick()
        // hook below heals back to full HP every tick — a damage sponge.
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
        builder.define(DATA_SKIN_UUID, "");
    }

    /** Set the player-skin name (display only) AND the resolved UUID
     *  (the actual skin texture is fetched from the UUID client-side). */
    public void setSkin(String name, UUID uuid) {
        this.entityData.set(DATA_SKIN_NAME, name == null ? "" : name);
        this.entityData.set(DATA_SKIN_UUID, uuid == null ? "" : uuid.toString());
    }

    public String getSkinName() {
        return this.entityData.get(DATA_SKIN_NAME);
    }

    public Optional<UUID> getSkinUUID() {
        String s = this.entityData.get(DATA_SKIN_UUID);
        if (s.isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("SkinName", this.getSkinName());
        // 26.1: EntityDataSerializers.OPTIONAL_UUID kept on the wire but
        // disk-side we go through UUIDUtil.CODEC — output.store skips
        // writing when the optional is empty.
        getSkinUUID().ifPresent(uuid -> compound.store("SkinUUID", UUIDUtil.CODEC, uuid));
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        // CompoundTag.getString returns Optional<String> in 26.1
        String name = compound.getString("SkinName").orElse("");
        UUID uuid = compound.read("SkinUUID", UUIDUtil.CODEC).orElse(null);
        setSkin(name, uuid);
    }
}
