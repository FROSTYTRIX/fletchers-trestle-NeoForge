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

        if (hasHeatBelow(level, pos)) {
            for (int i = 0; i < 16; i++) {
                ItemStack currentItem = itemHandler.getStackInSlot(i);

                if (!currentItem.isEmpty()) {
                    SingleRecipeInput input = new SingleRecipeInput(currentItem);
                    var recipeHolder = level.getRecipeManager().getRecipeFor(ModRecipes.STEAMING_TYPE.get(), input, level);

                    if (recipeHolder.isPresent()) {
                        SteamingRecipe recipe = recipeHolder.get().value();
                        int requiredWater = recipe.getWaterAmount();
                        int requiredTime = recipe.getProcessingTime();

                        // Progress only advances while there's enough water in the tank.
                        if (fluidTank.getFluidAmount() >= requiredWater) {
                            cookingTimes[i]++;

                            if (cookingTimes[i] >= requiredTime) {
                                // Consume water
                                fluidTank.drain(requiredWater, IFluidHandler.FluidAction.EXECUTE);

                                // Buffer the finished limb in its slot. If an item pipe is
                                // attached it gets pulled out; otherwise it's ejected below.
                                ItemStack result = recipe.assemble(input, level.registryAccess());
                                itemHandler.setStackInSlot(i, result);

                                cookingTimes[i] = 0;
                            }
                        }
                    } else {
                        // Item in slot has no steaming recipe (e.g. a finished limb)
                        cookingTimes[i] = 0;
                    }
                } else {
                    cookingTimes[i] = 0;
                }
            }
        }

        // Move finished limbs out of the (GUI-less) box:
        //   1. push them into an adjacent chest or barrel that has room,
        //   2. if a chest/barrel IS next to it but full, leave them buffered (wait for room),
        //   3. if no chest/barrel is attached, pop them out on top.
        if (hasFinishedLimbs()) {
            pushFinishedLimbsToNeighbors(level, pos);
            if (hasFinishedLimbs() && !hasChestOrBarrelNeighbor(level, pos)) {
                ejectFinishedLimbs(level, pos);
            }
        }
    }

    /** Pushes finished limbs into an adjacent chest or barrel that has room (never into pipes/machines). */
    private void pushFinishedLimbsToNeighbors(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir == net.minecraft.core.Direction.DOWN) continue; // heat source is below
            BlockPos neighbor = pos.relative(dir);
            if (!isChestOrBarrel(level, neighbor)) continue;

            IItemHandler dest = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    neighbor, dir.getOpposite());
            if (dest == null) continue;

            for (int i = 0; i < 16; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (stack.isEmpty() || hasSteamingRecipe(stack)) continue; // finished limbs only
                ItemStack remainder = net.neoforged.neoforge.items.ItemHandlerHelper
                        .insertItemStacked(dest, stack, false);
                itemHandler.setStackInSlot(i, remainder);
            }

            if (!hasFinishedLimbs()) return; // everything placed
        }
    }

    /** True if the block is a (vanilla or modded) chest or barrel. */
    private static boolean isChestOrBarrel(Level level, BlockPos pos) {
        net.minecraft.world.level.block.Block block = level.getBlockState(pos).getBlock();
        return block instanceof net.minecraft.world.level.block.ChestBlock
                || block instanceof net.minecraft.world.level.block.BarrelBlock;
    }

    /** True if any non-bottom side has a chest or barrel. */
    private static boolean hasChestOrBarrelNeighbor(Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (dir == net.minecraft.core.Direction.DOWN) continue;
            if (isChestOrBarrel(level, pos.relative(dir))) return true;
        }
        return false;
    }

    /** True if at least one slot holds a finished (non-steamable) limb. */
    private boolean hasFinishedLimbs() {
        for (int i = 0; i < 16; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && !hasSteamingRecipe(stack)) return true;
        }
        return false;
    }

    /** Pops every finished limb out just above the box. */
    private void ejectFinishedLimbs(Level level, BlockPos pos) {
        for (int i = 0; i < 16; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty() || hasSteamingRecipe(stack)) continue;
            ItemStack out = itemHandler.extractItem(i, stack.getCount(), false);
            if (out.isEmpty()) continue;
            ItemEntity drop = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, out);
            drop.setDeltaMovement(0.0, 0.05, 0.0);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    /** True if there's an active heat source directly under the block. */
    public static boolean hasHeatBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(BlockTags.CAMPFIRES) && below.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)) {
            return below.getValue(net.minecraft.world.level.block.CampfireBlock.LIT);
        }
        return below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK)
                || below.is(Blocks.LAVA) || below.is(Blocks.LAVA_CAULDRON);
    }

    /** Highest cooking progress across all slots, 0-100 (for tooltip mods). */
    public int getDisplayProgress() {
        if (level == null) return 0;
        int best = 0;
        for (int i = 0; i < 16; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            var holder = level.getRecipeManager()
                    .getRecipeFor(ModRecipes.STEAMING_TYPE.get(), new SingleRecipeInput(stack), level);
            if (holder.isPresent()) {
                int time = holder.get().value().getProcessingTime();
                if (time > 0) {
                    best = Math.max(best, Math.min(100, cookingTimes[i] * 100 / time));
                }
            }
        }
        return best;
    }

    /** True if at least one raw limb is currently steaming. */
    public boolean hasCookingItems() {
        for (int i = 0; i < 16; i++) {
            if (hasSteamingRecipe(itemHandler.getStackInSlot(i))) return true;
        }
        return false;
    }

    /** Shared status line for the tooltip-mod integrations (Jade / TOP / WTHIT). */
    public static net.minecraft.network.chat.Component statusLine(boolean busy, boolean heat, boolean water, int progress) {
        if (!busy) {
            return net.minecraft.network.chat.Component.translatable("fletcherstrestle.tooltip.steam_box.idle");
        }
        if (!heat) {
            return net.minecraft.network.chat.Component.translatable("fletcherstrestle.tooltip.steam_box.no_heat");
        }
        if (!water) {
            return net.minecraft.network.chat.Component.translatable("fletcherstrestle.tooltip.steam_box.no_water");
        }
        return net.minecraft.network.chat.Component.translatable("fletcherstrestle.tooltip.steam_box.steaming", progress);
    }

    /** True if the tank holds enough water to advance at least one steaming limb. */
    public boolean hasWaterToSteam() {
        if (level == null) return false;
        for (int i = 0; i < 16; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            var holder = level.getRecipeManager()
                    .getRecipeFor(ModRecipes.STEAMING_TYPE.get(), new SingleRecipeInput(stack), level);
            if (holder.isPresent() && fluidTank.getFluidAmount() >= holder.get().value().getWaterAmount()) {
                return true;
            }
        }
        return false;
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