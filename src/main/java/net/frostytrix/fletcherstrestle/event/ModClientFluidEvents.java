package net.frostytrix.fletcherstrestle.event; // Ajuste selon ton package

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.fluid.ModFluidTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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

        // On enregistre nos extensions visuelles et on les lie à notre fluide (ModFluidTypes.LIQUID_POTION_TYPE)
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

            // La magie de la couleur dynamique est ici
            @Override
            public int getTintColor(FluidStack stack) {
                net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null && customData.contains("potion")) {
                    String potionId = customData.copyTag().getString("potion");
                    var potionHolder = BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(potionId)).orElse(null);

                    if (potionHolder != null) {
                        PotionContents contents = new PotionContents(potionHolder);
                        return contents.getColor() | 0xFF000000;
                    }
                }
                return 0xFF385DC6; // Bleu par défaut si aucune potion n'est détectée
            }
        }, ModFluidTypes.LIQUID_POTION_TYPE.get());

    }
}