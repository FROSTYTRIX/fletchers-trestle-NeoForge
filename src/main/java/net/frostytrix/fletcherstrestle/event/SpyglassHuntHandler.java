package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Phase 6 — Spyglass-driven hunt activation.
// Server-side per-player state tracks how long the player has been steadily
// looking at the same living entity through a spyglass. Once the threshold
// is reached, a nearby tamed eagle owned by that player is sent into hunt
// mode targeting that entity.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class SpyglassHuntHandler {

    private SpyglassHuntHandler() {}

    // Tunables — kept here so they're easy to find when iterating on feel.
    private static final int    LOCK_ON_TICKS  = 60;   // 3 seconds at 20 tps
    private static final double RAY_RANGE      = 48.0; // blocks
    private static final double EAGLE_RANGE    = 16.0; // owner -> eagle search radius

    // Per-player lock-on state. Lives on the server only.
    private static final Map<UUID, LockOnData> LOCK_DATA = new HashMap<>();

    private static final class LockOnData {
        int targetEntityId = -1;
        int ticksLooking   = 0;

        void reset() {
            targetEntityId = -1;
            ticksLooking   = 0;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Server side only — we never trust client input for entity targeting.
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer)) return;

        LockOnData data = LOCK_DATA.computeIfAbsent(player.getUUID(), id -> new LockOnData());

        // Must be actively using a spyglass (right-mouse held).
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof SpyglassItem)) {
            data.reset();
            return;
        }

        LivingEntity hit = raycastLivingTarget(player);
        if (hit == null || hit == player) {
            data.reset();
            return;
        }

        if (data.targetEntityId == hit.getId()) {
            data.ticksLooking++;
            if (data.ticksLooking >= LOCK_ON_TICKS) {
                triggerHunt(player, hit);
                data.reset();
            }
        } else {
            data.targetEntityId = hit.getId();
            data.ticksLooking   = 1;
        }
    }

    // Raycast forward up to RAY_RANGE blocks and return the first living
    // entity in the line of sight, or null. We use the player's actual look
    // vector and check entity hitboxes along the way.
    private static LivingEntity raycastLivingTarget(Player player) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end  = eye.add(look.x * RAY_RANGE, look.y * RAY_RANGE, look.z * RAY_RANGE);

        // Find how far the ray reaches before hitting a truly opaque block.
        // Leaves, glass, iron bars, ice, etc. are skipped — they don't block
        // a spyglass scope in spirit. We stop at the first block whose state
        // reports canOcclude() (full opaque face).
        double maxDistSqr = occludingHitDistanceSqr(player.level(), eye, end, look, player);

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RAY_RANGE)).inflate(1.0);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && e.isPickable() && !(e instanceof EagleEntity));

        LivingEntity best = null;
        double bestDistSqr = maxDistSqr;
        for (LivingEntity e : candidates) {
            // Inflate hitbox a touch so a slightly-off aim still counts.
            AABB hitbox = e.getBoundingBox().inflate(0.3);
            var clip = hitbox.clip(eye, end);
            if (clip.isPresent()) {
                double d = clip.get().distanceToSqr(eye);
                if (d < bestDistSqr) {
                    best = e;
                    bestDistSqr = d;
                }
            }
        }
        return best;
    }

    // Walk the ray segment-by-segment using Level.clip(), and when we hit a
    // non-occluding block (glass, leaves, iron bars, etc.) advance past it
    // and continue. Stops at the first truly opaque block, or at the end of
    // the ray. Returns the squared distance from `eye` to the stopping point.
    private static double occludingHitDistanceSqr(Level level, Vec3 eye, Vec3 end, Vec3 look, Player player) {
        Vec3 from = eye;
        // Safety cap on iterations so a degenerate setup can't loop forever.
        for (int i = 0; i < 32; i++) {
            BlockHitResult hit = level.clip(new ClipContext(
                    from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK) {
                // Clear all the way to `end`.
                return RAY_RANGE * RAY_RANGE;
            }
            BlockState state = level.getBlockState(hit.getBlockPos());
            if (state.canOcclude()) {
                // True opaque block — vision stops here.
                return hit.getLocation().distanceToSqr(eye);
            }
            // Non-occluding (leaves, glass, bars, ice, ...). Step a tiny bit
            // past the hit point and continue the trace.
            from = hit.getLocation().add(look.x * 0.01, look.y * 0.01, look.z * 0.01);
        }
        return RAY_RANGE * RAY_RANGE;
    }

    private static void triggerHunt(Player player, LivingEntity target) {
        AABB searchArea = player.getBoundingBox().inflate(EAGLE_RANGE);
        // Mutual exclusion: only pick an eagle that's idle. Don't yank one
        // out of an active fetch/return/hunt cycle.
        List<EagleEntity> nearby = player.level().getEntitiesOfClass(
                EagleEntity.class, searchArea,
                e -> e.isTame()
                        && e.isOwnedBy(player)
                        && !e.isOrderedToSit()
                        && e.getEagleState() == EagleEntity.STATE_IDLE);

        if (nearby.isEmpty()) {
            // Nothing to do — player has no available eagle. Stay quiet
            // so the player can still scope around without spam.
            return;
        }
        // Pick the closest available eagle.
        EagleEntity chosen = nearby.get(0);
        double bestDistSqr = chosen.distanceToSqr(player);
        for (int i = 1; i < nearby.size(); i++) {
            double d = nearby.get(i).distanceToSqr(player);
            if (d < bestDistSqr) { chosen = nearby.get(i); bestDistSqr = d; }
        }
        chosen.setHuntTarget(target);
        player.displayClientMessage(
                Component.literal("Your eagle locks onto " + target.getName().getString() + "."),
                true);
    }

    // Clear lock-on state when the player leaves so the map doesn't grow forever.
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOCK_DATA.remove(event.getEntity().getUUID());
    }
}
