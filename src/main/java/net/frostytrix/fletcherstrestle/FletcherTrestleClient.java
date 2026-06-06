package net.frostytrix.fletcherstrestle;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.client.BlackHoleRenderer;
import net.frostytrix.fletcherstrestle.entity.client.HeavyDummyModel;
import net.frostytrix.fletcherstrestle.entity.client.HeavyDummyRenderer;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = FletcherTrestle.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public class FletcherTrestleClient {
    public void ExampleModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public FletcherTrestleClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerLayers);
    }


    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HEAVY_DUMMY.get(), HeavyDummyRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HeavyDummyModel.LAYER_LOCATION, HeavyDummyModel::createBodyLayer);
    }

    @SubscribeEvent
    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            // "pull" animation driven by our per-limb draw time, not vanilla's 20 ticks.
            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) {
                            return 0.0F;
                        } else {
                            ModularBowItem bow = (ModularBowItem) stack.getItem();
                            return entity.getUseItem() != stack ? 0.0F :
                                    (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / bow.getDrawTime(stack);
                        }
                    });

            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> {
                        return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
                    });

            ItemProperties.register(ModItems.MODULAR_CROSSBOW.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
                if (entity == null || CrossbowItem.isCharged(stack)) {
                    return 0.0F;
                }
                // Divide by the crossbow's OWN charge duration (which a magazine
                // stretches), so the pull animation matches the real draw time.
                float required = stack.getItem() instanceof ModularCrossbowItem crossbow
                        ? Math.max(1, crossbow.requiredChargeTicks(stack, entity))
                        : CrossbowItem.getChargeDuration(stack, entity);
                return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / required;
            });

            ItemProperties.register(ModItems.MODULAR_CROSSBOW.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> {
                return entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F;
            });

            ItemProperties.register(ModItems.MODULAR_CROSSBOW.get(), ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) -> {
                return entity != null && CrossbowItem.isCharged(stack) ? 1.0F : 0.0F;
            });

            ItemProperties.register(ModItems.MODULAR_CROSSBOW.get(), ResourceLocation.withDefaultNamespace("firework"), (stack, level, entity, seed) -> {
                if (entity != null && CrossbowItem.isCharged(stack)) {
                    ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
                    if (projectiles != null && projectiles.contains(Items.FIREWORK_ROCKET)) {
                        return 1.0F;
                    }
                }
                return 0.0F;
            });

        });


    }
}
