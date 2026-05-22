package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
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

    // 26.1 model-dispatch components — mirror the strings inside BowAssembly /
    // ArrowAssembly onto top-level String components so the new ItemModel
    // JSON system can drive composite layer selection via
    // `minecraft:select` + `minecraft:component`. The select codec compares
    // the whole component value, so we need primitive-typed dispatch
    // components (a record like BowAssembly doesn't decompose cleanly).
    // The recipe writes these in tandem with BowAssembly / ArrowAssembly —
    // the assembly stays as the gameplay source of truth.
    public static final Supplier<DataComponentType<String>> LIMB_MATERIAL =
            DATA_COMPONENT_TYPES.register("limb_material", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());
    public static final Supplier<DataComponentType<String>> RISER_MATERIAL =
            DATA_COMPONENT_TYPES.register("riser_material", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());
    public static final Supplier<DataComponentType<String>> STRING_MATERIAL =
            DATA_COMPONENT_TYPES.register("string_material", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static final Supplier<DataComponentType<String>> HEAD_MATERIAL =
            DATA_COMPONENT_TYPES.register("head_material", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());
    public static final Supplier<DataComponentType<String>> SHAFT_MATERIAL =
            DATA_COMPONENT_TYPES.register("shaft_material", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());
    public static final Supplier<DataComponentType<String>> FLETCHING_MATERIAL =
            DATA_COMPONENT_TYPES.register("fletching_material", () ->
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    // Stored on the Eagle Whistle item — UUID of the specific eagle the
    // whistle is bound to (or absent if it targets all owned eagles in range).
    public static final Supplier<DataComponentType<UUID>> BOUND_EAGLE =
            DATA_COMPONENT_TYPES.register("bound_eagle", () ->
                    DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build());

    public static void register(IEventBus bus) {DATA_COMPONENT_TYPES.register(bus);}
}