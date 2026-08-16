package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.EagleNestBlockEntity;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// Eagle Nest. Sits on top of a block, can be claimed by a player, and used
// as a breeding destination. Has an egg_count blockstate (0–3) driven by the
// block entity's egg list so visuals stay in sync with state.
//
// Interactions:
//   - Right-click empty hand on unclaimed nest: claim
//   - Right-click empty hand on claimed nest:   show ownership + status
//   - Sneak + right-click claimed nest (owner): unclaim
//   - Right-click with Eagle Egg in hand:       handled by EagleEggItem
//   - Break nest:                                drops itself + any eggs as items
public class EagleNestBlock extends BaseEntityBlock {

    public static final MapCodec<EagleNestBlock> CODEC = simpleCodec(EagleNestBlock::new);
    public static final IntegerProperty EGG_COUNT =
            IntegerProperty.create("egg_count", 0, EagleNestBlockEntity.MAX_EGGS);

    // Low bowl shape, ~3px tall: enough to feel like a perch for the bird
    // but doesn't block walking past.
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 4, 14);

    public EagleNestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EGG_COUNT, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGG_COUNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EagleNestBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.EAGLE_NEST_BE.get(),
                EagleNestBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        if (!(level.getBlockEntity(pos) instanceof EagleNestBlockEntity nest)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (nest.isClaimed() && player.getUUID().equals(nest.getOwnerUUID())) {
                nest.unclaim();
                player.displayClientMessage(Component.literal("Nest unclaimed."), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (nest.isClaimed()) {
            String label = nest.getOwnerName() != null ? nest.getOwnerName() : "unknown";
            int eggs = nest.eggCount();
            player.displayClientMessage(
                    Component.literal("Nest of " + label + ": " + eggs + " egg(s)."),
                    true);
            return InteractionResult.SUCCESS;
        }

        nest.claim(player.getUUID(), player.getName().getString());
        player.displayClientMessage(
                Component.literal("You claimed this nest."), true);
        return InteractionResult.SUCCESS;
    }

    // On break, drop any remaining eggs as items so the player isn't punished
    // for relocating their nest mid-incubation. (Hatch progress is lost: eggs
    // come back as fresh items.)
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof EagleNestBlockEntity nest) {
                int count = nest.eggCount();
                if (count > 0 && !level.isClientSide) {
                    Block.popResource(level, pos,
                            new ItemStack(ModItems.EAGLE_EGG.get(), count));
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
