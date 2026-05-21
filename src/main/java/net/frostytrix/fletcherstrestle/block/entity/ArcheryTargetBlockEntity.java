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

    // TODO(port-26.1): shot list save + packet sync stubbed.
    // ShotRecord.toNBT/fromNBT needs migrating to a Codec, and the
    // getUpdateTag/getUpdatePacket/onDataPacket trio uses ValueOutput now.
    // Until ported, shots are lost on save/reload and don't sync to the
    // client GUI (which is also stubbed).

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        shots.clear();
    }
}