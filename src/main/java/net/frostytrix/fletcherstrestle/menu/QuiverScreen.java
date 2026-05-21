package net.frostytrix.fletcherstrestle.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class QuiverScreen extends AbstractContainerScreen<QuiverMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    public QuiverScreen(QuiverMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        int maxSlots = menu.slots.size() - 36;
        int rows = (int) Math.ceil((double) maxSlots / 9.0);

        this.imageWidth = 176;
        this.imageHeight = 114 + rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int maxSlots = this.menu.slots.size() - 36;
        int rows = (int) Math.ceil((double) maxSlots / 9.0);

        // --- LAYER 1: THE BASE ---
        // 1a. Splice the Top Half
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, rows * 18 + 17);

        // 1b. Splice the Bottom Half (Player Inventory)
        guiGraphics.blit(TEXTURE, x, y + rows * 18 + 17, 0, 126, this.imageWidth, 96);

        // --- LAYER 2: THE ERASER ---
        // Wipe the empty space BEFORE drawing the slots!
        int emptySlots = (rows * 9) - maxSlots;
        if (emptySlots > 0) {
            int lastRowY = y + 17 + ((rows - 1) * 18);
            // Wipe the entire 9-slot area for the last row
            guiGraphics.fill(x + 7, lastRowY, x + 7 + 162, lastRowY + 18, 0xFFC6C6C6);
        }

        // --- LAYER 3: THE SLOTS ---
        // Now that the canvas is clean, stamp the individual slot backgrounds
        for (int i = 0; i < maxSlots; i++) {
            Slot slot = this.menu.slots.get(i);
            // Stamp the slot texture exactly where the slot logic tells it to be
            guiGraphics.blitSprite(SLOT_SPRITE, x + slot.x - 1, y + slot.y - 1, 18, 18);
        }
    }
}