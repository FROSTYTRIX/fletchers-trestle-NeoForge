package net.frostytrix.fletcherstrestle.compat.top;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * TheOneProbe integration entry point. Registered via IMC so none of TOP's
 * classes are touched unless the mod is actually present.
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class TheOneProbeCompat {

    @SubscribeEvent
    public static void onEnqueueIMC(InterModEnqueueEvent event) {
        if (!ModList.get().isLoaded("theoneprobe")) return;
        // GetTheOneProbe (which references TOP types) is only classloaded here,
        // behind the isLoaded guard.
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", GetTheOneProbe::new);
    }
}
