package net.frostytrix.fletcherstrestle.client;

import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static java.lang.Math.round;

public class QuiverHudOverlay {
    public static int displayTicks = 0;    // How long the HUD should stay open
    public static float slideProgress = 0; // The actual animation state (0 to 10)
    public static float slideProgressO = 0; // The "Old" state for smooth interpolation

    private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        // Slide position, interpolated between ticks.
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        float smoothSlide = Mth.lerp(partialTick, slideProgressO, slideProgress) / 10.0f;

        // Fully hidden: nothing to draw.
        if (smoothSlide <= 0.01f) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // The first quiver in the inventory.
        ItemStack quiver = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() instanceof ModularQuiverItem) {
                quiver = player.getInventory().getItem(i);
                break;
            }
        }
        if (quiver.isEmpty()) return;

        int selected = quiver.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
        List<ItemStack> contents = ModularQuiverItem.getQuiverContents(quiver);
        int slots = Mth.clamp(quiver.getOrDefault(ModDataComponents.MAX_QUIVER_SLOTS.get(), 9), 1, contents.size());

        // Slide offset.
        // When smoothSlide is 1.0 (open), offset is 0. When it's 0.0 (closed), offset is -50.
        float yAnimationOffset = -50.0f * (1.0f - smoothSlide);

        float scale = 0.85f;
        int screenWidth = guiGraphics.guiWidth();
        // The vanilla hotbar is a 1px border, 20px per slot, and a 1px border.
        int baseWidth = slots * 20 + 2;

        int x = (screenWidth - (int) (baseWidth * scale)) / 2 + (int) round(FletcherConfig.QUIVER_HUD_X.get());
        int y = (int) round(FletcherConfig.QUIVER_HUD_Y.get()) + (int) yAnimationOffset;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // Only as many slots as the quiver has: the hotbar's left part, then its right edge.
        guiGraphics.blitSprite(HOTBAR_SPRITE, 182, 22, 0, 0, 0, 0, baseWidth - 1, 22);
        guiGraphics.blitSprite(HOTBAR_SPRITE, 182, 22, 181, 0, baseWidth - 1, 0, 1, 22);
        if (selected >= 0 && selected < slots) {
            guiGraphics.blitSprite(HOTBAR_SELECTION_SPRITE, -1 + selected * 20, -1, 24, 23);
        }

        for (int i = 0; i < slots; i++) {
            ItemStack stack = contents.get(i);
            if (!stack.isEmpty()) {
                int itemX = 3 + i * 20;
                int itemY = 3;
                guiGraphics.renderItem(stack, itemX, itemY);
                guiGraphics.renderItemDecorations(mc.font, stack, itemX, itemY);
            }
        }

        guiGraphics.pose().popPose();
    }
}