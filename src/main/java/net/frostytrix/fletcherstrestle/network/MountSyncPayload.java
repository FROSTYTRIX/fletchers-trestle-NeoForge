package net.frostytrix.fletcherstrestle.network;

import io.netty.buffer.ByteBuf;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record MountSyncPayload(int entityId, float yRot) implements CustomPacketPayload {

    // 1. The Unique ID for this packet
    public static final Type<MountSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "mount_sync"));

    // 2. The Codec that translates the data into bytes for the network
    public static final StreamCodec<ByteBuf, MountSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MountSyncPayload::entityId,
            ByteBufCodecs.FLOAT, MountSyncPayload::yRot,
            MountSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}