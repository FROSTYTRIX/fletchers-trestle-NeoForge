package net.frostytrix.fletcherstrestle.menu;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// TODO(port-26.1): Same Screen API rewrite as FletchingScreen.
// 1.21.1 implementation drew the target canvas + shot list + scrollbar +
// info box. Stubbed for compile; shot tracking still happens server-side
// but the player has no UI for it.
public class ArcheryTargetScreen extends AbstractContainerScreen<ArcheryTargetMenu> {

    public ArcheryTargetScreen(ArcheryTargetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
