package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Block entity for the Eagle Perch. Tracks which player claimed the perch
// and which of their tamed eagles is assigned to it. The pair survives
// chunk unloads via NBT so the AI's "return to my perch" behavior is stable.
public class EaglePerchBlockEntity extends BlockEntity {

    @Nullable
    private UUID ownerUUID;
    @Nullable
    private UUID eagleUUID;
    // Cached at claim time. UUIDs only resolve to names through the live
    // player list, so caching the name makes the "claimed by ..." message
    // work even when the owner is offline.
    @Nullable
    private String ownerName;

    public EaglePerchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EAGLE_PERCH_BE.get(), pos, state);
    }

    public boolean isClaimed() {
        return ownerUUID != null;
    }

    @Nullable public UUID   getOwnerUUID() { return ownerUUID; }
    @Nullable public UUID   getEagleUUID() { return eagleUUID; }
    @Nullable public String getOwnerName() { return ownerName; }

    public void claim(UUID owner, String name, UUID eagle) {
        this.ownerUUID = owner;
        this.ownerName = name;
        this.eagleUUID = eagle;
        setChanged();
    }

    public void unclaim() {
        this.ownerUUID = null;
        this.ownerName = null;
        this.eagleUUID = null;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (eagleUUID != null) tag.putUUID("Eagle", eagleUUID);
        if (ownerName != null) tag.putString("OwnerName", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.eagleUUID = tag.hasUUID("Eagle") ? tag.getUUID("Eagle") : null;
        this.ownerName = tag.contains("OwnerName") ? tag.getString("OwnerName") : null;
    }
}
