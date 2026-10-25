package net.frostytrix.fletcherstrestle.garrison;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.frostytrix.fletcherstrestle.progression.GarrisonTrigger;
import net.frostytrix.fletcherstrestle.progression.ModCriteria;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/**
 * Winding up a golem, the hand-off of its bolts, and Field of Fire.
 *
 * <p>A golem is four blocks of {@code #fletcherstrestle:garrison_golem_body}
 * (stripped logs and woods) in a T: one with another on either side of it,
 * standing on a fourth. Using a {@code #fletcherstrestle:mechanisms} item (the
 * Mechanical Trigger) on the middle of the top row winds it up. It takes that
 * log's wood, and drops two of those logs on top of the {@code
 * entities/garrison_golem} loot table.</p>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class GarrisonEvents {
    /** How many posts a garrison needs for Field of Fire. */
    private static final int FIELD_OF_FIRE_POSTS = 3;

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
        // The warden is made of the log that becomes its chest: that sets its look and its drops.
        BlockState chest = level.getBlockState(top);
        GarrisonGolemEntity golem = ModEntities.GARRISON_GOLEM.get().create(level);
        if (golem == null) return;
        golem.setLogs(chest, level, top);

        for (BlockPos pos : body) {
            BlockState state = level.getBlockState(pos);
            level.levelEvent(2001, pos, Block.getId(state));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        BlockPos feet = top.below();
        Player player = event.getEntity();
        golem.setBuilder(player.getUUID());
        golem.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        golem.setYHeadRot(golem.getYRot());
        golem.yBodyRot = golem.getYRot();
        golem.setHome(feet);
        EventHooks.finalizeMobSpawn(golem, level, level.getCurrentDifficultyAt(feet), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(golem);
        level.playSound(null, feet, ModSounds.GARRISON_WIND_UP.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);

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
     * Field of Fire: a captain shot down by a warden whose garrison has at least
     * {@link #FIELD_OF_FIRE_POSTS} posts. It goes to whoever built the warden.
     */
    @SubscribeEvent
    public static void onCaptainDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Raider raider) || !raider.isCaptain()) return;
        if (!(event.getSource().getEntity() instanceof GarrisonGolemEntity golem)) return;
        if (!(golem.level() instanceof ServerLevel level) || golem.getBuilder() == null) return;
        if (CrewPostGoal.nearbyPosts(golem, level).size() < FIELD_OF_FIRE_POSTS) return;
        if (level.getPlayerByUUID(golem.getBuilder()) instanceof ServerPlayer builder) {
            ModCriteria.GARRISON.get().trigger(builder, GarrisonTrigger.FIELD_OF_FIRE);
        }
    }

    /**
     * A golem's bolts leave from the crossbow on the post, not from the golem's
     * head, and they can be picked back up: the ammunition was the player's.
     *
     * <p>The crossbow rests just above the post's turntable, so a bolt fired
     * steeply down would bury itself in its own stand. It starts where its path
     * leaves the post's block instead; the post's sight checks leave that block
     * out the same way.</p>
     */
    @SubscribeEvent
    public static void onBoltJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof GarrisonGolemEntity golem)) return;
        Vec3 muzzle = golem.muzzle();
        if (muzzle != null) {
            BlockPos post = golem.firingFrom();
            Vec3 heading = arrow.getDeltaMovement();
            double out = post != null && heading.lengthSqr() > 1.0E-6
                    ? exitDistance(muzzle, heading.normalize(), new AABB(post)) : 0.0;
            arrow.setPos(out > 0.0 ? muzzle.add(heading.normalize().scale(out + 0.05)) : muzzle);
        }
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
    }

    /** How far along a ray it leaves a box it passes through, or 0 if it misses the box. */
    private static double exitDistance(Vec3 from, Vec3 dir, AABB box) {
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        double[] start = {from.x, from.y, from.z};
        double[] step = {dir.x, dir.y, dir.z};
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(step[axis]) < 1.0E-9) {
                if (start[axis] < min[axis] || start[axis] > max[axis]) return 0.0;
                continue;
            }
            double a = (min[axis] - start[axis]) / step[axis];
            double b = (max[axis] - start[axis]) / step[axis];
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        return exit > Math.max(enter, 0.0) ? exit : 0.0;
    }
}
