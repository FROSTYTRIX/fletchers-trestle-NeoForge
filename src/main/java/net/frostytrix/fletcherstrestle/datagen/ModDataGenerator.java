package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

// The bus = EventBusSubscriber.Bus.MOD is crucial here so NeoForge finds the event
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // 1. The Datapack Provider (Generates the Photosynthesis JSON)
        CompletableFuture<HolderLookup.Provider> datapackProvider = generator.addProvider(
                event.includeServer(),
                new ModDatapackProvider(packOutput, lookupProvider)
        ).getRegistryProvider();

        // 2. The Tags Provider (Adds Photosynthesis to the Enchanting Table)
        generator.addProvider(
                event.includeServer(),
                new ModEnchantmentTagsProvider(packOutput, datapackProvider, existingFileHelper)
        );
    }
}