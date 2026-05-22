package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.network.FletchingTabPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
// 26.1 split client→server packet send out of PacketDistributor into a
// client-only helper. The server side still uses PacketDistributor.
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// 26.1 fletching table screen. Restores the bow-vs-arrow tab UI from the
// 1.21.1 implementation so the player can actually switch crafting modes.
// Background swaps between bow/arrow textures and the two tab pads draw
// off the left edge of the GUI, each showing the icon for what they
// craft. Clicks on the inactive tab fire FletchingTabPayload at the
// server.
//
// 26.1 changes that bit us before:
//   * render(GuiGraphics, ...)        → extractBackground(GuiGraphicsExtractor, ...)
//   * blit(Identifier, x, y, u, v, w, h)
//       → blit(RenderPipeline, Identifier, x, y, u, v, w, h, texW, texH)
//   * renderItem(stack, x, y)         → extractor.item(stack, x, y)
//   * mouseClicked(double, double, int)
//       → mouseClicked(MouseButtonEvent, boolean doubleClick)
//
// Still pending the wider screen port: tuning sweet-spot minigame,
// "Assemble" button, draw-time readout, recipe placement listeners.
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");
    private static final Identifier ARROW_TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table_arrow.png");

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Main panel — bow texture for tab 0, arrow texture for tab 1.
        Identifier panel = this.menu.activeTab == 0 ? TEXTURE : ARROW_TEXTURE;
        extractor.blit(RenderPipelines.GUI_TEXTURED, panel,
                x, y, 0F, 0F,
                this.imageWidth, this.imageHeight,
                256, 256);

        // Bow tab — sits at (x-23, y+10), 28×32. Inactive variant pulls
        // from u=28 on the bow sheet to draw a slightly receded look.
        int bowTabY = y + 10;
        float bowU = this.menu.activeTab == 0 ? 0F : 28F;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x - 23, bowTabY, bowU, 166F,
                28, 32, 256, 256);
        extractor.item(new ItemStack(ModItems.MODULAR_BOW.get()), x - 17, bowTabY + 8);

        // Arrow tab — same pad geometry, offset 34px below.
        int arrowTabY = y + 44;
        float arrowU = this.menu.activeTab == 1 ? 0F : 28F;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x - 23, arrowTabY, arrowU, 166F,
                28, 32, 256, 256);
        extractor.item(new ItemStack(ModItems.MODULAR_ARROW.get()), x - 17, arrowTabY + 8);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int x = this.leftPos;
        int y = this.topPos;

        // Bow tab hit-rect: 28×32 starting at (x-23, y+10).
        if (this.menu.activeTab != 0
                && mouseX >= x - 23 && mouseX < x + 5
                && mouseY >= y + 10 && mouseY < y + 42) {
            switchTab(0);
            return true;
        }
        // Arrow tab hit-rect: 28×32 starting at (x-23, y+44).
        if (this.menu.activeTab != 1
                && mouseX >= x - 23 && mouseX < x + 5
                && mouseY >= y + 44 && mouseY < y + 76) {
            switchTab(1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void switchTab(int tabId) {
        ClientPacketDistributor.sendToServer(new FletchingTabPayload(tabId));
        // Server is authoritative, but flip locally too so the next frame
        // already shows the new background instead of waiting for the
        // packet round-trip.
        this.menu.activeTab = tabId;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
