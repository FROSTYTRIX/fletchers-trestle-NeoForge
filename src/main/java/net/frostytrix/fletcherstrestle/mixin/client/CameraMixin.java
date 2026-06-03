package net.frostytrix.fletcherstrestle.mixin.client;

import net.frostytrix.fletcherstrestle.client.ClientState;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Entity entity;

    @Unique
    private double fletcherstrestle$lastY;

    @Unique
    private long fletcherstrestle$lastRenderTime = 0;

    @ModifyVariable(method = "setPosition(DDD)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double smoothCameraY(double y) {

        // Delta time since last frame, in seconds.
        long currentTime = Util.getMillis();
        if (this.fletcherstrestle$lastRenderTime == 0) {
            this.fletcherstrestle$lastRenderTime = currentTime;
        }
        double deltaTime = (currentTime - this.fletcherstrestle$lastRenderTime) / 1000.0;
        this.fletcherstrestle$lastRenderTime = currentTime;

        if (this.entity != null && this.entity.getVehicle() instanceof AbstractHorse && ClientState.isFreeLooking) {
            // Framerate-independent exponential decay (higher = tighter to the horse).
            double decay = 12.0;
            double smoothedY = y + (this.fletcherstrestle$lastY - y) * Math.exp(-decay * deltaTime);

            this.fletcherstrestle$lastY = smoothedY;
            return smoothedY;

        } else {
            this.fletcherstrestle$lastY = y;
            return y;
        }
    }
}