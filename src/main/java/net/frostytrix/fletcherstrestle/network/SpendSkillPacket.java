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

/** Client -> server: spend one skill point in the given branch. */
public record SpendSkillPacket(int branch) implements CustomPacketPayload {
    public static final Type<SpendSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "spend_skill"));

    public static final StreamCodec<FriendlyByteBuf, SpendSkillPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpendSkillPacket::branch,
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
                    ArcheryProgression.trySpend(player, skills[payload.branch()]);
                }
            }
        });
    }
}
