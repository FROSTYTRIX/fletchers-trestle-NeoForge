package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArcheryTargetBlockEntity extends BlockEntity {

    public static final int MAX_SHOTS = 50;

    private final List<ShotRecord> shots = new ArrayList<>();

    public ArcheryTargetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCHERY_TARGET_BE.get(), pos, state);
    }

    // --- Data access ---

    public List<ShotRecord> getShots() {
        return Collections.unmodifiableList(shots);
    }

    public void addShot(ShotRecord record) {
        if (shots.size() >= MAX_SHOTS) {
            shots.remove(0); // Drop oldest if full
        }
        shots.add(record);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearShots() {
        shots.clear();
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- NBT Save / Load ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ShotRecord shot : shots) {
            list.add(shot.toNBT());
        }
        tag.put("shots", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shots.clear();
        if (tag.contains("shots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("shots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                shots.add(ShotRecord.fromNBT(list.getCompound(i)));
            }
        }
    }

    // --- Sync client <-> server (for GUI) ---

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }
}