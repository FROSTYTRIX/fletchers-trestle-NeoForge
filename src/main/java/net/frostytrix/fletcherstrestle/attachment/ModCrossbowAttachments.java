package net.frostytrix.fletcherstrestle.attachment;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The datapack registry that holds crossbow-attachment definitions, loaded
 * from {@code data/<ns>/fletcherstrestle/crossbow_attachment/<id>.json}.
 *
 * <p>Mirrors {@code material/ModMaterialRegistries}: the same codec is passed
 * as both data- and network-codec so the full def syncs to clients (JEI's
 * attachment category and the aim/zoom logic both run client-side).</p>
 *
 * <p>Call {@link #register(IEventBus)} from the mod constructor.</p>
 */
public final class ModCrossbowAttachments {
    private ModCrossbowAttachments() {
    }

    public static final ResourceKey<Registry<CrossbowAttachmentDef>> CROSSBOW_ATTACHMENT =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "crossbow_attachment"));

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCrossbowAttachments::onNewDataPackRegistry);
    }

    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CROSSBOW_ATTACHMENT, CrossbowAttachmentDef.CODEC, CrossbowAttachmentDef.CODEC);
    }
}
