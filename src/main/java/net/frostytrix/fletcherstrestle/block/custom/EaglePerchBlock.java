package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.EaglePerchBlockEntity;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

// Eagle Perch — a craftable stand a tamed eagle can claim as its home base.
// Stores ownership in its BlockEntity; the eagle stores the perch position.
// Together they form a stable "this eagle belongs to this perch" link that
// survives chunk unloads.
//
// Interactions (must be by the claiming owner, except first claim):
//   - Right-click unclaimed perch (empty hand): claim using your nearest
//                                                idle owned eagle (within 16 blocks)
//   - Right-click claimed perch (empty hand):    show ownership info
//   - Sneak + right-click claimed perch:         unclaim (owner only)
public class EaglePerchBlock extends BaseEntityBlock {

    public static final MapCodec<EaglePerchBlock> CODEC = simpleCodec(EaglePerchBlock::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    // Collision = just base + pole. The crossbar is decorative so the
    // player doesn't catch on it while walking past.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2, 0, 2, 14, 2, 14),  // base
            Block.box(6, 2, 6, 10, 12, 10)   // pole
    );

    public EaglePerchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Crossbar faces the player so the eagle perches "facing" them
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EaglePerchBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (!(level.getBlockEntity(pos) instanceof EaglePerchBlockEntity perch)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            // Unclaim — only the owner can do this.
            if (perch.isClaimed() && player.getUUID().equals(perch.getOwnerUUID())) {
                clearEaglePerchPos(level, perch.getEagleUUID());
                perch.unclaim();
                player.displayClientMessage(Component.literal("Perch unclaimed."), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (perch.isClaimed()) {
            // Show info
            String ownerLabel = perch.getOwnerName() != null ? perch.getOwnerName() : "unknown";
            player.displayClientMessage(
                    Component.literal("Perch claimed by " + ownerLabel + "."), true);
            return InteractionResult.SUCCESS;
        }

        // Unclaimed — try to claim using the nearest idle owned eagle.
        EagleEntity eagle = findClaimableEagle(level, player, pos);
        if (eagle == null) {
            player.displayClientMessage(
                    Component.literal("No idle eagle nearby to claim this perch."), true);
            return InteractionResult.SUCCESS;
        }
        perch.claim(player.getUUID(), player.getName().getString(), eagle.getUUID());
        eagle.setPerchPos(pos);
        player.displayClientMessage(
                Component.literal("Eagle bound to this perch."), true);
        return InteractionResult.SUCCESS;
    }

    // Find the nearest idle owned eagle to bind to this perch.
    @Nullable
    private static EagleEntity findClaimableEagle(Level level, Player player, BlockPos pos) {
        AABB search = new AABB(pos).inflate(16.0);
        List<EagleEntity> nearby = level.getEntitiesOfClass(
                EagleEntity.class, search,
                e -> e.isOwnedBy(player)
                        && !e.isOrderedToSit()
                        && e.getEagleState() == EagleEntity.STATE_IDLE);
        if (nearby.isEmpty()) return null;
        EagleEntity best = nearby.get(0);
        double bestDistSqr = best.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        for (int i = 1; i < nearby.size(); i++) {
            double d = nearby.get(i).distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestDistSqr) {
                best = nearby.get(i);
                bestDistSqr = d;
            }
        }
        return best;
    }

    // When the perch is unclaimed (or broken), clear the eagle's pointer so
    // its AI doesn't try to fly to a dead perch.
    private static void clearEaglePerchPos(Level level, @Nullable UUID eagleUUID) {
        if (eagleUUID == null || level.isClientSide) return;
        AABB world = new AABB(-30_000_000, -64, -30_000_000, 30_000_000, 320, 30_000_000);
        for (EagleEntity e : level.getEntitiesOfClass(EagleEntity.class, world,
                e -> eagleUUID.equals(e.getUUID()))) {
            e.setPerchPos(null);
        }
    }

    // If the perch gets broken, drop the bound eagle's pointer too.
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof EaglePerchBlockEntity perch) {
                clearEaglePerchPos(level, perch.getEagleUUID());
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
