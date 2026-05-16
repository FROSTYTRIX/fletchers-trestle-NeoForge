package net.frostytrix.fletcherstrestle.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.network.QuiverSlotPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public class ClientKeybinds {
    public static final KeyMapping CYCLE_LEFT = new KeyMapping("key.fletcherstrestle.quiver_left", GLFW.GLFW_KEY_LEFT, "key.categories.fletcherstrestle");
    public static final KeyMapping CYCLE_RIGHT = new KeyMapping("key.fletcherstrestle.quiver_right", GLFW.GLFW_KEY_RIGHT, "key.categories.fletcherstrestle");
    public static final KeyMapping QUIVER_MODIFIER = new KeyMapping("key.fletcherstrestle.quiver_modifier", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.fletcherstrestle");

    public static final KeyMapping FREE_LOOK_KEY = new KeyMapping("key.fletcherstrestle.free_look", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.fletcherstrestle");
    public static final KeyMapping GALLOP_LOCK_KEY = new KeyMapping("key.fletcherstrestle.gallop_lock_key", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.fletcherstrestle");


    // Note: The bus = Bus.MOD is required for Registration events, but Bus.GAME is used for Tick events.
    @EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(CYCLE_LEFT);
            event.register(CYCLE_RIGHT);
            event.register(FREE_LOOK_KEY);
            event.register(GALLOP_LOCK_KEY);
        }
    }



    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientState.isFreeLooking = ClientKeybinds.FREE_LOOK_KEY.isDown();

        // 1. Save the old animation state for the smooth lerp
        QuiverHudOverlay.slideProgressO = QuiverHudOverlay.slideProgress;

        // 2. Process the timer and animation states
        if (QuiverHudOverlay.displayTicks > 0) {
            QuiverHudOverlay.displayTicks--;
            // If the timer is active, slide IN (up to a max of 10)
            if (QuiverHudOverlay.slideProgress < 10) {
                QuiverHudOverlay.slideProgress++;
            }
        } else {
            // If the timer is dead, slide OUT (down to 0)
            if (QuiverHudOverlay.slideProgress > 0) {
                QuiverHudOverlay.slideProgress--;
            }
        }

        // 3. Handle Keybinds
        while (CYCLE_LEFT.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new QuiverSlotPacket(false));
            // This ONLY resets the timer now! The animation won't replay if it's already open.
            QuiverHudOverlay.displayTicks = 60;
        }

        while (CYCLE_RIGHT.consumeClick()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new QuiverSlotPacket(true));
            QuiverHudOverlay.displayTicks = 60;
        }
    }


}