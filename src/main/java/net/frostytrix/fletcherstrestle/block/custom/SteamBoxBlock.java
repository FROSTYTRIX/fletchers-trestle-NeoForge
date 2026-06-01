package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.block.entity.SteamBoxBlockEntity;
import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class SteamBoxBlock extends BaseEntityBlock {
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 12.5D, 15.0D);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public SteamBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // A comparator reads the water tank's fill level (0 = empty, 15 = full).
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SteamBoxBlockEntity steamBox) {
            int amount = steamBox.fluidTank.getFluidAmount();
            int capacity = steamBox.fluidTank.getCapacity();
            if (amount <= 0 || capacity <= 0) return 0;
            return Math.max(1, amount * 15 / capacity);
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamBoxBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof SteamBoxBlockEntity steamBox) {

            // --- ACTION 1: ADDING WATER ---
            if (stack.is(Items.WATER_BUCKET)) {
                int filled = steamBox.fluidTank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

                if (filled > 0) {
                    if (!player.isCreative()) {
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    }
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // --- ACTION 1b: REMOVING WATER (Empty Bucket) ---
            if (stack.is(Items.BUCKET)) {
                FluidStack drained = steamBox.fluidTank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                if (drained.getAmount() >= 1000) {
                    steamBox.fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
                        if (stack.isEmpty()) {
                            player.setItemInHand(hand, waterBucket);
                        } else if (!player.getInventory().add(waterBucket)) {
                            player.drop(waterBucket, false);
                        }
                    }
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // --- ACTION 2: INSERTING VALID RECIPE ITEMS ---
            if (!stack.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(stack);
                var recipe = level.getRecipeManager().getRecipeFor(ModRecipes.STEAMING_TYPE.get(), input, level);

                if (recipe.isPresent()) {
                    for (int i = 0; i < 16; i++) {
                        if (steamBox.itemHandler.getStackInSlot(i).isEmpty()) {
                            steamBox.itemHandler.insertItem(i, new ItemStack(stack.getItem(), 1), false);

                            if (!player.isCreative()) stack.shrink(1);

                            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, 0.8F);
                            return ItemInteractionResult.SUCCESS;
                        }
                    }
                }
            }

            // --- ACTION 3: EXTRACTING ITEMS (Empty Hand / Sneaking) ---
            if (stack.isEmpty() && player.isShiftKeyDown()) {
                ItemStack extracted = steamBox.itemHandler.extractItem(0, 1, false);
                if (!extracted.isEmpty()) {
                    player.addItem(extracted);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.STEAM_BOX_BE.get(),
                (lvl, pos, st, blockEntity) -> blockEntity.tick(lvl, pos, st));
    }
}