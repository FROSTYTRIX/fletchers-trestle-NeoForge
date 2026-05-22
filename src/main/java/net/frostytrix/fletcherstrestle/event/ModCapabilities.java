package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.capability.FluidTankResourceAdapter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// 26.1: capability tokens moved.
// * Old: Capabilities.FluidHandler.BLOCK : BlockCapability<IFluidHandler, Direction>
// * New: Capabilities.Fluid.BLOCK        : BlockCapability<ResourceHandler<FluidResource>, Direction>
// FluidTank doesn't implement ResourceHandler<FluidResource>, so we expose
// it through FluidTankResourceAdapter (see capability/FluidTankResourceAdapter.java).
// This restores pipe/bucket/cauldron interaction with the dipping vat while
// leaving the BE's internal storage untouched.
//
// TODO(port-26.1): when DippingVatBlockEntity is fully ported to
// FluidStacksResourceHandler, drop the adapter and provide the handler directly.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Fluid.BLOCK,
                ModBlockEntities.DIPPING_VAT_BE.get(),
                (vat, side) -> new FluidTankResourceAdapter(vat.getFluidTank())
        );
    }
}
