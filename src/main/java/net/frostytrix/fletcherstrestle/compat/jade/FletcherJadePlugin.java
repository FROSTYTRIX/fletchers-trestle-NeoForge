package net.frostytrix.fletcherstrestle.compat.jade;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.custom.SteamBoxBlock;
import net.frostytrix.fletcherstrestle.block.entity.SteamBoxBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration. The water tank and inventory already show via Jade's
 * built-in capability providers; this adds the Steam Box's heat/steaming status.
 */
@WailaPlugin
public class FletcherJadePlugin implements IWailaPlugin {

    private static final ResourceLocation STEAM_BOX =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steam_box");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(SteamBoxProvider.INSTANCE, SteamBoxBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(SteamBoxProvider.INSTANCE, SteamBoxBlock.class);
    }

    enum SteamBoxProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof SteamBoxBlockEntity box) {
                data.putBoolean("ft_heat", SteamBoxBlockEntity.hasHeatBelow(accessor.getLevel(), accessor.getPosition()));
                data.putBoolean("ft_busy", box.hasCookingItems());
                data.putInt("ft_progress", box.getDisplayProgress());
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains("ft_busy")) return;
            boolean busy = data.getBoolean("ft_busy");
            boolean heat = data.getBoolean("ft_heat");
            if (busy && heat) {
                tooltip.add(Component.translatable("fletcherstrestle.tooltip.steam_box.steaming", data.getInt("ft_progress")));
            } else if (busy) {
                tooltip.add(Component.translatable("fletcherstrestle.tooltip.steam_box.no_heat"));
            } else {
                tooltip.add(Component.translatable("fletcherstrestle.tooltip.steam_box.idle"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return STEAM_BOX;
        }
    }
}
