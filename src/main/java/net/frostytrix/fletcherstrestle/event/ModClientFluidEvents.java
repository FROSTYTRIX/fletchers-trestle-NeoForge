package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.fluid.ModFluidTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public class ModClientFluidEvents {

    @SubscribeEvent
    public static void onClientExtensions(RegisterClientExtensionsEvent event) {

        // Register client visual extensions for our liquid-potion fluid type.
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
            private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

            @Override
            public ResourceLocation getStillTexture() {
                return WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return WATER_FLOW;
            }

            // Dynamic colour: tint to the stored potion's colour.
            @Override
            public int getTintColor(FluidStack stack) {
                PotionContents contents = net.frostytrix.fletcherstrestle.fluid.PotionFluid.contents(stack);
                if (!contents.equals(PotionContents.EMPTY)) {
                    return contents.getColor() | 0xFF000000;
                }
                return 0xFF385DC6; // default blue when no potion is present
            }
        }, ModFluidTypes.LIQUID_POTION_TYPE.get());

    }
}