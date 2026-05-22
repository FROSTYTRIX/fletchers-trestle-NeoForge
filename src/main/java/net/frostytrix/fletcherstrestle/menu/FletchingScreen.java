package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// 26.1 minimum-viable fletching table screen. The 1.21.1 implementation
// had a full custom UI — tab switching between bow/arrow recipes, a
// tuning minigame, draw-time readout — none of that has been ported yet.
// For now we at least render the background texture and the inventory
// label so the player can see slots and shift-click items in/out.
//
// 26.1 changes that bit us before:
//   - render(GuiGraphics, …) was replaced by extractRenderState /
//     extractBackground (GuiGraphicsExtractor, …).
//   - blit() now takes (RenderPipeline, Identifier, x, y, u, v, w, h,
//     texW, texH) on GuiGraphicsExtractor.
//
// TODO(port-26.1): restore tab buttons, tuning sweet-spot bar, recipe
// readout, and the assorted listeners that fed the bow/crossbow draw-time
// labels.
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Standard 256×256 GUI sheet; we blit the imageWidth×imageHeight
        // window from the top-left.
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight,
                256, 256);
    }
}
