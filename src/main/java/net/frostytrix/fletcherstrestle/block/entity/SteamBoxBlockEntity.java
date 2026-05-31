package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.frostytrix.fletcherstrestle.recipe.SteamingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
            }
        }
    };

    public FluidTank getFluidTank() {
        return this.fluidTank;
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

                            // Get output from JSON and clear input slot
                            ItemStack result = recipe.assemble(input, level.registryAccess());
                            itemHandler.extractItem(i, 1, false);

                            // Drop item on top of the block
                            ItemEntity droppedItem = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
                            level.addFreshEntity(droppedItem);

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