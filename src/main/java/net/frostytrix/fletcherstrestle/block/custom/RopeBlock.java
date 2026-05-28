package net.frostytrix.fletcherstrestle.block.custom;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RopeBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty PERSISTENT = BooleanProperty.create("persistent");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int duration = 600;

    // A thin hitbox in the center of the block for targeting/climbing
    private static final VoxelShape SHAPE = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);

    public RopeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BOTTOM, false)
                .setValue(PERSISTENT, true) // Defaults to true for player-placed items
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // --- PLACEMENT & SUPPORT LOGIC ---

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        boolean isBottom = !context.getLevel().getBlockState(context.getClickedPos().below()).is(this);

        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER)
                .setValue(BOTTOM, isBottom);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState stateAbove = level.getBlockState(above);
        // The rope survives if the block above is either another rope OR a solid downward face
        return stateAbove.is(this) || stateAbove.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        // If we lost our support from above, break instantly
        if (!state.canSurvive(level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        // Dynamically update the 'BOTTOM' visual state if the block below changes
        if (direction == Direction.DOWN) {
            return state.setValue(BOTTOM, !neighborState.is(this));
        }

        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    // --- DECAY LOGIC (For Arrow-Deployed Ropes) ---

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !state.getValue(PERSISTENT)) {
            // Schedule destruction 600 ticks (30 seconds) after being spawned
            level.scheduleTick(pos, this, duration);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PERSISTENT)) {
            // Silently destroy the block without dropping items
            level.destroyBlock(pos, false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(PERSISTENT) && !player.level().isClientSide && !level.getBlockState(pos.below()).is(ModBlocks.ROPE.get())) {
            if (!state.getValue(BOTTOM)) {
                level.setBlockAndUpdate(pos, state.setValue(BOTTOM, true));
                return InteractionResult.SUCCESS;
            } else {
                level.setBlockAndUpdate(pos, state.setValue(BOTTOM, false));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 1. Only react if the player is holding the Rope item
        if (stack.is(this.asItem())) {

            // If the player is sneaking, we let them place it normally (against the side/top)
            if (player.isSecondaryUseActive()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                // 2. Find the bottom of the current rope chain
                BlockPos.MutableBlockPos searchPos = pos.mutable();

                // Move down as long as the block at searchPos is a Rope
                while (level.getBlockState(searchPos).is(this)) {
                    searchPos.move(Direction.DOWN);

                    // Safety: Stop if we go outside the world
                    if (level.isOutsideBuildHeight(searchPos)) {
                        return ItemInteractionResult.FAIL;
                    }
                }

                // 3. We've found the first non-rope block. Can we place a rope here?
                BlockState targetState = level.getBlockState(searchPos);
                if (targetState.canBeReplaced()) {

                    // Use the default state, but ensure it's PERSISTENT so it doesn't decay
                    BlockState newState = this.defaultBlockState()
                            .setValue(PERSISTENT, true)
                            .setValue(BOTTOM, true);

                    level.setBlock(searchPos, newState, 3);

                    // 4. Effects and Item consumption
                    level.playSound(null, searchPos, SoundEvents.WOOL_PLACE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    return ItemInteractionResult.SUCCESS;
                }
            } else {
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // --- WATERLOGGING & DATA ---

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOTTOM, PERSISTENT, WATERLOGGED);
    }
}