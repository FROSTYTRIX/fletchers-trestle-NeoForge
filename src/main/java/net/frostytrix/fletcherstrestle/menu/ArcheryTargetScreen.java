package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// 26.1 minimum-viable archery target screen. Renders the background panel
// so the player at least sees the target canvas / list panel layout.
//
// TODO(port-26.1): still missing from the 1.21.1 implementation:
//   * Per-shot dot overlay on the target canvas
//   * Scrollable shot list w/ thumb on the right panel
//   * Detail info box for the currently selected shot
//   * "Clear shots" button + ClearShotsPacket on the network side
public class ArcheryTargetScreen extends AbstractContainerScreen<ArcheryTargetMenu> {

    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(
            FletcherTrestle.MOD_ID, "textures/gui/archery_target_gui.png");

    private static final int GUI_W = 256;
    private static final int GUI_H = 196;

    public ArcheryTargetScreen(ArcheryTargetMenu menu, Inventory inv, Component title) {
        // 26.1: imageWidth/imageHeight became final — pass them via super().
        super(menu, inv, title, GUI_W, GUI_H);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        extractor.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                x, y, 0F, 0F,
                this.imageWidth, this.imageHeight,
                256, 256);
    }
}
