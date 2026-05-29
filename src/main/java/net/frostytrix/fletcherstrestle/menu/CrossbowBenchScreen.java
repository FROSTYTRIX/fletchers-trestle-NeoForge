package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CrossbowBenchScreen extends AbstractContainerScreen<CrossbowBenchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/crossbow_bench_gui.png");

    public CrossbowBenchScreen(CrossbowBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        // Move the "Inventory" label to sit just above the player inventory.
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // GUI art lives in the top-left of a 256x256 sheet.
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);

        ItemStack input = this.menu.slots.get(CrossbowBenchMenu.SLOT_INPUT).getItem();
        final int color = 0x404040;

        // Left panel — what the bow/crossbow is made of.
        var assembly = input.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            g.drawString(this.font, "Limb: " + assembly.limbMaterial(), 8, 18, color, false);
            g.drawString(this.font, "Riser: " + assembly.riserMaterial(), 8, 28, color, false);
            g.drawString(this.font, "String: " + assembly.stringMaterial(), 8, 38, color, false);
        }

        // Right panel — what's fitted (crossbow only).
        if (input.is(ModItems.MODULAR_CROSSBOW.get())) {
            ResourceLocation att = input.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
            g.drawString(this.font, "Trigger:", 116, 18, color, false);
            g.drawString(this.font, "Mechanical", 116, 28, color, false);
            g.drawString(this.font, "Attach: " + (att != null ? att.getPath() : "-"), 116, 42, color, false);
        }
    }
}
