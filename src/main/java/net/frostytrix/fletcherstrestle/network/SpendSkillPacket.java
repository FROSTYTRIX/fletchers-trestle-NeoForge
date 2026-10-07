package net.frostytrix.fletcherstrestle.network;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.progression.ArcheryProgression;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: spend a point in a branch, or buy that branch's capstone. */
public record SpendSkillPacket(int branch, boolean capstone) implements CustomPacketPayload {
    public static final Type<SpendSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "spend_skill"));

    public static final StreamCodec<FriendlyByteBuf, SpendSkillPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpendSkillPacket::branch,
            ByteBufCodecs.BOOL, SpendSkillPacket::capstone,
            SpendSkillPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SpendSkillPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ArcherySkill[] skills = ArcherySkill.values();
                if (payload.branch() >= 0 && payload.branch() < skills.length) {
                    if (payload.capstone()) {
                        ArcheryProgression.trySpendCapstone(player, skills[payload.branch()]);
                    } else {
                        ArcheryProgression.trySpend(player, skills[payload.branch()]);
                    }
                }
            }
        });
    }
}
