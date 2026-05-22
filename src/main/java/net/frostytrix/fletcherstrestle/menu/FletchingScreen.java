package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.network.FletchingTabPayload;
import net.frostytrix.fletcherstrestle.network.TuningPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

// 26.1 fletching table screen — full port of the 1.21.1 implementation:
//   * Bow / arrow tab buttons on the left edge (with active/inactive
//     pad states + click-to-switch firing FletchingTabPayload).
//   * Background panel swaps between bow / arrow textures.
//   * Bow tab only: "Assemble" button that kicks off the tuning
//     sweet-spot minigame. Bar slides left-right, player presses
//     space / enter near the moving target to lock in a tuning score
//     that gets shipped to the server via TuningPacket.
//
// 26.1 changes I had to thread through:
//   * imageWidth / imageHeight are final → pass via super() / leave the
//     defaults (176×166).
//   * render(GuiGraphics, …)            → extractBackground(GuiGraphicsExtractor, …)
//   * blit(Identifier, …)               → blit(RenderPipeline, Identifier, …, texW, texH)
//   * renderItem(stack, x, y)           → extractor.item(stack, x, y)
//   * mouseClicked(double, double, int) → mouseClicked(MouseButtonEvent, boolean)
//   * keyPressed(int, int, int)         → keyPressed(KeyEvent)
//   * PacketDistributor.sendToServer    → ClientPacketDistributor.sendToServer
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");
    private static final Identifier ARROW_TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table_arrow.png");
    private static final Identifier MINIGAME_TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/minigame.png");

    private boolean isTuning = false;
    private float barPosition = 0.0F;
    private boolean movingRight = true;
    private float targetPosition = 0.5F;

    private Button assembleButton;

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean canAssemble() {
        return !this.menu.resultSlots.getItem(0).isEmpty();
    }

    @Override
    protected void init() {
        super.init();
        this.assembleButton = Button.builder(Component.literal("Assemble"), b -> {
            if (canAssemble()) {
                this.isTuning = true;
                // Pick a random sweet-spot 15%-85% across the bar so the
                // minigame isn't always centered.
                this.targetPosition = 0.15F + (float) (Math.random() * 0.70F);
                this.barPosition = 0.0F;
                this.movingRight = true;
            }
        }).bounds(this.leftPos + 100, this.topPos + 60, 60, 20).build();
        this.addRenderableWidget(this.assembleButton);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.assembleButton != null) {
            // Hide on the arrow tab — no minigame there.
            this.assembleButton.visible = this.menu.activeTab == 0;
            this.assembleButton.active = canAssemble() && !this.isTuning;
        }
        if (this.isTuning) {
            updateBarLogic();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Main panel.
        Identifier panel = this.menu.activeTab == 0 ? TEXTURE : ARROW_TEXTURE;
        extractor.blit(RenderPipelines.GUI_TEXTURED, panel,
                x, y, 0F, 0F,
                this.imageWidth, this.imageHeight,
                256, 256);

        // Bow tab pad at (x-23, y+10).
        int bowTabY = y + 10;
        float bowU = this.menu.activeTab == 0 ? 0F : 28F;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x - 23, bowTabY, bowU, 166F,
                28, 32, 256, 256);
        extractor.item(new ItemStack(ModItems.MODULAR_BOW.get()), x - 17, bowTabY + 8);

        // Arrow tab pad at (x-23, y+44).
        int arrowTabY = y + 44;
        float arrowU = this.menu.activeTab == 1 ? 0F : 28F;
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x - 23, arrowTabY, arrowU, 166F,
                28, 32, 256, 256);
        extractor.item(new ItemStack(ModItems.MODULAR_ARROW.get()), x - 17, arrowTabY + 8);

        // Tuning bar overlay (only while the minigame is active on the bow tab).
        if (this.isTuning && this.menu.activeTab == 0) {
            renderTuningBar(extractor);
        }
    }

    private void renderTuningBar(GuiGraphicsExtractor extractor) {
        int barWidth = 128;
        int x = this.leftPos + (this.imageWidth - barWidth) / 2;
        int y = this.topPos + 75;

        // Track (bar background).
        extractor.blit(RenderPipelines.GUI_TEXTURED, MINIGAME_TEXTURE,
                x, y, 0F, 0F, barWidth, 16, 256, 256);

        // Sweet-spot target zone.
        int targetWidth = 20;
        int targetX = x + (int) (this.targetPosition * (barWidth - targetWidth));
        extractor.blit(RenderPipelines.GUI_TEXTURED, MINIGAME_TEXTURE,
                targetX, y, 0F, 16F, targetWidth, 16, 256, 256);

        // Moving pointer. When the pointer is in the sweet spot we add a
        // small vertical bob to telegraph "now is the time to press".
        int pointerWidth = 12;
        int pointerX = x + (int) (this.barPosition * (barWidth - pointerWidth));
        int yOffset = 0;
        float distance = Math.abs(this.barPosition - this.targetPosition);
        if (distance < 0.1F && Minecraft.getInstance().level != null) {
            yOffset = (int) (Math.sin(Minecraft.getInstance().level.getGameTime() * 2) * 1.5);
        }
        extractor.blit(RenderPipelines.GUI_TEXTURED, MINIGAME_TEXTURE,
                pointerX, (y - 2) + yOffset, 0F, 32F, pointerWidth, 20, 256, 256);
    }

    private void updateBarLogic() {
        float speed = FletcherConfig.MINIGAME_SPEED.get().floatValue();
        if (this.movingRight) {
            this.barPosition += speed;
            if (this.barPosition >= 1.0F) {
                this.barPosition = 1.0F;
                this.movingRight = false;
            }
        } else {
            this.barPosition -= speed;
            if (this.barPosition <= 0.0F) {
                this.barPosition = 0.0F;
                this.movingRight = true;
            }
        }
    }

    private void confirmTuning() {
        float distance = Math.abs(this.barPosition - this.targetPosition);
        float minScore = FletcherConfig.MINIGAME_MIN_SCORE.get().floatValue();
        float multiplier = FletcherConfig.MINIGAME_PUNISH_MULTIPLIER.get().floatValue();
        float quality = Math.max(minScore, 1.0F - (distance * multiplier));
        ClientPacketDistributor.sendToServer(new TuningPacket(quality));
        this.isTuning = false;
        this.barPosition = 0.0F;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int x = this.leftPos;
        int y = this.topPos;

        if (this.menu.activeTab != 0
                && mouseX >= x - 23 && mouseX < x + 5
                && mouseY >= y + 10 && mouseY < y + 42) {
            switchTab(0);
            return true;
        }
        if (this.menu.activeTab != 1
                && mouseX >= x - 23 && mouseX < x + 5
                && mouseY >= y + 44 && mouseY < y + 76) {
            switchTab(1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Space (32) or Enter (257) locks in the tuning result.
        if (this.isTuning && (event.key() == 32 || event.key() == 257)) {
            confirmTuning();
            return true;
        }
        return super.keyPressed(event);
    }

    private void switchTab(int tabId) {
        ClientPacketDistributor.sendToServer(new FletchingTabPayload(tabId));
        this.menu.activeTab = tabId;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
