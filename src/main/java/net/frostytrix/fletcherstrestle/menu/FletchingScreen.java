package net.frostytrix.fletcherstrestle.menu;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// TODO(port-26.1): Screen API completely rewritten.
//
// 26.1 changes:
//   - AbstractContainerScreen now uses the extractRenderState(GuiGraphicsExtractor,
//     mouseX, mouseY, partialTick) pattern instead of render(GuiGraphics, ...).
//   - mouseClicked(double, double, int) became mouseClicked(MouseButtonEvent, boolean).
//   - GuiGraphics is replaced/extended by GuiGraphicsExtractor.
//   - blit() and renderItem() signatures all changed.
//   - renderBackground / renderTooltip moved or were renamed.
//   - PacketDistributor.sendToServer moved (its package or class changed).
//
// The 1.21.1 implementation had a fully custom UI: tab switching between
// bow/arrow tabs, a tuning minigame (closing sweet-spot bar), draw-time
// calculation. Stubbed for compile — the player will see the vanilla
// AbstractContainerScreen background, no custom widgets.
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
