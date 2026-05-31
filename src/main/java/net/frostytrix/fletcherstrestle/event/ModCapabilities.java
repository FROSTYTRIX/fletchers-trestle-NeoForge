package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // On enregistre la capacité de gestion de fluide pour notre Dipping Vat
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, // Le type de capacité (Fluide pour Bloc)
                ModBlockEntities.DIPPING_VAT_BE.get(), // Ton BlockEntityType enregistré
                (blockEntity, direction) -> blockEntity.getFluidTank() // Ce qu'on fournit au tuyau
        );

        // Steam Box: water-only tank exposed to pipes/pumps from other mods.
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.STEAM_BOX_BE.get(),
                (blockEntity, direction) -> blockEntity.getFluidTank()
        );

        // Steam Box: hopper/pipe automation — insert raw limbs, extract finished ones.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.STEAM_BOX_BE.get(),
                (blockEntity, direction) -> blockEntity.getAutomationHandler()
        );
    }
}