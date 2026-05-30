package net.frostytrix.fletcherstrestle.progression;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Custom advancement criterion triggers (Phase 3). */
public final class ModCriteria {
    private ModCriteria() {
    }

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, FletcherTrestle.MOD_ID);

    public static final Supplier<HeadshotTrigger> HEADSHOT =
            TRIGGERS.register("headshot", HeadshotTrigger::new);
    public static final Supplier<ArcheryLevelTrigger> ARCHERY_LEVEL =
            TRIGGERS.register("archery_level", ArcheryLevelTrigger::new);
    public static final Supplier<AttachmentInstalledTrigger> ATTACHMENT_INSTALLED =
            TRIGGERS.register("attachment_installed", AttachmentInstalledTrigger::new);

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }
}
