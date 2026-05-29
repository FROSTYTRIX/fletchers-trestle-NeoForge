package net.frostytrix.fletcherstrestle.block.custom;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.block.entity.CrossbowBenchBlockEntity;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.menu.CrossbowBenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Crossbow Bench — assembles bows into crossbows (and back) and installs
 * crossbow attachments. Backed by a {@link CrossbowBenchBlockEntity} so its
 * work slots persist across reloads and drop when the block is broken.
 */
public class CrossbowBenchBlock extends BaseEntityBlock {

    public static final MapCodec<CrossbowBenchBlock> CODEC = simpleCodec(CrossbowBenchBlock::new);

    public CrossbowBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrossbowBenchBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrossbowBenchBlockEntity be) {
            SimpleContainer inv = be.getInventory();
            ItemStack input = inv.getItem(CrossbowBenchMenu.SLOT_INPUT);
            // A finished crossbow already contains its trigger + attachment, so the
            // trigger/attachment slots are only representations — don't drop those
            // or they'd duplicate. Otherwise the slots hold real items: drop all.
            boolean inputIsCrossbow = input.is(ModItems.MODULAR_CROSSBOW.get());
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), input);
            if (!inputIsCrossbow) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        inv.getItem(CrossbowBenchMenu.SLOT_TRIGGER));
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        inv.getItem(CrossbowBenchMenu.SLOT_ATTACHMENT));
            }
            inv.clearContent();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
