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

    // 1. Le fluide source (celui qui est statique dans le tank)
    public static final Supplier<FlowingFluid> LIQUID_POTION_SOURCE = FLUIDS.register("liquid_potion",
            () -> new BaseFlowingFluid.Source(ModFluids.LIQUID_POTION_PROPERTIES));

    // 2. Le fluide en mouvement (requis par NeoForge même si on ne s'en sert pas directement)
    public static final Supplier<FlowingFluid> LIQUID_POTION_FLOWING = FLUIDS.register("flowing_liquid_potion",
            () -> new BaseFlowingFluid.Flowing(ModFluids.LIQUID_POTION_PROPERTIES));

    // 3. Les propriétés qui lient le Type (ModFluidTypes) aux Fluides physiques
    public static final BaseFlowingFluid.Properties LIQUID_POTION_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.LIQUID_POTION_TYPE,
            LIQUID_POTION_SOURCE,
            LIQUID_POTION_FLOWING
    );
}