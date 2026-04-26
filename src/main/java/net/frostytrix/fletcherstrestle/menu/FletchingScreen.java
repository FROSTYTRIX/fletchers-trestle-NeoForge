package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.network.TuningPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");
    private static final ResourceLocation MINIGAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/minigame.png");

    private boolean isTuning = false;
    private float barPosition = 0.0f; // 0.0 to 1.0
    private boolean movingRight = true;
    private float targetPosition = 0.5f;

    private net.minecraft.client.gui.components.Button assembleButton;

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.assembleButton = net.minecraft.client.gui.components.Button.builder(Component.literal("Assemble"), b -> {
            if (this.getMenu().canAssemble()) {
                this.isTuning = true;

                // Randomize the sweet spot between 15% and 85% of the bar
                // (We avoid 0.0 and 1.0 so the sweet spot doesn't touch the very edges)
                this.targetPosition = 0.15f + (float)(Math.random() * 0.70f);

                this.barPosition = 0.0f; // Reset pointer to the start
            }
        }).bounds(getGuiLeft() + 100, getGuiTop() + 60, 60, 20).build();

        this.addRenderableWidget(this.assembleButton);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        if (this.assembleButton != null) {
            // The button is ONLY active if the recipe is valid AND we aren't currently playing the mini-game!
            this.assembleButton.active = this.getMenu().canAssemble() && !this.isTuning;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Calculate the center of the screen
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        // Draw the background texture
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Darkens the background behind the menu
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Renders the item names when you hover over them
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isTuning) {
            renderTuningBar(guiGraphics);
            updateBarLogic();
        }
    }

    private void updateBarLogic() {
        float speed = FletcherConfig.MINIGAME_SPEED.get().floatValue();

        if (movingRight) {
            barPosition += speed;
            if (barPosition >= 1.0f) {
                barPosition = 1.0f;  // Force it perfectly to the edge
                movingRight = false; // Bounce left
            }
        } else {
            barPosition -= speed;
            if (barPosition <= 0.0f) {
                barPosition = 0.0f;  // Force it perfectly to the edge
                movingRight = true;  // Bounce right
            }
        }
    }

    private void renderTuningBar(GuiGraphics guiGraphics) {
        // We set the total width of your texture bar
        int barWidth = 128;

        // This math perfectly centers the 128px bar horizontally inside the GUI
        int x = getGuiLeft() + (imageWidth - barWidth) / 2;
        int y = getGuiTop() + 75;

        // --- 1. Draw the Background (The Log) ---
        // Parameters: texture, screenX, screenY, uOffset (X in image), vOffset (Y in image), width, height
        guiGraphics.blit(MINIGAME_TEXTURE, x, y, 0, 0, barWidth, 16);

        // --- 2. Draw the Sweet Spot (The Target) ---
        int targetWidth = 20;
        // Map the random targetPosition (0.15 to 0.85) to the pixel width of the bar
        int targetX = x + (int) (targetPosition * (barWidth - targetWidth));
        guiGraphics.blit(MINIGAME_TEXTURE, targetX, y, 0, 16, targetWidth, 16);

        // --- 3. Draw the Moving Pointer ---
        int pointerWidth = 12;
        int pointerX = x + (int) (barPosition * (barWidth - pointerWidth));

        // -- JUICE: The Shake Effect --
        int yOffset = 0;
        // CHANGE: Calculate distance from the RANDOM target, not 0.5f!
        float distance = Math.abs(barPosition - targetPosition);

        if (distance < 0.1f && Minecraft.getInstance().level != null) {
            yOffset = (int)(Math.sin(Minecraft.getInstance().level.getGameTime() * 2) * 1.5);
        }

        guiGraphics.blit(MINIGAME_TEXTURE, pointerX, (y - 2) + yOffset, 0, 32, pointerWidth, 20);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isTuning && (keyCode == 32 || keyCode == 257)) { // Space or Enter
            calculateFinalScore();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void calculateFinalScore() {
        float distance = Math.abs(barPosition - targetPosition);

        // Fetch the config values!
        float minScore = FletcherConfig.MINIGAME_MIN_SCORE.get().floatValue();
        float multiplier = FletcherConfig.MINIGAME_PUNISH_MULTIPLIER.get().floatValue();

        // Apply them to the math
        float quality = Math.max(minScore, 1.0f - (distance * multiplier));

        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TuningPacket(quality));

        this.isTuning = false;
        this.barPosition = 0.0f;
    }
}