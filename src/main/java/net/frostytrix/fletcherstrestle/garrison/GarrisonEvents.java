package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Winding up a golem, and the hand-off of its bolts.
 *
 * <p>A golem is four blocks of {@code #fletcherstrestle:garrison_golem_body}
 * (stripped logs and woods) in a T: one with another on either side of it,
 * standing on a fourth. Using a {@code #fletcherstrestle:mechanisms} item (the
 * Mechanical Trigger) on the middle of the top row winds it up. Its drops are
 * the {@code entities/garrison_golem} loot table.</p>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class GarrisonEvents {
    private GarrisonEvents() {
    }

    @SubscribeEvent
    public static void onUseTrigger(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!held.is(ModTags.Items.MECHANISMS)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) {
            // Let the client swing; the server decides.
            if (isStrippedLog(event.getLevel().getBlockState(event.getPos()))) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        BlockPos top = event.getPos();
        List<BlockPos> body = golemBody(level, top);
        if (body.isEmpty()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        for (BlockPos pos : body) {
            BlockState state = level.getBlockState(pos);
            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        BlockPos feet = top.below();
        Player player = event.getEntity();
        GarrisonGolemEntity golem = ModEntities.GARRISON_GOLEM.get().create(level);
        if (golem == null) return;
        golem.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        golem.setYHeadRot(golem.getYRot());
        golem.yBodyRot = golem.getYRot();
        golem.setHome(feet);
        EventHooks.finalizeMobSpawn(golem, level, level.getCurrentDifficultyAt(feet), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(golem);
        level.playSound(null, feet, SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), SoundSource.NEUTRAL, 1.0f, 0.8f);

        for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(nearby, golem);
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        for (BlockPos pos : body) {
            level.blockUpdated(pos, Blocks.AIR);
        }
    }

    /**
     * The four logs of a golem whose top-middle log is {@code top}, or an empty
     * list. The arms may run east-west or north-south.
     */
    private static List<BlockPos> golemBody(ServerLevel level, BlockPos top) {
        if (!isStrippedLog(level.getBlockState(top)) || !isStrippedLog(level.getBlockState(top.below()))) {
            return List.of();
        }
        for (Direction side : new Direction[]{Direction.EAST, Direction.SOUTH}) {
            BlockPos a = top.relative(side);
            BlockPos b = top.relative(side.getOpposite());
            if (isStrippedLog(level.getBlockState(a)) && isStrippedLog(level.getBlockState(b))) {
                return List.of(top, top.below(), a, b);
            }
        }
        return List.of();
    }

    private static boolean isStrippedLog(BlockState state) {
        return state.is(ModTags.Blocks.GARRISON_GOLEM_BODY);
    }

    /**
     * A golem's bolts leave from the crossbow on the post, not from the golem's
     * head, and they can be picked back up: the ammunition was the player's.
     */
    @SubscribeEvent
    public static void onBoltJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof GarrisonGolemEntity golem)) return;
        Vec3 muzzle = golem.muzzle();
        if (muzzle != null) {
            arrow.setPos(muzzle);
        }
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
    }
}
