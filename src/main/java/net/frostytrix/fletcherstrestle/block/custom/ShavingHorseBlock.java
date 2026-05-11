package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.ShavingHorseBlockEntity;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ShavingHorseBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.5D, 16.0D);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public ShavingHorseBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShavingHorseBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof ShavingHorseBlockEntity horse) {
            ItemStack storedLog = horse.itemHandler.getStackInSlot(0);

            // --- ACTION 1: USING THE DRAWKNIFE ---
            if (stack.is(ModItems.DRAWKNIFE.get()) && !storedLog.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(storedLog);
                var recipeHolder = level.getRecipeManager().getRecipeFor(ModRecipes.SHAVING_TYPE.get(), input, level);

                if (recipeHolder.isPresent()) {
                    horse.currentShaves++;

                    stack.hurtAndBreak(1, player, Player.getSlotForHand(hand));
                    level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (horse.currentShaves >= recipeHolder.get().value().getShavesRequired()) {
                        ItemStack result = recipeHolder.get().value().assemble(input, level.registryAccess());

                        horse.itemHandler.extractItem(0, 1, false);
                        horse.currentShaves = 0;

                        ItemEntity droppedItem = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
                        level.addFreshEntity(droppedItem);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // --- ACTION 2: STOCKING THE BLOCK ---
            if (storedLog.isEmpty() && !stack.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(stack);
                var recipeHolder = level.getRecipeManager().getRecipeFor(ModRecipes.SHAVING_TYPE.get(), input, level);

                if (recipeHolder.isPresent()) {
                    horse.itemHandler.insertItem(0, new ItemStack(stack.getItem(), 1), false);
                    horse.currentShaves = 0;

                    horse.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);

                    if (!player.isCreative()) stack.shrink(1);

                    level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // --- ACTION 3: REMOVING THE BLOCK ---
            if (stack.isEmpty() && player.isShiftKeyDown() && !storedLog.isEmpty()) {
                ItemStack extracted = horse.itemHandler.extractItem(0, 1, false);
                horse.currentShaves = 0;
                player.addItem(extracted);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}