package net.frostytrix.fletcherstrestle.worldgen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, FletcherTrestle.MOD_ID);

    public static final DeferredHolder<Feature<?>, EagleNestFeature> EAGLE_NEST =
            FEATURES.register("eagle_nest",
                    () -> new EagleNestFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
