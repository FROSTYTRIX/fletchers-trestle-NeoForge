package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// Subset of the old ModClientEvents — for now just registers a no-op
// renderer for every modded entity so the client doesn't NPE when one
// shows up in the world. The real renderers (eagle model, modular arrow
// renderer, heavy dummy, etc.) are still pending the 26.1 renderer
// rewrite (deferred extract/submit pattern + new ArrowRenderer<T,S>
// shape).
//
// TODO(port-26.1): replace NoopRenderer wiring with real renderers.
// Keybinds, quiver scroll, color handlers, geometry loaders, and FOV
// modifier are still on the long-term TODO list.
// 26.1: EventBusSubscriber lost the `bus` member (mod-bus is the default)
// and `value` is now Dist[]. The "client only" gate is `value = Dist.CLIENT`.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public final class ModClientEvents {
    private ModClientEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Block entity renderers — same event covers them in 26.1.
        event.registerBlockEntityRenderer(
                net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities.SHAVING_HORSE_BE.get(),
                net.frostytrix.fletcherstrestle.block.entity.renderer.ShavingHorseRenderer::new);

        // Eagles tick on the server even without a renderer, but the client
        // entity dispatcher NPEs the first time one enters the camera frustum.
        event.registerEntityRenderer(ModEntities.EAGLE.get(), NoopRenderer::new);

        // Modular arrows get a real renderer so projectiles are visible in
        // flight. Texture is picked from ArrowAssembly.shaft each frame.
        event.registerEntityRenderer(
                ModEntities.MODULAR_ARROW.get(),
                net.frostytrix.fletcherstrestle.client.renderer.ModularArrowRenderer::new);

        if (ModEntities.HEAVY_DUMMY != null)
            event.registerEntityRenderer(ModEntities.HEAVY_DUMMY.get(), NoopRenderer::new);
    }

    // Layered HUD draw — sits the quiver overlay just above vanilla's
    // hotbar so the selection-ring + arrow icons appear in the same
    // visual space.
    @SubscribeEvent
    public static void onRegisterGuiLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerAbove(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "quiver_hud"),
                net.frostytrix.fletcherstrestle.client.QuiverHudOverlay.INSTANCE);
    }
}
