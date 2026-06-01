package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
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

    // Stored on the Eagle Whistle item — UUID of the specific eagle the
    // whistle is bound to (or absent if it targets all owned eagles in range).
    public static final Supplier<DataComponentType<UUID>> BOUND_EAGLE =
            DATA_COMPONENT_TYPES.register("bound_eagle", () ->
                    DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build());

    // Crossbow attachment (Phase 1) — id of the installed attachment def,
    // resolved against the crossbow_attachment datapack registry at aim/fire
    // time. One universal slot, so a single id is enough.
    public static final Supplier<DataComponentType<ResourceLocation>> CROSSBOW_ATTACHMENT =
            DATA_COMPONENT_TYPES.register("crossbow_attachment", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build());

    // The actual item that was installed as the attachment (e.g. the bayonet
    // sword), kept so it can be handed back intact on removal — preserving its
    // own durability/enchantments instead of returning a fresh copy.
    public static final Supplier<DataComponentType<net.minecraft.world.item.ItemStack>> CROSSBOW_ATTACHMENT_ITEM =
            DATA_COMPONENT_TYPES.register("crossbow_attachment_item", () ->
                    DataComponentType.<net.minecraft.world.item.ItemStack>builder()
                            .persistent(net.minecraft.world.item.ItemStack.CODEC)
                            .networkSynchronized(net.minecraft.world.item.ItemStack.STREAM_CODEC)
                            .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENT_TYPES.register(bus);
    }
}