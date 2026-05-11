package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.network.FletchingTabPayload;
import net.frostytrix.fletcherstrestle.network.TuningPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table_arrow.png");
    private static final ResourceLocation MINIGAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/minigame.png");

    private boolean isTuning = false;
    private float barPosition = 0.0f;
    private boolean movingRight = true;
    private float targetPosition = 0.5f;

    private net.minecraft.client.gui.components.Button assembleButton;

    public FletchingScreen(FletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    private boolean canAssemble() {
        // Validation now perfectly mirrors the RecipeManager's output logic
        return !this.menu.resultSlots.getItem(0).isEmpty();
    }

    @Override
    protected void init() {
        super.init();

        this.assembleButton = net.minecraft.client.gui.components.Button.builder(Component.literal("Assemble"), b -> {
            if (canAssemble()) {
                this.isTuning = true;
                this.targetPosition = 0.15f + (float)(Math.random() * 0.70f);
                this.barPosition = 0.0f;
            }
        }).bounds(getGuiLeft() + 100, getGuiTop() + 60, 60, 20).build();

        this.addRenderableWidget(this.assembleButton);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.assembleButton != null) {
            this.assembleButton.visible = this.menu.activeTab == 0;
            this.assembleButton.active = canAssemble() && !this.isTuning;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (this.menu.activeTab == 0) {
            guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        } else {
            guiGraphics.blit(ARROW_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        }

        // Draw Bow Tab (Tab 0)
        int bowTabY = y + 10;
        if (this.menu.activeTab == 0) {
            guiGraphics.blit(TEXTURE, x - 23, bowTabY, 0, 166, 28, 32);
        } else {
            guiGraphics.blit(TEXTURE, x - 23, bowTabY, 28, 166, 28, 32);
        }
        guiGraphics.renderItem(new ItemStack(ModItems.MODULAR_BOW.get()), x - 17, bowTabY + 8);

        // Draw Arrow Tab (Tab 1)
        int arrowTabY = y + 44;
        if (this.menu.activeTab == 1) {
            guiGraphics.blit(TEXTURE, x - 23, arrowTabY, 0, 166, 28, 32);
        } else {
            guiGraphics.blit(TEXTURE, x - 23, arrowTabY, 28, 166, 28, 32);
        }
        guiGraphics.renderItem(new ItemStack(ModItems.MODULAR_ARROW.get()), x - 17, arrowTabY + 8);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isTuning) {
            renderTuningBar(guiGraphics);
            updateBarLogic();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        if (this.menu.activeTab != 0 && mouseX >= x - 23 && mouseX < x && mouseY >= y + 10 && mouseY < y + 42) {
            PacketDistributor.sendToServer(new FletchingTabPayload(0));
            this.menu.activeTab = 0;
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (this.menu.activeTab != 1 && mouseX >= x - 28 && mouseX < x && mouseY >= y + 44 && mouseY < y + 76) {
            PacketDistributor.sendToServer(new FletchingTabPayload(1));
            this.menu.activeTab = 1;
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateBarLogic() {
        float speed = FletcherConfig.MINIGAME_SPEED.get().floatValue();
        if (movingRight) {
            barPosition += speed;
            if (barPosition >= 1.0f) {
                barPosition = 1.0f;
                movingRight = false;
            }
        } else {
            barPosition -= speed;
            if (barPosition <= 0.0f) {
                barPosition = 0.0f;
                movingRight = true;
            }
        }
    }

    private void renderTuningBar(GuiGraphics guiGraphics) {
        int barWidth = 128;
        int x = getGuiLeft() + (imageWidth - barWidth) / 2;
        int y = getGuiTop() + 75;

        guiGraphics.blit(MINIGAME_TEXTURE, x, y, 0, 0, barWidth, 16);

        int targetWidth = 20;
        int targetX = x + (int) (targetPosition * (barWidth - targetWidth));
        guiGraphics.blit(MINIGAME_TEXTURE, targetX, y, 0, 16, targetWidth, 16);

        int pointerWidth = 12;
        int pointerX = x + (int) (barPosition * (barWidth - pointerWidth));

        int yOffset = 0;
        float distance = Math.abs(barPosition - targetPosition);
        if (distance < 0.1f && Minecraft.getInstance().level != null) {
            yOffset = (int)(Math.sin(Minecraft.getInstance().level.getGameTime() * 2) * 1.5);
        }

        guiGraphics.blit(MINIGAME_TEXTURE, pointerX, (y - 2) + yOffset, 0, 32, pointerWidth, 20);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isTuning && (keyCode == 32 || keyCode == 257)) {
            calculateFinalScore();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void calculateFinalScore() {
        float distance = Math.abs(barPosition - targetPosition);
        float minScore = FletcherConfig.MINIGAME_MIN_SCORE.get().floatValue();
        float multiplier = FletcherConfig.MINIGAME_PUNISH_MULTIPLIER.get().floatValue();
        float quality = Math.max(minScore, 1.0f - (distance * multiplier));

        PacketDistributor.sendToServer(new TuningPacket(quality));

        this.isTuning = false;
        this.barPosition = 0.0f;
    }
}