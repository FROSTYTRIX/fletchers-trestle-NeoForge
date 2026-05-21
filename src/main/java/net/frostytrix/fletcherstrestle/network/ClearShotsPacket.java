package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.ArcheryTargetBlockEntity;
import net.frostytrix.fletcherstrestle.menu.ArcheryTargetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;

public record ClearShotsPacket(BlockPos pos, int containerId) implements CustomPacketPayload {

    public static final Type<ClearShotsPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "clear_shots"));

    public static final StreamCodec<FriendlyByteBuf, ClearShotsPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> { buf.writeBlockPos(pkt.pos()); buf.writeInt(pkt.containerId()); },
            buf -> new ClearShotsPacket(buf.readBlockPos(), buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(Player player) {
        // 1. Check if the player currently has the ArcheryTargetMenu open and the IDs match
        if (player.containerMenu instanceof ArcheryTargetMenu menu && menu.containerId == this.containerId()) {

            // 2. Fetch the correct, server-validated BlockPos from the server menu
            BlockPos actualPos = menu.getTargetPos();

            // 3. Find the BlockEntity at the correct position and clear it
            if (player.level().getBlockEntity(actualPos) instanceof ArcheryTargetBlockEntity be) {
                be.clearShots();

                // 4. Also clear the server-side menu's internal list so it is synced
                menu.setShots(Collections.emptyList());
            }
        }
    }
}