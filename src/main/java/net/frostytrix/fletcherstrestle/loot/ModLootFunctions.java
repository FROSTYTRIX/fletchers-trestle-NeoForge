package net.frostytrix.fletcherstrestle.loot;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Custom loot functions, usable from any loot table. */
public final class ModLootFunctions {
    private ModLootFunctions() {
    }

    public static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, FletcherTrestle.MOD_ID);

    public static final Supplier<LootItemFunctionType<RandomAssemblyFunction>> RANDOM_ASSEMBLY =
            FUNCTIONS.register("random_assembly", () -> new LootItemFunctionType<>(RandomAssemblyFunction.CODEC));

    public static void register(IEventBus bus) {
        FUNCTIONS.register(bus);
    }
}
