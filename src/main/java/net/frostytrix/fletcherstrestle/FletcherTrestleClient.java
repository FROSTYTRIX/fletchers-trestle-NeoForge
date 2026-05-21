package net.frostytrix.fletcherstrestle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// TODO(port-26.1):
//
//   - ItemProperties registration is gone in 26.1 (replaced by the new
//     ItemModel JSON system at assets/MODID/items/MODEL_NAME.json with
//     conditional model selectors). The 1.21.1 file registered "pull",
//     "pulling", "charged", and "firework" predicates for the modular
//     bow and crossbow. Those need to migrate to ItemModel JSONs.
//
//   - HeavyDummyRenderer / HeavyDummyModel registrations commented out
//     until the renderer/model classes are rewritten for the new
//     EntityModel<S extends EntityRenderState> shape.
@Mod(value = FletcherTrestle.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public class FletcherTrestleClient {

    public void ExampleModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public FletcherTrestleClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerLayers);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // TODO(port-26.1): re-register HeavyDummyRenderer when ported.
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // TODO(port-26.1): re-register HeavyDummyModel when ported.
    }
}
