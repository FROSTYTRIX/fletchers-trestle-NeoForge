package net.frostytrix.fletcherstrestle.compat.wthit;

import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import net.frostytrix.fletcherstrestle.block.entity.SteamBoxBlockEntity;

/**
 * WTHIT server-side plugin: gathers the Steam Box's heat/steaming state and
 * ships it to the client. Referenced from {@code waila_plugins.json}.
 */
public class WthitCommonPlugin implements IWailaCommonPlugin, IDataProvider<SteamBoxBlockEntity> {

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(this, SteamBoxBlockEntity.class);
    }

    @Override
    public void appendData(IDataWriter data, IServerAccessor<SteamBoxBlockEntity> accessor, IPluginConfig config) {
        SteamBoxBlockEntity box = accessor.getTarget();
        data.raw().putBoolean("ft_heat", SteamBoxBlockEntity.hasHeatBelow(accessor.getLevel(), box.getBlockPos()));
        data.raw().putBoolean("ft_busy", box.hasCookingItems());
        data.raw().putBoolean("ft_water", box.hasWaterToSteam());
        data.raw().putInt("ft_progress", box.getDisplayProgress());
    }
}
