package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.menu.FletchingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TuningPacket(float quality) implements CustomPacketPayload {
    public static final Type<TuningPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "tuning_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static final StreamCodec<FriendlyByteBuf, TuningPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TuningPacket::quality,
            TuningPacket::new
    );

    public static void handle(TuningPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            if (player.containerMenu instanceof FletchingMenu fletchingMenu) {
                // Set the score from the minigame
                fletchingMenu.customTuning = message.quality(); // or message.score()

                // Force the menu to recalculate the result slot to apply the new tuning!
                fletchingMenu.slotsChanged(fletchingMenu.craftSlots);
            }
        });
    }
}