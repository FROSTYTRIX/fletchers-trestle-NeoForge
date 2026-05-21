package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ShavingHorseBlockEntity extends BlockEntity {

    public int currentShaves = 0;

    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public ShavingHorseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAVING_HORSE_BE.get(), pos, state);
    }

    // TODO(port-26.1): packet/sync + inventory save stubbed.
    // ItemStackHandler.serializeNBT/deserializeNBT signature changed;
    // getUpdateTag/getUpdatePacket/onDataPacket also use ValueOutput now.
    // Only the simple int counter persists for now.

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("currentShaves", currentShaves);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        currentShaves = input.getIntOr("currentShaves", 0);
    }
}