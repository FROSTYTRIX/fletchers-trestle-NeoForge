package net.frostytrix.fletcherstrestle.mixin.client;

import net.frostytrix.fletcherstrestle.client.ClientState;
import net.minecraft.client.Camera;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Entity entity;

    @Unique
    private double fletcherstrestle$lastY;

    @Unique
    private long fletcherstrestle$lastRenderTime = 0;

    @ModifyVariable(method = "setPosition(DDD)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double smoothCameraY(double y) {

        // 1. Calculate Delta Time (Time since last frame in seconds)
        long currentTime = Util.getMillis();
        if (this.fletcherstrestle$lastRenderTime == 0) {
            this.fletcherstrestle$lastRenderTime = currentTime;
        }
        double deltaTime = (currentTime - this.fletcherstrestle$lastRenderTime) / 1000.0;
        this.fletcherstrestle$lastRenderTime = currentTime;

        // If we are riding a horse and Free-Looking
        if (this.entity != null && this.entity.getVehicle() instanceof AbstractHorse && ClientState.isFreeLooking) {

            // 2. Framerate-Independent Exponential Decay
            // Tweak this decay value!
            // Higher (e.g., 20.0) = snappier/tighter to the horse.
            // Lower (e.g., 5.0) = looser/more cinematic glide.
            double decay = 12.0;

            // Apply the exponential decay formula
            double smoothedY = y + (this.fletcherstrestle$lastY - y) * Math.exp(-decay * deltaTime);

            this.fletcherstrestle$lastY = smoothedY;
            return smoothedY;

        } else {
            this.fletcherstrestle$lastY = y;
            return y;
        }
    }
}