package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.menu.FletchingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TuningPacket(float quality) implements CustomPacketPayload {
    public static final Type<TuningPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "tuning_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, TuningPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TuningPacket::quality,
            TuningPacket::new
    );

    public static void handle(TuningPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            if (player.containerMenu instanceof FletchingMenu fletchingMenu) {
                // Set the score from the minigame. It comes from the client, so keep it
                // inside the range the minigame can actually produce: a forged 1000%
                // would otherwise make a bow that draws instantly.
                float floor = net.frostytrix.fletcherstrestle.config.FletcherConfig.MINIGAME_MIN_SCORE.get().floatValue();
                float quality = Float.isNaN(message.quality()) ? floor : message.quality();
                fletchingMenu.customTuning = net.minecraft.util.Mth.clamp(quality, floor, 1.0f);

                // Force the menu to recalculate the result slot to apply the new tuning.
                fletchingMenu.slotsChanged(fletchingMenu.craftSlots);
            }
        });
    }
}