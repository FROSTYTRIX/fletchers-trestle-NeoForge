package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.ShotRecord;
import net.frostytrix.fletcherstrestle.menu.ArcheryTargetMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record TargetSyncPacket(int containerId, List<ShotRecord> shots) implements CustomPacketPayload {
    public static final Type<TargetSyncPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "target_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final StreamCodec<FriendlyByteBuf, ShotRecord> SHOT_CODEC = StreamCodec.of(
            (buf, shot) -> {
                buf.writeDouble(shot.x());
                buf.writeDouble(shot.y());
                buf.writeDouble(shot.z());
                buf.writeFloat(shot.u());
                buf.writeFloat(shot.v());
                buf.writeFloat(shot.estimatedDamage());
                buf.writeFloat(shot.speed());
                buf.writeLong(shot.timestamp());
            },
            buf -> new ShotRecord(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readLong()
            )
    );

    public static final StreamCodec<FriendlyByteBuf, TargetSyncPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeInt(pkt.containerId());
                buf.writeCollection(pkt.shots(), (b, shot) -> SHOT_CODEC.encode(b, shot));
            },
            buf -> new TargetSyncPacket(
                    buf.readInt(),
                    buf.readList(b -> SHOT_CODEC.decode(b))
            )
    );

    public void handle(Player player) {
        Minecraft.getInstance().execute(() -> {
            if (player.containerMenu instanceof ArcheryTargetMenu menu && menu.containerId == containerId) {
                menu.setShots(shots);
            }
        });
    }
}