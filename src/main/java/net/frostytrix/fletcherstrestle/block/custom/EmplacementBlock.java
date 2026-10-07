package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.EmplacementBlockEntity;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
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
 * A swivel mount for a crossbow, crewed by a garrison golem.
 *
 * <p>The facing is the centre of its firing cone. On its own it does nothing: a
 * golem has to stand behind it, load and shoot. Right-click with a crossbow to
 * mount it, with a quiver to fit the ammunition, or with arrows to top the quiver
 * up. Empty-handed, it gives back the quiver first, then the crossbow (sneak to
 * take the crossbow straight away). A redstone signal holds its fire.</p>
 */
public class EmplacementBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<EmplacementBlock> CODEC = simpleCodec(EmplacementBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(6, 2, 6, 10, 14, 10),
            Block.box(4, 14, 4, 12, 16, 12));

    public EmplacementBlock(Properties properties) {
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

    /** It faces the way the player is looking: you place it pointing at the field it covers. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmplacementBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EmplacementBlockEntity post)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (EmplacementBlockEntity.canMount(held) && post.getCrossbow().isEmpty()) {
            post.setCrossbow(held.split(1));
            level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
            return ItemInteractionResult.CONSUME;
        }
        if (held.getItem() instanceof ModularQuiverItem && post.getQuiver().isEmpty()) {
            post.setQuiver(held.split(1));
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
            return ItemInteractionResult.CONSUME;
        }
        if (held.getItem() instanceof ArrowItem && !post.getQuiver().isEmpty()) {
            ItemStack quiver = post.getQuiver().copy();
            if (ModularQuiverItem.insert(quiver, held)) {
                post.setQuiver(quiver);
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 1.0f);
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.FAIL;
        }
        if (held.isEmpty()) {
            boolean takeQuiver = !post.getQuiver().isEmpty() && !player.isSecondaryUseActive();
            ItemStack taken = takeQuiver ? post.getQuiver() : post.getCrossbow();
            if (taken.isEmpty()) {
                taken = post.getQuiver();
                takeQuiver = true;
            }
            if (taken.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            player.setItemInHand(hand, taken);
            if (takeQuiver) {
                post.setQuiver(ItemStack.EMPTY);
            } else {
                post.setCrossbow(ItemStack.EMPTY);
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EmplacementBlockEntity post) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), post.getCrossbow());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), post.getQuiver());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparators read how full the quiver is. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof EmplacementBlockEntity post) || post.getQuiver().isEmpty()) {
            return 0;
        }
        ItemStack quiver = post.getQuiver();
        int slots = quiver.getOrDefault(net.frostytrix.fletcherstrestle.component.ModDataComponents.MAX_QUIVER_SLOTS.get(), 9);
        int arrows = 0;
        for (ItemStack stack : ModularQuiverItem.getQuiverContents(quiver)) {
            arrows += stack.getCount();
        }
        if (arrows == 0) return 0;
        return 1 + Math.round(14.0f * arrows / (slots * 64.0f));
    }
}
