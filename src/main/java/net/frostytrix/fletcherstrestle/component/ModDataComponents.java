package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
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

    public static final Supplier<DataComponentType<ArrowAssembly>> ARROW_ASSEMBLY =
            DATA_COMPONENT_TYPES.register("arrow_assembly", () ->
                    DataComponentType.<ArrowAssembly>builder()
                            .persistent(ArrowAssembly.CODEC)
                            .networkSynchronized(ArrowAssembly.STREAM_CODEC)
                            .build()
            );

    public static final Supplier<DataComponentType<Integer>> QUIVER_SELECTED_SLOT =
            DATA_COMPONENT_TYPES.register("quiver_selected_slot",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    // Inside ModDataComponents.java
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAX_QUIVER_SLOTS =
            DATA_COMPONENT_TYPES.register("max_quiver_slots", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)// For saving to NBT/Disk
                            .networkSynchronized(ByteBufCodecs.VAR_INT) // For syncing from Server -> Client
                            .build());

    public static void register(IEventBus bus) {DATA_COMPONENT_TYPES.register(bus);}
}