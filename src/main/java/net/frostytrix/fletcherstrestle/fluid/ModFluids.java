package net.frostytrix.fletcherstrestle.fluid;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, FletcherTrestle.MOD_ID);

    // Source fluid (the static one held in the tank).
    public static final Supplier<FlowingFluid> LIQUID_POTION_SOURCE = FLUIDS.register("liquid_potion",
            () -> new BaseFlowingFluid.Source(ModFluids.LIQUID_POTION_PROPERTIES));

    // Flowing fluid (required by NeoForge even though we never flow it).
    public static final Supplier<FlowingFluid> LIQUID_POTION_FLOWING = FLUIDS.register("flowing_liquid_potion",
            () -> new BaseFlowingFluid.Flowing(ModFluids.LIQUID_POTION_PROPERTIES));

    // Properties linking the fluid type (ModFluidTypes) to the physical fluids.
    public static final BaseFlowingFluid.Properties LIQUID_POTION_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.LIQUID_POTION_TYPE,
            LIQUID_POTION_SOURCE,
            LIQUID_POTION_FLOWING
    );
}