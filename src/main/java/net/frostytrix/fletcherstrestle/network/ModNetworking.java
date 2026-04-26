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
        // Create a registrar for our mod
        final PayloadRegistrar registrar = event.registrar(FletcherTrestle.MOD_ID);

        // Register our TuningPacket. It goes FROM the Client TO the Server.
        registrar.playToServer(
                TuningPacket.TYPE,
                TuningPacket.CODEC,
                (payload, context) -> {
                    // This enqueueWork ensures the logic runs safely on the main server thread
                    context.enqueueWork(() -> payload.handle(context.player()));
                }
        );
    }
}
