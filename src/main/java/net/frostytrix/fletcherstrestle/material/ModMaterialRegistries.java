package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The six datapack registries that hold material definitions. Each loads
 * JSON entries from {@code data/<datapack_namespace>/fletcherstrestle/<path>/<id>.json}
 * where {@code <path>} matches the registry-key path below
 * ({@code bow_limb}, {@code arrow_head}, …).
 *
 * <p>All six are <b>synced to clients</b>: the arrow renderer needs the
 * texture overrides, JEI categories need to enumerate entries, and the
 * tooltip code reads stats — all of those run client-side. The implication
 * is that any modpack adding a material def must ship the datapack on
 * client as well as server, which is the right default for this kind of
 * data and matches how recipes / loot tables already work.</p>
 *
 * <p>Registries are wired up in
 * {@link #onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry)},
 * which fires on the mod-event bus during init. Call
 * {@link #register(IEventBus)} from the mod constructor to hook the
 * listener.</p>
 *
 * <p>Phase B scope: declare the keys + listener. Lookup helpers
 * ({@code MaterialResolver}) and consumers land in Phase D; built-in
 * material JSONs land in Phase C.</p>
 */
public final class ModMaterialRegistries {
    private ModMaterialRegistries() {}

    public static final ResourceKey<Registry<BowLimbDef>> BOW_LIMB =
            ResourceKey.createRegistryKey(rl("bow_limb"));
    public static final ResourceKey<Registry<BowRiserDef>> BOW_RISER =
            ResourceKey.createRegistryKey(rl("bow_riser"));
    public static final ResourceKey<Registry<BowStringDef>> BOW_STRING =
            ResourceKey.createRegistryKey(rl("bow_string"));
    public static final ResourceKey<Registry<ArrowHeadDef>> ARROW_HEAD =
            ResourceKey.createRegistryKey(rl("arrow_head"));
    public static final ResourceKey<Registry<ArrowShaftDef>> ARROW_SHAFT =
            ResourceKey.createRegistryKey(rl("arrow_shaft"));
    public static final ResourceKey<Registry<ArrowFletchingDef>> ARROW_FLETCHING =
            ResourceKey.createRegistryKey(rl("arrow_fletching"));

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, path);
    }

    /** Hooks the {@link DataPackRegistryEvent.NewRegistry} listener. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModMaterialRegistries::onNewDataPackRegistry);
    }

    /**
     * Registers each of the six datapack registries. We pass the same codec
     * as both data-codec and network-codec — clients receive the full def
     * on connect, so renderer + JEI + tooltip code can read it.
     */
    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BOW_LIMB,        BowLimbDef.CODEC,        BowLimbDef.CODEC);
        event.dataPackRegistry(BOW_RISER,       BowRiserDef.CODEC,       BowRiserDef.CODEC);
        event.dataPackRegistry(BOW_STRING,      BowStringDef.CODEC,      BowStringDef.CODEC);
        event.dataPackRegistry(ARROW_HEAD,      ArrowHeadDef.CODEC,      ArrowHeadDef.CODEC);
        event.dataPackRegistry(ARROW_SHAFT,     ArrowShaftDef.CODEC,     ArrowShaftDef.CODEC);
        event.dataPackRegistry(ARROW_FLETCHING, ArrowFletchingDef.CODEC, ArrowFletchingDef.CODEC);
    }
}
