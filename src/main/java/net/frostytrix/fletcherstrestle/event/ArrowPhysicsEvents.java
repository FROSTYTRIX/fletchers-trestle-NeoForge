package net.frostytrix.fletcherstrestle.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = "fletcherstrestle") // Defaults to the GAME bus
public class ArrowPhysicsEvents {

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {

        // Check if the entity joining the world is an arrow
        if (event.getEntity() instanceof AbstractArrow arrow) {

            // Find out who shot it
            Entity shooter = arrow.getOwner();

            // Check if the shooter exists and is actively riding a horse
            if (shooter != null && shooter.getVehicle() instanceof AbstractHorse horse) {

                // Get the horse's exact directional velocity (X, Y, Z momentum)
                Vec3 horseVelocity = horse.getDeltaMovement();

                // Only apply math if the horse is actually moving
                if (horseVelocity.lengthSqr() > 0.001) {

                    // Get the arrow's base velocity (what vanilla Minecraft calculated)
                    Vec3 arrowVelocity = arrow.getDeltaMovement();

                    // Add the horse's momentum to the arrow's.
                    // We multiply the horse's velocity slightly because Minecraft's internal friction
                    // makes raw velocity values feel a bit sluggish on projectiles.
                    double momentumMultiplier = 1.5;
                    Vec3 newVelocity = arrowVelocity.add(horseVelocity.scale(momentumMultiplier));

                    // Apply the new super-charged vector
                    arrow.setDeltaMovement(newVelocity);

                    // Force the game engine to recognize the sudden change in physics
                    arrow.hasImpulse = true;
                }
            }
        }
    }
}