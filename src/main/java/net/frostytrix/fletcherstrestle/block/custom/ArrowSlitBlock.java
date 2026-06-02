package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.ArrowSlitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Arrow slit / loophole that disguises itself as any block placed into it,
 * adopting that block's look (via a dynamic model) and hardness / blast
 * resistance. Solid cover with a firing port — arrows pass through it, mobs and
 * melee don't. Directional; collision matches the {@code block/arrow_slit} model.
 */
public class ArrowSlitBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<ArrowSlitBlock> CODEC = simpleCodec(ArrowSlitBlock::new);

    // Default (model as authored): port runs along X — walls on N/S, open E/W.
    private static final VoxelShape SHAPE_X = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(0, 3, 0, 16, 13, 6),
            Block.box(0, 3, 10, 16, 13, 16));
    // Port runs along Z (model rotated 90°) — walls on E/W.
    private static final VoxelShape SHAPE_Z = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 13, 0, 16, 16, 16),
            Block.box(0, 3, 0, 6, 13, 16),
            Block.box(10, 3, 0, 16, 13, 16));

    public ArrowSlitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArrowSlitBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_Z : SHAPE_X;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    // --- Disguise: install a block with a block item, remove with empty hand + sneak ---

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !isValidMimic(blockItem.getBlock())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof ArrowSlitBlockEntity slit) {
            if (!level.isClientSide) {
                if (slit.hasMimic() && !player.getAbilities().instabuild) {
                    Block.popResource(level, pos, new ItemStack(slit.getMimic().getBlock()));
                }
                slit.setMimic(blockItem.getBlock().defaultBlockState());
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                SoundType sound = blockItem.getBlock().defaultBlockState().getSoundType();
                level.playSound(null, pos, sound.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                        (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof ArrowSlitBlockEntity slit && slit.hasMimic()) {
            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild) {
                    Block.popResource(level, pos, new ItemStack(slit.getMimic().getBlock()));
                }
                slit.setMimic(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** A valid disguise is a normal full-cube model block (not a slit, not a BE block). */
    private static boolean isValidMimic(Block block) {
        if (block instanceof ArrowSlitBlock) return false;
        BlockState s = block.defaultBlockState();
        if (s.getRenderShape() != RenderShape.MODEL || s.hasBlockEntity()) return false;
        return s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ArrowSlitBlockEntity slit && slit.hasMimic()) {
            return slit.getMimic().getLightEmission(level, pos);
        }
        return super.getLightEmission(state, level, pos);
    }

    // --- Delegate the "feel" of the disguise: hardness + blast resistance ---

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ArrowSlitBlockEntity slit && slit.hasMimic()) {
            return slit.getMimic().getDestroyProgress(player, level, pos);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof ArrowSlitBlockEntity slit && slit.hasMimic()) {
            return slit.getMimic().getBlock().getExplosionResistance();
        }
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    // The worn block drops alongside the slit, through the loot system — so it
    // follows the same rules (nothing drops on a creative break).
    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ArrowSlitBlockEntity slit
                && slit.hasMimic()) {
            drops.add(new ItemStack(slit.getMimic().getBlock()));
        }
        return drops;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
