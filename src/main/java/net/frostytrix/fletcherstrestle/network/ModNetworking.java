package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(FletcherTrestle.MOD_ID);

        // --- MISSING PACKETS ADDED HERE ---

        // Client → Server : change tab in the fletching menu
        registrar.playToServer(
                FletchingTabPayload.TYPE,
                FletchingTabPayload.STREAM_CODEC,
                FletchingTabPayload::handleData
        );

        // Client → Server : change active quiver slot
        registrar.playToServer(
                QuiverSlotPacket.TYPE,
                QuiverSlotPacket.CODEC,
                QuiverSlotPacket::handle
        );

        // ----------------------------------

        registrar.playToServer(
                TuningPacket.TYPE,
                TuningPacket.CODEC,
                TuningPacket::handle
        );

        // Server → Client : sync shot list to the archery target GUI
        registrar.playToClient(
                TargetSyncPacket.TYPE,
                TargetSyncPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
        );

        registrar.playToServer(
                ClearShotsPacket.TYPE,
                ClearShotsPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
        );

        registrar.playToServer(
                MountSyncPayload.TYPE,
                MountSyncPayload.CODEC,
                ModNetworking::handleMountSync
        );

        // Server → Client : sync archery XP + skill ranks
        registrar.playToClient(
                ArcherySyncPacket.TYPE,
                ArcherySyncPacket.CODEC,
                ArcherySyncPacket::handle
        );

        // Client → Server : spend a skill point
        registrar.playToServer(
                SpendSkillPacket.TYPE,
                SpendSkillPacket.CODEC,
                SpendSkillPacket::handle
        );
    }

    public static void handleMountSync(final MountSyncPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Find the player who sent the packet
            Entity sender = context.player();
            if (sender == null || sender.level() == null) return;

            Entity entity = sender.level().getEntity(payload.entityId());

            // Only apply if it's a living mount the sender is actually riding.
            if (entity instanceof LivingEntity mount && mount.hasPassenger(sender)) {
                mount.setYRot(payload.yRot());
                mount.yBodyRot = payload.yRot();
                mount.yHeadRot = payload.yRot();
            }
        });
    }
}