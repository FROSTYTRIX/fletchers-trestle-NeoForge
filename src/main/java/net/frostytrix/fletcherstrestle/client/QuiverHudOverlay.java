package net.frostytrix.fletcherstrestle.client;

import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.List;

import static java.lang.Math.round;

// HUD overlay that pops in from above the hotbar while the quiver
// modifier key is held / arrows are cycled. Mirrors the 1.21.1 look:
//   hotbar background + selection slot ring + arrow icons + decorations.
//
// 26.1 changes from the original:
//   * Now implements GuiLayer, registered under VanillaGuiLayers.HOTBAR
//     from menu/ModClientEvents (RegisterGuiLayersEvent).
//   * GuiGraphics → GuiGraphicsExtractor; PoseStack → joml Matrix3x2fStack
//     (pushMatrix/popMatrix/translate/scale instead of pushPose/popPose).
//   * blitSprite/item/itemDecorations gained / lost various args — calls
//     adapted to the 26.1 signatures.
public final class QuiverHudOverlay implements GuiLayer {

    // Animation state — driven by ClientKeybinds.onClientTick.
    public static int displayTicks = 0;
    public static float slideProgress = 0;
    public static float slideProgressO = 0;

    private static final Identifier HOTBAR_SPRITE =
            Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION_SPRITE =
            Identifier.withDefaultNamespace("hud/hotbar_selection");

    public static final QuiverHudOverlay INSTANCE = new QuiverHudOverlay();

    private QuiverHudOverlay() {}

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        float smoothSlide = Mth.lerp(partialTick, slideProgressO, slideProgress) / 10.0f;
        if (smoothSlide <= 0.01f) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Find the first quiver in the inventory — if there isn't one the
        // HUD has nothing to display.
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

        // Slide-in offset: -50px when closed → 0 when fully open.
        float yAnimationOffset = -50.0f * (1.0f - smoothSlide);

        float scale = 0.85f;
        int screenWidth = g.guiWidth();
        int baseWidth = 182;

        int x = (screenWidth - (int) (baseWidth * scale)) / 2
                + (int) round(FletcherConfig.QUIVER_HUD_X.get());
        int y = (int) round(FletcherConfig.QUIVER_HUD_Y.get()) + (int) yAnimationOffset;

        // 26.1: g.pose() returns a joml Matrix3x2fStack — push/pop with
        // pushMatrix/popMatrix and 2D translate/scale only.
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);

        g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 0, 0, 182, 22);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE,
                -1 + selected * 20, -1, 24, 23);

        for (int i = 0; i < 9 && i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            if (stack.isEmpty()) continue;
            int itemX = 3 + i * 20;
            int itemY = 3;
            g.item(stack, itemX, itemY);
            g.itemDecorations(mc.font, stack, itemX, itemY);
        }

        g.pose().popMatrix();
    }
}
