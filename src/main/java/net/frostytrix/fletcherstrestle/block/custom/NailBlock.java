package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.NailBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A small nail driven into a block face. On its own it is a decoration; its
 * purpose is to be an anchor point for a garland, which is strung between two
 * nails.
 *
 * <p>Extends the vanilla face-attached block (the button and lever base), so it
 * can be placed on a floor, wall or ceiling, requires a solid face behind it,
 * and pops off when that face is removed.</p>
 */
public class NailBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<NailBlock> CODEC = simpleCodec(NailBlock::new);

    // Small nail head sitting against whichever face it was driven into.
    private static final VoxelShape FLOOR_SHAPE = Block.box(6, 0, 6, 10, 4, 10);
    private static final VoxelShape CEILING_SHAPE = Block.box(6, 12, 6, 10, 16, 10);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 4);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 12, 10, 10, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 4, 10, 10);
    private static final VoxelShape EAST_SHAPE = Block.box(12, 6, 6, 16, 10, 10);

    public NailBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // getConnectedDirection points away from the support, so its opposite is
        // the face the nail is driven into.
        return switch (getConnectedDirection(state).getOpposite()) {
            case DOWN -> FLOOR_SHAPE;
            case UP -> CEILING_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    /** The face this nail is driven into. Public so the renderer can tie the cord to the head. */
    public static Direction supportFace(BlockState state) {
        return getConnectedDirection(state).getOpposite();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NailBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof NailBlockEntity nail) {
            // Breaking a nail takes down every garland tied to it, returning the
            // items and clearing the link from the nail at the far end.
            nail.dropAndClearGarlands();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
