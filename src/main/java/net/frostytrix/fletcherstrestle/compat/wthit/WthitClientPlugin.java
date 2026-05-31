package net.frostytrix.fletcherstrestle.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaPlugin;
import net.frostytrix.fletcherstrestle.block.custom.SteamBoxBlock;
import net.frostytrix.fletcherstrestle.block.entity.SteamBoxBlockEntity;
import net.minecraft.nbt.CompoundTag;

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
        tooltip.addLine(SteamBoxBlockEntity.statusLine(
                data.getBoolean("ft_busy"),
                data.getBoolean("ft_heat"),
                data.getBoolean("ft_water"),
                data.getInt("ft_progress")));
    }
}
