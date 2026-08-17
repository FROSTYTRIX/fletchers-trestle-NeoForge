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
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FletcherTrestle.MOD_ID);

    public static final Supplier<DataComponentType<BowAssembly>> BOW_ASSEMBLY =
            DATA_COMPONENT_TYPES.register("bow_assembly", () ->
                    DataComponentType.<BowAssembly>builder()
                            .persistent(BowAssembly.CODEC)
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

    public static final Supplier<DataComponentType<GarlandColours>> GARLAND_COLOURS =
            DATA_COMPONENT_TYPES.register("garland_colours", () ->
                    DataComponentType.<GarlandColours>builder()
                            .persistent(GarlandColours.CODEC)
                            .networkSynchronized(GarlandColours.STREAM_CODEC)
                            .build()
            );

    /** The nail a garland has been anchored to, waiting for its second end. */
    public static final Supplier<DataComponentType<net.minecraft.core.GlobalPos>> GARLAND_ANCHOR =
            DATA_COMPONENT_TYPES.register("garland_anchor", () ->
                    DataComponentType.<net.minecraft.core.GlobalPos>builder()
                            .persistent(net.minecraft.core.GlobalPos.CODEC)
                            .networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC)
                            .build()
            );

    public static final Supplier<DataComponentType<Integer>> QUIVER_SELECTED_SLOT =
            DATA_COMPONENT_TYPES.register("quiver_selected_slot",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAX_QUIVER_SLOTS =
            DATA_COMPONENT_TYPES.register("max_quiver_slots", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    // Stored on the Eagle Whistle item: UUID of the specific eagle the
    // whistle is bound to (or absent if it targets all owned eagles in range).
    public static final Supplier<DataComponentType<UUID>> BOUND_EAGLE =
            DATA_COMPONENT_TYPES.register("bound_eagle", () ->
                    DataComponentType.<UUID>builder()
                            .persistent(UUIDUtil.CODEC)
                            .networkSynchronized(UUIDUtil.STREAM_CODEC)
                            .build());

    // Crossbow attachment: id of the installed attachment def, resolved against the
    // crossbow_attachment datapack registry at aim/fire time. One universal slot, so one id.
    public static final Supplier<DataComponentType<ResourceLocation>> CROSSBOW_ATTACHMENT =
            DATA_COMPONENT_TYPES.register("crossbow_attachment", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
                            .build());

    // Damage value the attachment item (e.g. the bayonet sword) had when it was
    // installed. Kept so the sword can be handed back at its original wear and
    // can never be repaired by re-installing. (We store the int rather than the
    // ItemStack itself: ItemStack isn't a legal data-component value.)
    public static final Supplier<DataComponentType<Integer>> CROSSBOW_ATTACHMENT_DAMAGE =
            DATA_COMPONENT_TYPES.register("crossbow_attachment_damage", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    public static void register(IEventBus bus) {
        DATA_COMPONENT_TYPES.register(bus);
    }
}