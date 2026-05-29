package net.frostytrix.fletcherstrestle.block.custom;

import net.frostytrix.fletcherstrestle.menu.CrossbowBenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Crossbow Bench — assembles bows into crossbows (and back) and installs
 * crossbow attachments. Transient like the vanilla smithing table: the menu
 * holds the work item only while open and returns it on close, so no block
 * entity is needed.
 */
public class CrossbowBenchBlock extends Block {

    private static final Component TITLE =
            Component.translatable("block.fletcherstrestle.crossbow_bench");

    public CrossbowBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new CrossbowBenchMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    TITLE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
