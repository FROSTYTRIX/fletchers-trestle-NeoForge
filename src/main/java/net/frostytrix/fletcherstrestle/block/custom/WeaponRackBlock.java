package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.WeaponRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A wall-mounted rack that displays one modular bow or crossbow.
 *
 * <p>The block model is only the rack itself: a backboard and two pegs. The
 * weapon is drawn on top at runtime by {@code WeaponRackRenderer}, so a racked
 * bow shows its own limb/riser/string materials rather than a generic icon.</p>
 *
 * <p>Right-click with a weapon to hang it; right-click empty-handed to take it
 * back. Breaking the rack drops whatever was on it.</p>
 */
public class WeaponRackBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<WeaponRackBlock> CODEC = simpleCodec(WeaponRackBlock::new);

    // Shallow fixture hugging the wall: backboard (1px) + pegs (4px out).
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2, 6, 15, 14, 9, 16),   // backboard
            Block.box(3, 7, 11, 5, 9, 15),    // left peg
            Block.box(11, 7, 11, 13, 9, 15)); // right peg
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(2, 6, 0, 14, 9, 1),
            Block.box(3, 7, 1, 5, 9, 5),
            Block.box(11, 7, 1, 13, 9, 5));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(15, 6, 2, 16, 9, 14),
            Block.box(11, 7, 3, 15, 9, 5),
            Block.box(11, 7, 11, 15, 9, 13));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(0, 6, 2, 1, 9, 14),
            Block.box(1, 7, 3, 5, 9, 5),
            Block.box(1, 7, 11, 5, 9, 13));

    public WeaponRackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Wall-mounted like a torch: it hangs on the face you clicked, so FACING
        // (which points out of the wall) is that face. Fall back through the
        // other looking directions if the clicked one has no wall behind it.
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = this.defaultBlockState().setValue(FACING, direction.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        // Needs a solid wall behind it, exactly like a wall torch.
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Break off (dropping the rack and anything on it) when its wall goes.
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponRackBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        ItemStack displayed = rack.getDisplayedItem();

        if (!displayed.isEmpty()) {
            // Take the weapon back: into the hand if it's free, else the inventory.
            if (held.isEmpty()) {
                player.setItemInHand(hand, displayed);
            } else if (!player.getInventory().add(displayed)) {
                player.drop(displayed, false);
            }
            rack.setDisplayedItem(ItemStack.EMPTY);
            return ItemInteractionResult.CONSUME;
        }

        if (WeaponRackBlockEntity.canDisplay(held)) {
            rack.setDisplayedItem(held.split(1));
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            ItemStack displayed = rack.getDisplayedItem();
            if (!displayed.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), displayed);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
