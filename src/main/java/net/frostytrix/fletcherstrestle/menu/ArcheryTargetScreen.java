package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.ShotRecord;
import net.frostytrix.fletcherstrestle.network.ClearShotsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;

public class ArcheryTargetScreen extends AbstractContainerScreen<ArcheryTargetMenu> {

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FletcherTrestle.MOD_ID, "textures/gui/archery_target_gui.png");

    private static final int GUI_W = 256;
    private static final int GUI_H = 196;

    // Target canvas (inner drawable)
    private static final int TC_X = 17;
    private static final int TC_Y = 15;
    private static final int TC_W = 134;
    private static final int TC_H = 134;

    // Right panel list (inner, below header strip)
    private static final int LIST_X = 180;
    private static final int LIST_Y = 26;
    private static final int LIST_W = 53;  // leave 9px for scrollbar
    private static final int LIST_H = 165;
    private static final int ROW_H = 10;

    // Scrollbar
    private static final int SB_X = 236;
    private static final int SB_Y = 26;
    private static final int SB_H = 163;

    //Thumb
    private static final int THUMB_X = 0;
    private static final int THUMB_Y = 197;
    private static final int THUMB_W = 12;
    private static final int THUMB_H = 15;

    // Info box (inner)
    private static final int INFO_X = 14;
    private static final int INFO_Y = 153;
    private static final int INFO_W = 140;
    private static final int INFO_H = 38;

    // Clear button (centered)
    private static final int BTN_W = 120;
    private static final int BTN_H = 13;
    private static final int BTN_X = (GUI_W - BTN_W) / 2;  // = 68
    private static final int BTN_Y = 200;

    private int scrollOffset = 0;
    private int selectedShot = -1;

    public ArcheryTargetScreen(ArcheryTargetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = 10000;
        this.titleLabelX = 8;
        this.titleLabelY = -10; // hidden: no title label needed
    }

    @Override
    protected void init() {
        super.init();
        ArcheryTargetMenu targetMenu = this.menu;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.fletcherstrestle.clear_shots"),
                        btn -> {
                            // Clear client-side immediately (no flicker).
                            targetMenu.setShots(Collections.emptyList());
                            scrollOffset = 0;
                            selectedShot = -1;
                            // Send the containerId with the packet so server and client stay in sync.
                            PacketDistributor.sendToServer(
                                    new ClearShotsPacket(targetMenu.getTargetPos(), targetMenu.containerId)
                            );
                        })
                .bounds(leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        g.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, GUI_W, GUI_H, 256, 256);
        renderTargetCanvas(g, mx, my);
        renderShotList(g, mx, my);
        renderInfoBox(g);
        renderScrollbar(g);
    }

    // ── Target canvas ────────────────────────────────────────────────────

    private void renderTargetCanvas(GuiGraphics g, int mx, int my) {
        int ox = leftPos + TC_X;
        int oy = topPos + TC_Y;
        int cx = ox + TC_W / 2;
        int cy = oy + TC_H / 2;

        List<ShotRecord> shots = menu.getShots();
        for (int i = 0; i < shots.size(); i++) {
            ShotRecord s = shots.get(i);
            int px = ox + Math.round(s.u() * (TC_W - 1));
            int py = oy + Math.round(s.v() * (TC_H - 1));
            boolean sel = (i == selectedShot);
            g.fill(px - 2, py - 2, px + 3, py + 3, sel ? 0xFF00FFFF : 0xFF000000);
            g.fill(px - 1, py - 1, px + 2, py + 2, sel ? 0xFF00CCCC : 0xFFFFD54A);
        }
    }

    // ── Shot list ─────────────────────────────────────────────────────────

    private void renderShotList(GuiGraphics g, int mx, int my) {
        List<ShotRecord> shots = menu.getShots();
        int ox = leftPos + LIST_X;
        int oy = topPos + LIST_Y;
        int visibleRows = LIST_H / ROW_H;
        int maxOffset = Math.max(0, shots.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxOffset);

        // "Point List" header
        g.drawString(font, "Point List", ox, topPos + 13, 0x333333, false);

        for (int i = 0; i < visibleRows; i++) {
            int idx = i + scrollOffset;
            if (idx >= shots.size()) break;
            int ry = oy + i * ROW_H;
            boolean hovered = mx >= ox && mx < ox + LIST_W && my >= ry && my < ry + ROW_H;
            boolean selected = (idx == selectedShot);
            if (selected) g.fill(ox, ry, ox + LIST_W, ry + ROW_H, 0x8033AAFF);
            else if (hovered) g.fill(ox, ry, ox + LIST_W, ry + ROW_H, 0x44FFFFFF);
            String label = "Point " + (idx + 1);
            g.drawString(font, label, ox + 2, ry + 1, 0x222222, false);
        }
    }

    // ── Info box ──────────────────────────────────────────────────────────

    private void renderInfoBox(GuiGraphics g) {
        int ox = leftPos + INFO_X;
        int oy = topPos + INFO_Y;

        if (selectedShot >= 0 && selectedShot < menu.getShots().size()) {
            ShotRecord s = menu.getShots().get(selectedShot);
            g.drawString(font,
                    "Point #" + (selectedShot + 1) +
                            "  Dmg: " + String.format("%.2f", s.estimatedDamage()) +
                            "  Spd: " + String.format("%.3f", s.speed()),
                    ox + 2, oy + 3, 0x222222, false);
            g.drawString(font,
                    "X: " + String.format("%.3f", s.x()) +
                            "  Y: " + String.format("%.3f", s.y()) +
                            "  Z: " + String.format("%.3f", s.z()),
                    ox + 2, oy + 13, 0x333333, false);
        } else {
            g.drawString(font, "Point infos :", ox + 2, oy + 3, 0x555555, false);
        }
    }

    // ── Scrollbar ─────────────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphics g) {
        List<ShotRecord> shots = menu.getShots();
        int visibleRows = LIST_H / ROW_H;
        if (shots.size() <= visibleRows) return;
        int ox = leftPos + SB_X;
        int oy = topPos + SB_Y;
        float ratio = (float) scrollOffset / Math.max(1, shots.size() - visibleRows);
        int thumbY = oy + (int) (ratio * (SB_H - THUMB_H));

        g.blit(GUI_TEXTURE, ox, thumbY, THUMB_X, THUMB_Y, THUMB_W, THUMB_H, 256, 256);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int maxOffset = Math.max(0, menu.getShots().size() - LIST_H / ROW_H);
        scrollOffset = (int) Math.max(0, Math.min(maxOffset, scrollOffset - dy));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Click on list row
        int ox = leftPos + LIST_X;
        int oy = topPos + LIST_Y;
        if (mx >= ox && mx < ox + LIST_W && my >= oy && my < oy + LIST_H) {
            int row = (int) ((my - oy) / ROW_H);
            int idx = row + scrollOffset;
            if (idx < menu.getShots().size()) {
                selectedShot = (selectedShot == idx) ? -1 : idx;
                return true;
            }
        }
        // Click on target point
        int tcox = leftPos + TC_X;
        int tcoy = topPos + TC_Y;
        List<ShotRecord> shots = menu.getShots();
        for (int i = 0; i < shots.size(); i++) {
            ShotRecord s = shots.get(i);
            int px = tcox + Math.round(s.u() * (TC_W - 1));
            int py = tcoy + Math.round(s.v() * (TC_H - 1));
            int ddx = (int) mx - px, ddy = (int) my - py;
            if (ddx * ddx + ddy * ddy <= 16) {
                selectedShot = (selectedShot == i) ? -1 : i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);

        // Hover tooltip on target points
        int tcox = leftPos + TC_X;
        int tcoy = topPos + TC_Y;
        List<ShotRecord> shots = menu.getShots();
        for (int i = 0; i < shots.size(); i++) {
            if (i == selectedShot) continue;
            ShotRecord s = shots.get(i);
            int px = tcox + Math.round(s.u() * (TC_W - 1));
            int py = tcoy + Math.round(s.v() * (TC_H - 1));
            int ddx = mx - px, ddy = my - py;
            if (ddx * ddx + ddy * ddy <= 16) {
                List<FormattedCharSequence> lines = List.of(
                        Component.literal("Point #" + (i + 1)).getVisualOrderText(),
                        Component.literal("Dmg: " + String.format("%.2f", s.estimatedDamage())).getVisualOrderText(),
                        Component.literal("Spd: " + String.format("%.3f", s.speed())).getVisualOrderText()
                );
                g.renderTooltip(font, lines, mx, my);
                break;
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // No default labels
    }
}