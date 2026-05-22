package net.frostytrix.fletcherstrestle.mixin.client;

import net.frostytrix.fletcherstrestle.client.ClientState;
import net.frostytrix.fletcherstrestle.network.MountSyncPayload;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Target the horse specifically so it can't override us!
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

            // 1. Convert A/D keys into a steering wheel!
            // passenger.xxa tracks your A/D keys (usually -1.0 to 1.0)
            float turnSpeed = 5.0F; // Tweak this number to make the horse turn faster or slower

            // Subtracting the input turns the horse mathematically left or right
            this.fletcherstrestle$storedYRot -= passenger.xxa * turnSpeed;

            // Keep the body aligned with the new direction
            this.fletcherstrestle$storedBodyRot = this.fletcherstrestle$storedYRot;

            // 2. Re-apply the newly steered rotation back to the horse
            mount.setYRot(this.fletcherstrestle$storedYRot);
            mount.yBodyRot = this.fletcherstrestle$storedBodyRot;
            mount.yHeadRot = this.fletcherstrestle$storedBodyRot;
            mount.yRotO = this.fletcherstrestle$storedYRot;

            // 3. SEND THE DATA TO THE SERVER!
            // TODO(port-26.1): PacketDistributor.sendToServer was removed from the
            // client-to-server side; sending a custom payload upstream now goes
            // through the connection (Minecraft.getInstance().getConnection().send(...))
            // or the new ClientPacketDistributor helper. Until that's wired, mount
            // steering is client-visual only — the server-side rotation sync is
            // dropped, so other players will still see the old facing.
            // PacketDistributor.sendToServer(new MountSyncPayload(mount.getId(), this.fletcherstrestle$storedYRot));
        }
    }
}