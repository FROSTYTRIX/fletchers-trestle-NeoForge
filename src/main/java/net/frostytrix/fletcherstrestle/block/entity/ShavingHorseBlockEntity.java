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

    // 26.1: ItemStackHandler.serialize(ValueOutput)/.deserialize(ValueInput)
    // replace the old serializeNBT/deserializeNBT pair. Saving into a child
    // ValueOutput keeps the inventory blob namespaced and matches what
    // SteamBoxBlockEntity does.
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("currentShaves", currentShaves);
        itemHandler.serialize(output.child("inventory"));
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        currentShaves = input.getIntOr("currentShaves", 0);
        input.child("inventory").ifPresent(itemHandler::deserialize);
    }

    // 26.1 client sync — vanilla BlockEntity.getUpdateTag defaults to an
    // empty CompoundTag, which is why the held log didn't show on remote
    // clients: the renderer ran but the item slot was always EMPTY on the
    // client side. saveCustomOnly walks our saveAdditional, so the
    // inventory + shave counter ride along through the update packet.
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}