package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.ArrowTraits;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Gives the bow's arrow-borne traits to arrows that aren't the mod's own.
 *
 * <p>A modular bow stamps amphibious, punch and conductive onto whatever arrow it
 * fires. {@link ModularArrowEntity} handles them itself; these hooks apply the same
 * {@link ArrowTraits} to every other arrow, such as a vanilla arrow from a modular
 * bow or an armed mob's arrow.</p>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class ArrowTraitEvents {
    private ArrowTraitEvents() {
    }

    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractArrow arrow && !(arrow instanceof ModularArrowEntity)) {
            ArrowTraits.amphibiousTick(arrow);
        }
    }

    /** After the hit lands, while the arrow still carries its flight velocity. */
    @SubscribeEvent
    public static void onArrowHit(LivingDamageEvent.Post event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                && !(arrow instanceof ModularArrowEntity)
                && !arrow.level().isClientSide()) {
            ArrowTraits.punch(arrow, event.getEntity());
            ArrowTraits.conduct(arrow, event.getEntity());
        }
    }
}
