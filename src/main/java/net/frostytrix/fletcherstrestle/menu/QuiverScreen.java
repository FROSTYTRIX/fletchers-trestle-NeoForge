package net.frostytrix.fletcherstrestle.menu;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// TODO(port-26.1): Screen API rewrite — see FletchingScreen for details.
public class QuiverScreen extends AbstractContainerScreen<QuiverMenu> {
    public QuiverScreen(QuiverMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
