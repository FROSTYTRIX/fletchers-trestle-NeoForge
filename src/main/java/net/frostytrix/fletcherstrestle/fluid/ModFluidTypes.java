package net.frostytrix.fletcherstrestle.fluid;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, FletcherTrestle.MOD_ID);

    public static final Supplier<FluidType> LIQUID_POTION_TYPE = FLUID_TYPES.register("liquid_potion",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.fletcherstrestle.liquid_potion") // Le nom pour JEI
                    .density(1000)
                    .viscosity(1000)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
}