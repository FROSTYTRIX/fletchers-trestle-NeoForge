package net.frostytrix.fletcherstrestle.effect;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class EffectEvents {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
    }
}