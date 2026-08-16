package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.Materials;
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
        final float s = 0.7f; // readout text is rendered smaller to save space

        g.pose().pushPose();
        g.pose().scale(s, s, 1.0f);

        // Left panel: what the bow/crossbow is made of (names resolved like the
        // item tooltip, so crafted bows show "High-Tension" not a raw id).
        var assembly = input.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            line(g, label("limb") + ": " + Materials.bowLimbName(assembly.limbMaterial()).getString(), 8, 18, s, color);
            line(g, label("riser") + ": " + Materials.bowRiserName(assembly.riserMaterial()).getString(), 8, 27, s, color);
            line(g, label("string") + ": " + Materials.bowStringName(assembly.stringMaterial()).getString(), 8, 36, s, color);
        }

        // Right panel: what's fitted (crossbow only).
        if (input.is(ModItems.MODULAR_CROSSBOW.get())) {
            ResourceLocation att = input.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
            line(g, label("trigger") + ": " + label("mechanical"), 100, 18, s, color);
            String attName = att != null ? ModCrossbowAttachments.displayName(att).getString() : label("none");
            line(g, label("attachment") + ": " + attName, 100, 27, s, color);
        }

        g.pose().popPose();
    }

    /** Draws a readout line at unscaled (x,y) inside the already-scaled (by {@code s}) pose. */
    private void line(GuiGraphics g, String text, int x, int y, float s, int color) {
        g.drawString(this.font, text, (int) (x / s), (int) (y / s), color, false);
    }

    private static String label(String key) {
        return Component.translatable("gui.fletcherstrestle." + key).getString();
    }
}
