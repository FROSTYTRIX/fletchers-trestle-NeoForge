package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// Block entity for the Eagle Perch. Tracks which player claimed the perch
// and which of their tamed eagles is assigned to it. Pair survives chunk
// unloads via NBT so the AI's "return to my perch" behaviour is stable.
public class EaglePerchBlockEntity extends BlockEntity {

    @Nullable private UUID ownerUUID;
    @Nullable private UUID eagleUUID;
    @Nullable private String ownerName;

    public EaglePerchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EAGLE_PERCH_BE.get(), pos, state);
    }

    public boolean isClaimed() { return ownerUUID != null; }
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

    // 26.1: save/load now use ValueOutput / ValueInput. UUIDs go through
    // UUIDUtil.CODEC since the old putUUID/getUUID helpers are gone.
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (ownerUUID != null) output.store("Owner", UUIDUtil.CODEC, ownerUUID);
        if (eagleUUID != null) output.store("Eagle", UUIDUtil.CODEC, eagleUUID);
        if (ownerName != null) output.putString("OwnerName", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ownerUUID = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.eagleUUID = input.read("Eagle", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getString("OwnerName").orElse(null);
    }
}
