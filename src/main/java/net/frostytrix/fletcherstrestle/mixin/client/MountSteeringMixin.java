package net.frostytrix.fletcherstrestle.mixin.client;

import net.frostytrix.fletcherstrestle.client.ClientState;
import net.frostytrix.fletcherstrestle.network.MountSyncPayload;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Targets the horse directly so its own rotation logic can't override ours.
@Mixin(AbstractHorse.class)
public class MountSteeringMixin {

    @Unique
    private float fletcherstrestle$storedYRot;
    @Unique
    private float fletcherstrestle$storedBodyRot;

    @Inject(method = "tickRidden", at = @At("HEAD"))
    private void captureRotation(Player passenger, Vec3 travelVector, CallbackInfo ci) {
        AbstractHorse mount = (AbstractHorse) (Object) this;

        if (mount.level().isClientSide() && ClientState.isFreeLooking) {
            this.fletcherstrestle$storedYRot = mount.getYRot();
            this.fletcherstrestle$storedBodyRot = mount.yBodyRot;
        }
    }

    @Inject(method = "tickRidden", at = @At("TAIL"))
    private void restoreRotation(Player passenger, Vec3 travelVector, CallbackInfo ci) {
        AbstractHorse mount = (AbstractHorse) (Object) this;

        if (mount.level().isClientSide() && ClientState.isFreeLooking) {
            // Steer with A/D (passenger.xxa, -1..1).
            float turnSpeed = 5.0F;
            this.fletcherstrestle$storedYRot -= passenger.xxa * turnSpeed;
            this.fletcherstrestle$storedBodyRot = this.fletcherstrestle$storedYRot;

            mount.setYRot(this.fletcherstrestle$storedYRot);
            mount.yBodyRot = this.fletcherstrestle$storedBodyRot;
            mount.yHeadRot = this.fletcherstrestle$storedBodyRot;
            mount.yRotO = this.fletcherstrestle$storedYRot;

            // Sync the steered rotation to the server.
            PacketDistributor.sendToServer(new MountSyncPayload(mount.getId(), this.fletcherstrestle$storedYRot));
        }
    }
}