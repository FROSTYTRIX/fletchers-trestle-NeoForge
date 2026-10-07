package net.frostytrix.fletcherstrestle.garrison;

import com.google.common.collect.ImmutableSet;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Points of interest: how a golem finds the emplacements around it without scanning blocks. */
public final class ModPoiTypes {
    private ModPoiTypes() {
    }

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, FletcherTrestle.MOD_ID);

    // No tickets: golems share posts, and the block entity's crew claim keeps two
    // from manning the same one.
    public static final DeferredHolder<PoiType, PoiType> EMPLACEMENT = POI_TYPES.register("emplacement",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.EMPLACEMENT.get().getStateDefinition().getPossibleStates()), 0, 1));

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
    }
}
