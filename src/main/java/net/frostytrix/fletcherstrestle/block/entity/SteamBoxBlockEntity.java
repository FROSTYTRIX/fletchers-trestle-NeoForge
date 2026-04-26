package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.block.custom.SteamBoxBlock;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SteamBoxBlockEntity extends BlockEntity {

    public final ItemStackHandler itemHandler = new ItemStackHandler(16) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public final FluidTank fluidTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public int[] cookingTimes = new int[16];
    public final int maxProgress = 200;

    public SteamBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_BOX_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) return;

        BlockState blockBelow = level.getBlockState(pos.below());
        boolean hasHeat = blockBelow.getBlock() instanceof CampfireBlock && blockBelow.getValue(CampfireBlock.LIT);

        updateWaterLevelState(level, pos, state);

        if (!hasHeat || fluidTank.getFluidAmount() < 250) {
            return;
        }

        for (int i = 0; i < 16; i++) {
            ItemStack currentItem = itemHandler.getStackInSlot(i);

            if (!currentItem.isEmpty()) {
                cookingTimes[i]++;

                if (cookingTimes[i] >= maxProgress) {

                    if (fluidTank.getFluidAmount() >= 250) {

                        fluidTank.drain(250, IFluidHandler.FluidAction.EXECUTE);

                        ItemStack result = getPliableLimb(currentItem);
                        itemHandler.extractItem(i, 1, false);

                        ItemEntity droppedItem = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
                        level.addFreshEntity(droppedItem);

                        cookingTimes[i] = 0;
                    } else {
                        cookingTimes[i] = maxProgress - 1;
                    }
                }
            } else {
                cookingTimes[i] = 0;
            }
        }
    }

    private void updateWaterLevelState(Level level, BlockPos pos, BlockState state) {
        int amount = fluidTank.getFluidAmount();
        int newLevel = 0;

        if (amount > 3000) newLevel = 4;
        else if (amount > 2000) newLevel = 3;
        else if (amount > 1000) newLevel = 2;
        else if (amount > 0) newLevel = 1;

        if (state.getValue(SteamBoxBlock.WATER_LEVEL) != newLevel) {
            level.setBlock(pos, state.setValue(SteamBoxBlock.WATER_LEVEL, newLevel), 3);
        }
    }

    private ItemStack getPliableLimb(ItemStack input) {
        if (input.is(ModItems.ROUGH_OAK_LIMB.get())) return new ItemStack(ModItems.PLIABLE_OAK_LIMB.get());
        if (input.is(ModItems.ROUGH_BIRCH_LIMB.get())) return new ItemStack(ModItems.PLIABLE_BIRCH_LIMB.get());
        if (input.is(ModItems.ROUGH_SPRUCE_LIMB.get())) return new ItemStack(ModItems.PLIABLE_SPRUCE_LIMB.get());
        if (input.is(ModItems.ROUGH_JUNGLE_LIMB.get())) return new ItemStack(ModItems.PLIABLE_JUNGLE_LIMB.get());
        if (input.is(ModItems.ROUGH_ACACIA_LIMB.get())) return new ItemStack(ModItems.PLIABLE_ACACIA_LIMB.get());
        if (input.is(ModItems.ROUGH_DARK_OAK_LIMB.get())) return new ItemStack(ModItems.PLIABLE_DARK_OAK_LIMB.get());
        if (input.is(ModItems.ROUGH_MANGROVE_LIMB.get())) return new ItemStack(ModItems.PLIABLE_MANGROVE_LIMB.get());
        if (input.is(ModItems.ROUGH_CRIMSON_LIMB.get())) return new ItemStack(ModItems.PLIABLE_CRIMSON_LIMB.get());
        if (input.is(ModItems.ROUGH_WARPED_LIMB.get())) return new ItemStack(ModItems.PLIABLE_WARPED_LIMB.get());
        if (input.is(ModItems.ROUGH_MANGROVE_LIMB.get())) return new ItemStack(ModItems.PLIABLE_MANGROVE_LIMB.get());
        if (input.is(ModItems.ROUGH_CHERRY_LIMB.get())) return new ItemStack(ModItems.PLIABLE_CHERRY_LIMB.get());

        // Fallback (just in case someone forces an item in via commands)
        return new ItemStack(ModItems.PLIABLE_OAK_LIMB.get());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putIntArray("cookingTimes", cookingTimes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        fluidTank.readFromNBT(registries, tag.getCompound("fluid"));
        cookingTimes = tag.getIntArray("cookingTimes");
        if (cookingTimes.length != 16) cookingTimes = new int[16]; // Safety check
    }
}
