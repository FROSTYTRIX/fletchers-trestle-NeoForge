package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Guarding eagles defend their owner: when a mob hurts a player, every eagle of
 * theirs nearby that's on guard and free stoops on the attacker. Other players
 * and the owner's own pets are never targeted.
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public final class EagleGuardEvents {
    private EagleGuardEvents() {
    }

    private static final double GUARD_RANGE = 32.0;

    @SubscribeEvent
    public static void onOwnerHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player owner) || owner.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == owner) return;
        if (attacker instanceof Player) return;
        if (attacker instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) return;

        for (EagleEntity eagle : owner.level().getEntitiesOfClass(EagleEntity.class,
                owner.getBoundingBox().inflate(GUARD_RANGE),
                e -> e.isOwnedBy(owner) && e.isGuarding() && !e.isOrderedToSit()
                        && e.getEagleState() == EagleEntity.STATE_IDLE)) {
            eagle.setHuntTarget(attacker);
        }
    }
}
