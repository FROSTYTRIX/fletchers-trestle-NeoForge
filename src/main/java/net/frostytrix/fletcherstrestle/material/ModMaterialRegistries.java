package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The six datapack registries holding material definitions. Each loads JSON from
 * {@code data/<namespace>/fletcherstrestle/<path>/<id>.json} where {@code <path>} matches the
 * registry-key path below. All six are <b>synced to clients</b> (renderer, JEI and tooltips read
 * them client-side), so a modpack adding a material def must ship its datapack on the client too.
 */
public final class ModMaterialRegistries {
    private ModMaterialRegistries() {
    }

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

    /**
     * Hooks the {@link DataPackRegistryEvent.NewRegistry} listener.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModMaterialRegistries::onNewDataPackRegistry);
    }

    /**
     * Registers each of the six datapack registries. We pass the same codec
     * as both data-codec and network-codec — clients receive the full def
     * on connect, so renderer + JEI + tooltip code can read it.
     */
    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(BOW_LIMB, BowLimbDef.CODEC, BowLimbDef.CODEC);
        event.dataPackRegistry(BOW_RISER, BowRiserDef.CODEC, BowRiserDef.CODEC);
        event.dataPackRegistry(BOW_STRING, BowStringDef.CODEC, BowStringDef.CODEC);
        event.dataPackRegistry(ARROW_HEAD, ArrowHeadDef.CODEC, ArrowHeadDef.CODEC);
        event.dataPackRegistry(ARROW_SHAFT, ArrowShaftDef.CODEC, ArrowShaftDef.CODEC);
        event.dataPackRegistry(ARROW_FLETCHING, ArrowFletchingDef.CODEC, ArrowFletchingDef.CODEC);
    }
}
