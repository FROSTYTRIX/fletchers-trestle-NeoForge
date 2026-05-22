package net.frostytrix.fletcherstrestle.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.network.QuiverSlotPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

// Client-side key bindings for the quiver cycle (arrow-key) controls and
// the mount steering / gallop-lock keys. Wraps quiver-modifier scroll
// state in ClientState + QuiverHudOverlay for the HUD animation.
//
// 26.1 changes:
//   * KeyMapping no longer accepts a `String` category — needs a
//     KeyMapping.Category record. Registered our own under the mod id.
//   * `ClientTickEvent.Post` lives under neoforge.client.event.
//   * PacketDistributor.sendToServer moved to ClientPacketDistributor.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public final class ClientKeybinds {
    private ClientKeybinds() {}

    // Own category record so our key bindings show up under a "Fletcher's
    // Trestle" header in the controls menu instead of stuffed under
    // GAMEPLAY. register() returns the same instance for the same id.
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "main"));

    public static final KeyMapping CYCLE_LEFT = new KeyMapping(
            "key.fletcherstrestle.quiver_left", GLFW.GLFW_KEY_LEFT, CATEGORY);
    public static final KeyMapping CYCLE_RIGHT = new KeyMapping(
            "key.fletcherstrestle.quiver_right", GLFW.GLFW_KEY_RIGHT, CATEGORY);
    public static final KeyMapping QUIVER_MODIFIER = new KeyMapping(
            "key.fletcherstrestle.quiver_modifier",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    public static final KeyMapping FREE_LOOK_KEY = new KeyMapping(
            "key.fletcherstrestle.free_look",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping GALLOP_LOCK_KEY = new KeyMapping(
            "key.fletcherstrestle.gallop_lock_key",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    // Mod-bus event for registering the bindings — runs once at client setup.
    @EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {}

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(CYCLE_LEFT);
            event.register(CYCLE_RIGHT);
            event.register(QUIVER_MODIFIER);
            event.register(FREE_LOOK_KEY);
            event.register(GALLOP_LOCK_KEY);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Mirror free-look key onto the global flag the mount mixin reads.
        ClientState.isFreeLooking = FREE_LOOK_KEY.isDown();

        // Snapshot the slide state for partial-tick interpolation in the HUD.
        QuiverHudOverlay.slideProgressO = QuiverHudOverlay.slideProgress;

        if (QuiverHudOverlay.displayTicks > 0) {
            QuiverHudOverlay.displayTicks--;
            if (QuiverHudOverlay.slideProgress < 10) QuiverHudOverlay.slideProgress++;
        } else if (QuiverHudOverlay.slideProgress > 0) {
            QuiverHudOverlay.slideProgress--;
        }

        // Arrow-key cycle bindings — drain queued clicks and ping the server.
        while (CYCLE_LEFT.consumeClick()) {
            ClientPacketDistributor.sendToServer(new QuiverSlotPacket(false));
            QuiverHudOverlay.displayTicks = 60;
        }
        while (CYCLE_RIGHT.consumeClick()) {
            ClientPacketDistributor.sendToServer(new QuiverSlotPacket(true));
            QuiverHudOverlay.displayTicks = 60;
        }
    }
}
