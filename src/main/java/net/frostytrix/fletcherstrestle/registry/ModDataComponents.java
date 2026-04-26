package net.frostytrix.fletcherstrestle.registry;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModDataComponents {
    // 1. Use the standard DeferredRegister format
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FletcherTrestle.MOD_ID);

    // 2. Use the standard .register() and manually build the Component Type
    public static final Supplier<DataComponentType<BowAssembly>> BOW_ASSEMBLY =
            DATA_COMPONENT_TYPES.register("bow_assembly", () ->
                    DataComponentType.<BowAssembly>builder()
                            .persistent(BowAssembly.CODEC) // Note: If your specific mapping version complains here, change 'persistent' to 'codec'
                            .networkSynchronized(BowAssembly.STREAM_CODEC)
                            .build()
            );

    public static void register(IEventBus bus) {DATA_COMPONENT_TYPES.register(bus);}
}