package net.frostytrix.fletcherstrestle.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.SteamBoxBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Adds the Steam Box's heat/steaming status to TheOneProbe's tooltip. */
public class SteamBoxProbeProvider implements IProbeInfoProvider {

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steam_box");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level world, BlockState blockState, IProbeHitData data) {
        if (world.getBlockEntity(data.getPos()) instanceof SteamBoxBlockEntity box) {
            boolean heat = SteamBoxBlockEntity.hasHeatBelow(world, data.getPos());
            if (box.hasCookingItems()) {
                if (heat) {
                    info.text(Component.translatable("fletcherstrestle.tooltip.steam_box.steaming", box.getDisplayProgress()));
                } else {
                    info.text(Component.translatable("fletcherstrestle.tooltip.steam_box.no_heat"));
                }
            } else {
                info.text(Component.translatable("fletcherstrestle.tooltip.steam_box.idle"));
            }
        }
    }
}
