package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(FletcherTrestle.MOD_ID);

        // Client → Server : change tab in the fletching menu
        registrar.playToServer(
                TuningPacket.TYPE,
                TuningPacket.CODEC,
                (payload, context) -> context.enqueueWork(() -> payload.handle(context.player()))
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
    }
}