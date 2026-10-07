package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.client.QuiverHudOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import static net.frostytrix.fletcherstrestle.FletcherTrestle.MOD_ID;

// Client-only subscriber so dedicated servers never touch client/graphics code.
@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FLETCHING_MENU.get(), FletchingScreen::new);
        event.register(ModMenuTypes.QUIVER_MENU.get(), QuiverScreen::new);
        event.register(ModMenuTypes.ARCHERY_TARGET_MENU.get(), ArcheryTargetScreen::new);
        event.register(ModMenuTypes.CROSSBOW_BENCH_MENU.get(), CrossbowBenchScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        // Quiver HUD, drawn above the hotbar.
        event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "quiver_hud"),
                QuiverHudOverlay::render);
    }

    @SubscribeEvent
    public static void registerConfigScreen(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        ModContainer modContainer = net.neoforged.fml.ModList.get().getModContainerById(FletcherTrestle.MOD_ID).orElseThrow();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
