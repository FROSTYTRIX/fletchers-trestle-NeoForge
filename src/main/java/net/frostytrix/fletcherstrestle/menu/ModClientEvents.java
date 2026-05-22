package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// Wires up the client-side screen factories for our custom MenuTypes.
// Without these, opening any of our menus on the server sends an
// OpenScreen packet to a client that doesn't know how to build a Screen
// for that MenuType — the result is "right-click does nothing".
//
// Stayed on the mod bus by default (RegisterMenuScreensEvent extends
// IModBusEvent). 26.1: EventBusSubscriber lost the `bus` member and
// `value` is Dist[].
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FLETCHING_MENU.get(), FletchingScreen::new);
        event.register(ModMenuTypes.QUIVER_MENU.get(), QuiverScreen::new);
        event.register(ModMenuTypes.ARCHERY_TARGET_MENU.get(), ArcheryTargetScreen::new);
    }
}
