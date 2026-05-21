package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.entity.ArcheryTargetBlockEntity;
import net.frostytrix.fletcherstrestle.block.entity.ShotRecord;
import net.frostytrix.fletcherstrestle.menu.FletchingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Game-bus events for things that can't easily live on items/entities:
//   - Right-clicking a vanilla fletching table opens our custom menu.
//   - Arrows striking an archery target block record a shot on its BE.
// Material-specific arrow effects (spruce punch, copper conductivity, etc.)
// live on the items/entities themselves — keep this file lean.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModServerEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)) return;

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

        if (!event.getLevel().isClientSide()) {
            event.getEntity().openMenu(new SimpleMenuProvider(
                    (id, inv, player) -> new FletchingMenu(id, inv),
                    Component.translatable("block.minecraft.fletching_table")
            ));
        }
    }

    @SubscribeEvent
    public static void onArrowHitTarget(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(event.getRayTraceResult() instanceof BlockHitResult blockHit)) return;

        BlockPos hitPos = blockHit.getBlockPos();
        if (!(arrow.level().getBlockEntity(hitPos) instanceof ArcheryTargetBlockEntity target)) return;

        BlockState state = arrow.level().getBlockState(hitPos);
        if (!state.is(ModBlocks.ARCHERY_TARGET.get())) return;

        double localX = blockHit.getLocation().x - hitPos.getX();
        double localY = blockHit.getLocation().y - hitPos.getY();
        double localZ = blockHit.getLocation().z - hitPos.getZ();

        float u;
        float v;
        switch (state.getValue(net.frostytrix.fletcherstrestle.block.custom.ArcheryTargetBlock.FACING)) {
            case NORTH -> { u = (float) (1.0D - localX); v = (float) (1.0D - localY); }
            case SOUTH -> { u = (float)  localX;        v = (float) (1.0D - localY); }
            case EAST  -> { u = (float) (1.0D - localZ); v = (float) (1.0D - localY); }
            case WEST  -> { u = (float)  localZ;        v = (float) (1.0D - localY); }
            default    -> { u = 0.5F; v = 0.5F; }
        }
        u = Math.max(0.0F, Math.min(1.0F, u));
        v = Math.max(0.0F, Math.min(1.0F, v));

        float speed = (float) arrow.getDeltaMovement().length();
        float estimatedDamage = (float) (arrow.getBaseDamage() * Math.max(0.25D, speed));

        target.addShot(new ShotRecord(
                localX, localY, localZ, u, v, estimatedDamage, speed,
                arrow.level().getGameTime()
        ));

        for (var player : arrow.level().players()) {
            if (player.containerMenu instanceof net.frostytrix.fletcherstrestle.menu.ArcheryTargetMenu menu
                    && menu.isFor(hitPos)
                    && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                menu.syncToClient(sp);
            }
        }
    }
}
