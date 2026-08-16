package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.Objects;

/**
 * Holds the block an arrow slit is disguised as ("mimic"). The mimic's
 * appearance is supplied to the dynamic model via {@link #MIMIC} model data,
 * and its hardness / blast resistance are delegated by the block.
 */
public class ArrowSlitBlockEntity extends BlockEntity {

    /** Model-data key the client model reads to know which block to wear. */
    public static final ModelProperty<BlockState> MIMIC = new ModelProperty<>();

    private BlockState mimic = Blocks.AIR.defaultBlockState();

    public ArrowSlitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARROW_SLIT_BE.get(), pos, state);
    }

    public BlockState getMimic() {
        return mimic;
    }

    public boolean hasMimic() {
        return !mimic.isAir();
    }

    /** Server-side: set the disguise and push it to clients. */
    public void setMimic(BlockState newMimic) {
        if (Objects.equals(this.mimic, newMimic)) return;
        this.mimic = newMimic;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // The mimic can emit light (e.g. glowstone): re-evaluate lighting
            // here since the blockstate itself didn't change.
            level.getLightEngine().checkBlock(worldPosition);
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MIMIC, mimic).build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Mimic", NbtUtils.writeBlockState(mimic));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Mimic")) {
            this.mimic = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Mimic"));
        } else {
            this.mimic = Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup) {
        BlockState previous = this.mimic;
        super.onDataPacket(connection, pkt, lookup);
        if (level != null && level.isClientSide() && !Objects.equals(previous, this.mimic)) {
            requestModelDataUpdate();
            // The mimic's light emission changed but the blockstate didn't, so the
            // client light engine isn't notified by the normal path: recompute it
            // here (handles both lighting up and going back to 0).
            level.getLightEngine().checkBlock(worldPosition);
            // Force the section to recompile now: setBlocksDirty(same,same) wouldn't.
            net.minecraft.client.Minecraft.getInstance().levelRenderer.setSectionDirty(
                    net.minecraft.core.SectionPos.blockToSectionCoord(worldPosition.getX()),
                    net.minecraft.core.SectionPos.blockToSectionCoord(worldPosition.getY()),
                    net.minecraft.core.SectionPos.blockToSectionCoord(worldPosition.getZ()));
        }
    }
}
