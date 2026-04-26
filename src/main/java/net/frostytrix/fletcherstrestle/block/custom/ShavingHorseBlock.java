package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.ShavingHorseBlockEntity;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
            // If the player holds a Drawknife AND there is a log stored inside
            if (stack.is(ModItems.DRAWKNIFE.get()) && !storedLog.isEmpty()) {

                // 1. Get the rough limb based on the stored log
                ItemStack roughLimb = getRoughLimbFromLog(storedLog);

                if (!roughLimb.isEmpty()) {
                    // 2. Consume the log from the block
                    horse.itemHandler.extractItem(0, 1, false);

                    // 3. Drop the rough limb into the world
                    ItemEntity droppedItem = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, roughLimb);
                    level.addFreshEntity(droppedItem);

                    // 4. Damage the drawknife by 1 point!
                    stack.hurtAndBreak(1, player, Player.getSlotForHand(hand));

                    // 5. Play carving sound
                    level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // --- ACTION 2: STOCKING THE LOG ---
            // If the player holds a Log AND the horse is currently empty
            if (stack.is(ItemTags.LOGS) && storedLog.isEmpty()) {
                // Insert 1 log
                horse.itemHandler.insertItem(0, new ItemStack(stack.getItem(), 1), false);
                horse.setChanged(); // Marque le bloc comme "modifié" pour la sauvegarde
                level.sendBlockUpdated(pos, state, state, 3);

                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                // Play a heavy wood placement sound
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                return ItemInteractionResult.SUCCESS;
            }

            // --- ACTION 3: REMOVING THE LOG ---
            // If they sneak-right-click with an empty hand, give the log back
            if (stack.isEmpty() && player.isShiftKeyDown() && !storedLog.isEmpty()) {
                ItemStack extracted = horse.itemHandler.extractItem(0, 1, false);
                player.addItem(extracted);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private ItemStack getRoughLimbFromLog(ItemStack log) {
        String name = log.getItem().toString();

        if (name.contains("dark_oak")) return new ItemStack(ModItems.ROUGH_DARK_OAK_LIMB.get());
        if (name.contains("pale_oak")) return new ItemStack(ModItems.ROUGH_PALE_OAK_LIMB.get());
        if (name.contains("oak")) return new ItemStack(ModItems.ROUGH_OAK_LIMB.get());

        if (name.contains("birch")) return new ItemStack(ModItems.ROUGH_BIRCH_LIMB.get());
        if (name.contains("spruce")) return new ItemStack(ModItems.ROUGH_SPRUCE_LIMB.get());
        if (name.contains("jungle")) return new ItemStack(ModItems.ROUGH_JUNGLE_LIMB.get());
        if (name.contains("acacia")) return new ItemStack(ModItems.ROUGH_ACACIA_LIMB.get());
        if (name.contains("crimson")) return new ItemStack(ModItems.ROUGH_CRIMSON_LIMB.get());
        if (name.contains("warped")) return new ItemStack(ModItems.ROUGH_WARPED_LIMB.get());
        if (name.contains("mangrove")) return new ItemStack(ModItems.ROUGH_MANGROVE_LIMB.get());
        if (name.contains("cherry")) return new ItemStack(ModItems.ROUGH_CHERRY_LIMB.get());

        // If it's a modded log we don't support, return empty so nothing happens
        return ItemStack.EMPTY;
    }
}