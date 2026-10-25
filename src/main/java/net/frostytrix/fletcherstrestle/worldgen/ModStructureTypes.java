package net.frostytrix.fletcherstrestle.worldgen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, FletcherTrestle.MOD_ID);

    /** A jigsaw structure that only generates on level, dry ground: see {@link LevelJigsawStructure}. */
    public static final DeferredHolder<StructureType<?>, StructureType<LevelJigsawStructure>> LEVEL_JIGSAW =
            STRUCTURE_TYPES.register("level_jigsaw", () -> () -> LevelJigsawStructure.CODEC);

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
    }
}
