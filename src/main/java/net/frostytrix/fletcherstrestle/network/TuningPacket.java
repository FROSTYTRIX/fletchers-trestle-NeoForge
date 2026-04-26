package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.menu.FletchingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record TuningPacket(float quality) implements CustomPacketPayload {
    public static final Type<TuningPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "tuning_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, TuningPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TuningPacket::quality,
            TuningPacket::new
    );

    // This runs on the SERVER when the packet arrives
    public void handle(Player player) {
        if (player.containerMenu instanceof FletchingMenu menu) {
            menu.finalizeBow(this.quality);
        }
    }
}