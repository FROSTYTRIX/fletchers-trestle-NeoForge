package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.client.ClientArcheryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the local player's archery XP and skill ranks. */
public record ArcherySyncPacket(int xp, int draw, int crit, int aim) implements CustomPacketPayload {
    public static final Type<ArcherySyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "archery_sync"));

    public static final StreamCodec<FriendlyByteBuf, ArcherySyncPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArcherySyncPacket::xp,
            ByteBufCodecs.VAR_INT, ArcherySyncPacket::draw,
            ByteBufCodecs.VAR_INT, ArcherySyncPacket::crit,
            ByteBufCodecs.VAR_INT, ArcherySyncPacket::aim,
            ArcherySyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ArcherySyncPacket payload, final IPayloadContext context) {
        context.enqueueWork(() ->
                ClientArcheryData.set(payload.xp(), payload.draw(), payload.crit(), payload.aim()));
    }
}
