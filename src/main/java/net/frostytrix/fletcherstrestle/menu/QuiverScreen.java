package net.frostytrix.fletcherstrestle.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

// 26.1 port of the quiver inventory screen. Uses vanilla's generic_54
// chest texture and crops it down to fit whatever row count the quiver
// menu currently exposes (3 rows for the leather quiver, 5 for the
// iron one). Last-row "extra" empty slots get painted grey so the
// player sees a clean canvas instead of empty slot frames.
//
// 26.1 changes:
//   * renderBg → extractBackground
//   * blit/fill/blitSprite gained a leading RenderPipeline arg
public class QuiverScreen extends AbstractContainerScreen<QuiverMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    public QuiverScreen(QuiverMenu menu, Inventory playerInventory, Component title) {
        // 26.1: imageWidth/imageHeight became final — pass them through the
        // 5-arg constructor instead of assigning after super().
        super(menu, playerInventory, title,
                176,
                114 + Math.max(1, (int) Math.ceil((menu.slots.size() - 36) / 9.0)) * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int maxSlots = this.menu.slots.size() - 36;
        int rows = Math.max(1, (int) Math.ceil(maxSlots / 9.0));

        // Top half — title bar + the N rows of quiver slots.
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x, y, 0F, 0F,
                this.imageWidth, rows * 18 + 17,
                256, 256);

        // Bottom half — the 4-row player inventory + hotbar. Pulled
        // from y=126 on generic_54 (matches the chest layout).
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x, y + rows * 18 + 17, 0F, 126F,
                this.imageWidth, 96,
                256, 256);

        // If the quiver isn't a multiple of 9, blank out the unused
        // tail of the last row so we don't show ghost slots.
        int emptySlots = (rows * 9) - maxSlots;
        if (emptySlots > 0) {
            int lastRowY = y + 17 + ((rows - 1) * 18);
            extractor.fill(x + 7, lastRowY, x + 7 + 162, lastRowY + 18, 0xFFC6C6C6);
        }

        // Stamp slot frames at the exact positions the menu's Slot list
        // asks for — keeps the visuals in sync with the server-side
        // layout no matter what the quiver capacity is.
        for (int i = 0; i < maxSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    x + slot.x - 1, y + slot.y - 1, 18, 18);
        }
    }
}
