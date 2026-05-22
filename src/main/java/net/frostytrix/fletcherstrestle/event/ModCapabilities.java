package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// TODO(port-26.1): capability registration stubbed.
// The 1.21.1 form was:
//   event.registerBlockEntity(
//       Capabilities.FluidHandler.BLOCK,
//       ModBlockEntities.DIPPING_VAT_BE.get(),
//       (be, dir) -> be.getFluidTank()
//   );
// 26.1 emptied out `Capabilities` (the FluidHandler.BLOCK token moved). Until
// the new resource-handler capability tokens are wired up, pipes/tanks can't
// pull fluid from the dipping vat automatically — players have to use the
// vat's manual interactions (bottles, buckets) instead.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Intentionally empty until the capability tokens are ported.
    }
}
