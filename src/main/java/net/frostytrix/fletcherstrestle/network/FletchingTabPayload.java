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

public record FletchingTabPayload(int tabId) implements CustomPacketPayload {

    public static final Type<FletchingTabPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "fletching_tab"));

    public static final StreamCodec<FriendlyByteBuf, FletchingTabPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, FletchingTabPayload::tabId,
            FletchingTabPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // This handles what happens when the Server receives the click!
    public static void handleData(final FletchingTabPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            // If the player has the Fletching Menu open, change its tab and force a recipe check!
            if (player.containerMenu instanceof FletchingMenu menu) {
                menu.activeTab = payload.tabId();
                menu.slotsChanged(menu.craftSlots); // Re-evaluate recipes immediately
            }
        });
    }
}