package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.block.custom.SteamBoxBlock;
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

    public final FluidTank fluidTank = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

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

        updateWaterLevelState(level, pos, state);

        if (!hasHeat) return; // Stop processing if fire goes out

        for (int i = 0; i < 16; i++) {
            ItemStack currentItem = itemHandler.getStackInSlot(i);

            if (!currentItem.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(currentItem);
                var recipeHolder = ((net.minecraft.server.level.ServerLevel) level).recipeAccess().getRecipeFor(ModRecipes.STEAMING_TYPE.get(), input, level);

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
                            ItemStack result = recipe.assemble(input);
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

    // TODO(port-26.1): save/load stubbed — full migration needs:
    //   - ItemStackHandler.serializeNBT/deserializeNBT signatures changed
    //   - FluidTank.writeToNBT/readFromNBT likewise
    //   - tag.getIntArray returns Optional now (input.getIntArray same)
    // Until ported, the steam box loses state across saves but compiles.
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("cookingTimes", cookingTimes);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        cookingTimes = input.getIntArray("cookingTimes").orElse(new int[16]);
        if (cookingTimes.length != 16) cookingTimes = new int[16];
    }
}