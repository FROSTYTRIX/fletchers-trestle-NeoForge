package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the single weapon shown on a {@code WeaponRackBlock}. The stack is
 * synced to the client so the renderer can draw it, including its modular
 * assembly, so the rack shows the real bow you built.
 */
public class WeaponRackBlockEntity extends BlockEntity {

    private ItemStack displayedItem = ItemStack.EMPTY;

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPON_RACK_BE.get(), pos, state);
    }

    /** Racks take bows and crossbows: this mod's and vanilla's alike. */
    public static boolean canDisplay(ItemStack stack) {
        return stack.getItem() instanceof ModularBowItem
                || stack.getItem() instanceof ModularCrossbowItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    public ItemStack getDisplayedItem() {
        return displayedItem;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // ALWAYS write the key, even when empty. If the update tag comes out
        // completely empty the client ignores it, so clearing the rack would
        // never reach the client and it would keep rendering the old weapon.
        tag.put("DisplayedItem", displayedItem.isEmpty()
                ? new CompoundTag()
                : displayedItem.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.displayedItem = tag.contains("DisplayedItem")
                ? ItemStack.parseOptional(registries, tag.getCompound("DisplayedItem"))
                : ItemStack.EMPTY;
    }

    // --- Client sync: without these the rack renders empty until a reload. ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
