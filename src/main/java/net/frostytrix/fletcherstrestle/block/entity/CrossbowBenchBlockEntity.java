package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.menu.CrossbowBenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent backing inventory for the Crossbow Bench. Holding the three work
 * slots in a block entity (rather than a transient menu container) means items
 * survive a game close / world reload and drop when the block is broken,
 * fixing the "items vanish on quit" bug.
 */
public class CrossbowBenchBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SIZE = 3;

    private final SimpleContainer inventory = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();                       // notify open menus (listeners)
            CrossbowBenchBlockEntity.this.setChanged(); // persist
        }
    };

    public CrossbowBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CROSSBOW_BENCH_BE.get(), pos, state);
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) {
            items.set(i, this.inventory.getItem(i));
        }
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < SIZE; i++) {
            this.inventory.setItem(i, items.get(i));
        }
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fletcherstrestle.crossbow_bench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CrossbowBenchMenu(id, playerInventory, this.inventory,
                ContainerLevelAccess.create(this.level, this.worldPosition));
    }
}
