package net.frostytrix.fletcherstrestle.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaPlugin;
import net.frostytrix.fletcherstrestle.block.custom.SteamBoxBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * WTHIT client-side plugin: renders the Steam Box's status line from the data
 * sent by {@link WthitCommonPlugin}. Referenced from {@code waila_plugins.json}.
 */
public class WthitClientPlugin implements IWailaPlugin, IBlockComponentProvider {

    @Override
    public void register(IRegistrar registrar) {
        registrar.body(this, SteamBoxBlock.class);
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getData().raw();
        if (!data.contains("ft_busy")) return;
        boolean busy = data.getBoolean("ft_busy");
        boolean heat = data.getBoolean("ft_heat");
        if (busy && heat) {
            tooltip.addLine(Component.translatable("fletcherstrestle.tooltip.steam_box.steaming", data.getInt("ft_progress")));
        } else if (busy) {
            tooltip.addLine(Component.translatable("fletcherstrestle.tooltip.steam_box.no_heat"));
        } else {
            tooltip.addLine(Component.translatable("fletcherstrestle.tooltip.steam_box.idle"));
        }
    }
}
