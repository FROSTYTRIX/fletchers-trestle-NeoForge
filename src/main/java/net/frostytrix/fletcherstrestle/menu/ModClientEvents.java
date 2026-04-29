package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.client.QuiverHudOverlay;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// Notice the bus = MOD and value = CLIENT.
// This ensures servers don't crash trying to load graphics!
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FLETCHING_MENU.get(), FletchingScreen::new);
        event.register(ModMenuTypes.QUIVER_MENU.get(), QuiverScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        // This ensures the Quiver HUD renders perfectly on top of the screen
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "quiver_hud"),
                QuiverHudOverlay::render);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 1. The Vanilla Pulling Properties
            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
                if (entity == null) return 0.0F;
                return entity.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
            });
            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) -> {
                return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
            });

            // 2. Our Custom Material Properties!
            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "limb"), (stack, level, entity, seed) -> {
                var assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
                if (assembly == null) return 0.0F; // Default
                return switch (assembly.limbMaterial()) {
                    case "Oak" -> 0.1F;
                    case "Spruce" -> 0.2F;
                    case "Birch" -> 0.3F;
                    case "Jungle" -> 0.4F;
                    case "Acacia" -> 0.5F;
                    case "Dark Oak" -> 0.6F;
                    case "Mangrove" -> 0.7F;
                    case "Cherry" -> 0.8F;
                    case "Pale Oak" -> 0.9F;
                    case "Crimson" -> 1.0F;
                    case "Warped" -> 1.1F;
                    default -> 0.1F;
                };
            });

            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "riser"), (stack, level, entity, seed) -> {
                var assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
                if (assembly == null) return 0.0F;
                return switch (assembly.riserMaterial()) {
                    case "Wood" -> 0.1F;
                    case "Iron" -> 0.2F;
                    case "Copper" -> 0.3F;
                    default -> 0.1F;
                };
            });

            ItemProperties.register(ModItems.MODULAR_BOW.get(), ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "string"), (stack, level, entity, seed) -> {
                var assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
                if (assembly == null) return 0.0F;
                return switch (assembly.stringMaterial()) {
                    case "Spider" -> 0.1F;
                    case "Flax" -> 0.2F;
                    case "High Tension" -> 0.3F;
                    default -> 0.1F;
                };
            });

            ItemProperties.register(ModItems.MODULAR_ARROW.get(), ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_head"), (stack, level, entity, seed) -> {
                var assembly = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
                if (assembly == null) return 0.0F;
                return switch (assembly.head()) {
                    case "flint" -> 0.1F;
                    case "broadhead" -> 0.2F;
                    case "bodkin_point" -> 0.3F;
                    case "resonance_tip" -> 0.4F;
                    case "barbed_tip" -> 0.5F;
                    case "weighted_blunt" -> 0.6F;
                    default -> 0.1F;
                };
            });

            ItemProperties.register(ModItems.MODULAR_ARROW.get(), ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_shaft"), (stack, level, entity, seed) -> {
                var assembly = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
                if (assembly == null) return 0.0F;
                return switch (assembly.shaft()) {
                    case "oak" -> 0.1F;
                    case "spruce" -> 0.2F;
                    case "birch" -> 0.3F;
                    case "jungle" -> 0.4F;
                    case "acacia" -> 0.5F;
                    case "dark_oak" -> 0.6F;
                    case "mangrove" -> 0.7F;
                    case "cherry" -> 0.8F;
                    case "pale_oak" -> 0.9F;
                    case "crimson" -> 1.0F;
                    case "warped" -> 1.1F;
                    default -> 0.1F;
                };
            });

            ItemProperties.register(ModItems.MODULAR_ARROW.get(), ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_fletching"), (stack, level, entity, seed) -> {
                var assembly = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
                if (assembly == null) return 0.0F;
                return switch (assembly.fletching()) {
                    case "feather" -> 0.1F;
                    case "rigid" -> 0.2F;
                    case "trailing" -> 0.3F;
                    case "serrated" -> 0.4F;
                    case "bound" -> 0.5F;
                    case "vex" -> 0.6F;
                    default -> 0.1F;
                };
            });
        });
    }
}
