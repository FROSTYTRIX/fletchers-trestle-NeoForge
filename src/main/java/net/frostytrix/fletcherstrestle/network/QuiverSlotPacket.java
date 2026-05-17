package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuiverSlotPacket(boolean cycleRight) implements CustomPacketPayload {
    public static final Type<QuiverSlotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "quiver_slot"));

    public static final StreamCodec<FriendlyByteBuf, QuiverSlotPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, QuiverSlotPacket::cycleRight,
            QuiverSlotPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(final QuiverSlotPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof ModularQuiverItem) {
                    // Read the max slots (default to 9 if the component is somehow missing)
                    int maxSlots = stack.getOrDefault(ModDataComponents.MAX_QUIVER_SLOTS.get(), 9);

                    int current = stack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);

                    // Dynamic modulo math to wrap around the specific size of this quiver tier!
                    int next = payload.cycleRight() ? (current + 1) % maxSlots : (current - 1 + maxSlots) % maxSlots;

                    stack.set(ModDataComponents.QUIVER_SELECTED_SLOT.get(), next);
                    break;
                }
            }
        });
    }
}