package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.frostytrix.fletcherstrestle.recipe.SteamingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
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

    // Water-only tank so it plays nice with other mods' pipes/pumps:
    // the validator rejects every fluid except water on insertion.
    public final FluidTank fluidTank = new FluidTank(4000,
            fluidStack -> fluidStack.getFluid() == net.minecraft.world.level.material.Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            // Push the new fluid amount to clients so the renderer's water
            // surface updates immediately (incl. external pipe/pump fills).
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                // Keep any attached comparator in sync with the tank level.
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };

    public FluidTank getFluidTank() {
        return this.fluidTank;
    }

    // View of the inventory exposed to hoppers/pipes: it only accepts raw
    // (steamable) limbs and only releases finished ones, so automation can't
    // pull half-cooked items or jam finished slots with new inputs.
    private IItemHandler automationHandler;

    public IItemHandler getAutomationHandler() {
        if (automationHandler == null) {
            automationHandler = new AutomationItemHandler();
        }
        return automationHandler;
    }

    /** True if the stack has a steaming recipe (i.e. it's a raw input). */
    private boolean hasSteamingRecipe(ItemStack stack) {
        if (stack.isEmpty() || level == null) return false;
        return level.getRecipeManager()
                .getRecipeFor(ModRecipes.STEAMING_TYPE.get(), new SingleRecipeInput(stack), level)
                .isPresent();
    }

    private class AutomationItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // Only raw, steamable inputs may be inserted by automation.
            if (stack.isEmpty() || !hasSteamingRecipe(stack)) return stack;
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Only finished limbs (no steaming recipe left) may be pulled out.
            ItemStack inSlot = itemHandler.getStackInSlot(slot);
            if (inSlot.isEmpty() || hasSteamingRecipe(inSlot)) return ItemStack.EMPTY;
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return hasSteamingRecipe(stack);
        }
    }

    public int[] cookingTimes = new int[16];

    public SteamBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_BOX_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) return;

        BlockState blockBelow = level.getBlockState(pos.below());

        boolean hasHeat = false;
        if (blockBelow.is(net.minecraft.tags.BlockTags.CAMPFIRES) && blockBelow.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)) {
            hasHeat = blockBelow.getValue(net.minecraft.world.level.block.CampfireBlock.LIT);
        } else if (blockBelow.is(BlockTags.FIRE) || blockBelow.is(Blocks.MAGMA_BLOCK) || blockBelow.is(Blocks.LAVA) || blockBelow.is(Blocks.LAVA_CAULDRON)) {
            hasHeat = true;
        }

        if (!hasHeat) return; // Stop processing if fire goes out

        for (int i = 0; i < 16; i++) {
            ItemStack currentItem = itemHandler.getStackInSlot(i);

            if (!currentItem.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(currentItem);
                var recipeHolder = level.getRecipeManager().getRecipeFor(ModRecipes.STEAMING_TYPE.get(), input, level);

                if (recipeHolder.isPresent()) {
                    SteamingRecipe recipe = recipeHolder.get().value();
                    int requiredWater = recipe.getWaterAmount();
                    int requiredTime = recipe.getProcessingTime();

                    cookingTimes[i]++;

                    if (cookingTimes[i] >= requiredTime) {
                        if (fluidTank.getFluidAmount() >= requiredWater) {
                            // Consume water
                            fluidTank.drain(requiredWater, IFluidHandler.FluidAction.EXECUTE);

                            // Replace the raw limb with the finished one in-place so it
                            // can be pulled out by hand or by a hopper underneath.
                            ItemStack result = recipe.assemble(input, level.registryAccess());
                            itemHandler.setStackInSlot(i, result);

                            cookingTimes[i] = 0;
                        } else {
                            // Pause progress just before finishing if waiting on water
                            cookingTimes[i] = requiredTime - 1;
                        }
                    }
                } else {
                    // Item in slot has no steaming recipe
                    cookingTimes[i] = 0;
                }
            } else {
                cookingTimes[i] = 0;
            }
        }
    }

    // --- NETWORK SYNC FOR THE RENDERER ---
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
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
        if (cookingTimes.length != 16) cookingTimes = new int[16];
    }
}