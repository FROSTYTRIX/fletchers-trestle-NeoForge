package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.ShotRecord;
import net.frostytrix.fletcherstrestle.network.ClearShotsPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Collections;
import java.util.List;

// 26.1 port of the archery-target shot-log screen. Restores the full
// 1.21.1 functionality:
//   * Background panel + per-shot dots on the target canvas (yellow,
//     cyan when selected).
//   * Scrollable point list down the right panel with hover + selected
//     highlights and a hand-drawn scrollbar thumb.
//   * Info box at the bottom showing dmg / speed / x / y / z for the
//     selected shot.
//   * "Clear shots" button at the bottom that resets local state
//     immediately (no flicker) and fires ClearShotsPacket to the server.
//   * Mouse wheel scrolls the list; clicking a point or row toggles
//     selection; hovering an un-selected dot pops a tooltip.
//
// 26.1 changes threaded through this rewrite:
//   * imageWidth / imageHeight are final → pass through the 5-arg super.
//   * renderBg / render → extractBackground / extractRenderState.
//   * blit / renderTooltip / drawString gained RenderPipeline-flavoured
//     replacements: blit(RenderPipeline, Identifier, …, texW, texH),
//     setTooltipForNextFrame(Font, lines, x, y), text(Font, str, x, y,
//     color).
//   * mouseClicked(double, double, int) → mouseClicked(MouseButtonEvent,
//     boolean doubleClick).
//   * PacketDistributor.sendToServer → ClientPacketDistributor.sendToServer.
public class ArcheryTargetScreen extends AbstractContainerScreen<ArcheryTargetMenu> {

    private static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(
            FletcherTrestle.MOD_ID, "textures/gui/archery_target_gui.png");

    private static final int GUI_W = 256;
    private static final int GUI_H = 196;

    // Target canvas (the painted face of the target).
    private static final int TC_X = 17;
    private static final int TC_Y = 15;
    private static final int TC_W = 134;
    private static final int TC_H = 134;

    // Right-side point list rect.
    private static final int LIST_X = 180;
    private static final int LIST_Y = 26;
    private static final int LIST_W = 53;
    private static final int LIST_H = 165;
    private static final int ROW_H  = 10;

    // Scrollbar geometry.
    private static final int SB_X  = 236;
    private static final int SB_Y  = 26;
    private static final int SB_H  = 163;

    // Thumb sprite location within GUI_TEXTURE.
    private static final int THUMB_X = 0;
    private static final int THUMB_Y = 197;
    private static final int THUMB_W = 12;
    private static final int THUMB_H = 15;

    // Info box (bottom band, below the canvas).
    private static final int INFO_X = 14;
    private static final int INFO_Y = 153;

    // Clear-shots button geometry.
    private static final int BTN_W = 120;
    private static final int BTN_H = 13;
    private static final int BTN_X = (GUI_W - BTN_W) / 2;
    private static final int BTN_Y = 200;

    private int scrollOffset = 0;
    private int selectedShot = -1;

    public ArcheryTargetScreen(ArcheryTargetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, GUI_W, GUI_H);
        // No inventory label — push it off-screen rather than override
        // extractLabels (cheaper, matches the 1.21.1 behaviour).
        this.inventoryLabelY = 10000;
        this.titleLabelY = -10;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.fletcherstrestle.clear_shots"),
                        btn -> {
                            // Clear locally so the UI updates immediately;
                            // the server is authoritative but the round-trip
                            // would otherwise cause a one-frame flicker.
                            this.menu.setShots(Collections.emptyList());
                            this.scrollOffset = 0;
                            this.selectedShot = -1;
                            ClientPacketDistributor.sendToServer(
                                    new ClearShotsPacket(this.menu.getTargetPos(), this.menu.containerId));
                        })
                .bounds(this.leftPos + BTN_X, this.topPos + BTN_Y, BTN_W, BTN_H)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        // Main panel.
        g.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                this.leftPos, this.topPos, 0F, 0F,
                GUI_W, GUI_H, 256, 256);
        renderTargetCanvas(g);
        renderShotList(g, mouseX, mouseY);
        renderInfoBox(g);
        renderScrollbar(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // Hover tooltip for unselected dots on the canvas. extractRenderState
        // runs after extractBackground / slots / labels, so this layers on
        // top via the deferred tooltip slot.
        int tcox = this.leftPos + TC_X;
        int tcoy = this.topPos + TC_Y;
        List<ShotRecord> shots = this.menu.getShots();
        for (int i = 0; i < shots.size(); i++) {
            if (i == this.selectedShot) continue;
            ShotRecord s = shots.get(i);
            int px = tcox + Math.round(s.u() * (TC_W - 1));
            int py = tcoy + Math.round(s.v() * (TC_H - 1));
            int dx = mouseX - px;
            int dy = mouseY - py;
            if (dx * dx + dy * dy <= 16) {
                List<FormattedCharSequence> lines = List.of(
                        Component.literal("Point #" + (i + 1)).getVisualOrderText(),
                        Component.literal("Dmg: " + String.format("%.2f", s.estimatedDamage())).getVisualOrderText(),
                        Component.literal("Spd: " + String.format("%.3f", s.speed())).getVisualOrderText());
                g.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                break;
            }
        }
    }

    // ── Target canvas ────────────────────────────────────────────────────

    private void renderTargetCanvas(GuiGraphicsExtractor g) {
        int ox = this.leftPos + TC_X;
        int oy = this.topPos + TC_Y;
        List<ShotRecord> shots = this.menu.getShots();
        for (int i = 0; i < shots.size(); i++) {
            ShotRecord s = shots.get(i);
            int px = ox + Math.round(s.u() * (TC_W - 1));
            int py = oy + Math.round(s.v() * (TC_H - 1));
            boolean sel = i == this.selectedShot;
            // 5×5 outer pixel, 3×3 inner pixel — outer is black/cyan
            // (selection ring), inner is yellow/teal (the dot itself).
            g.fill(px - 2, py - 2, px + 3, py + 3, sel ? 0xFF00FFFF : 0xFF000000);
            g.fill(px - 1, py - 1, px + 2, py + 2, sel ? 0xFF00CCCC : 0xFFFFD54A);
        }
    }

    // ── Shot list ────────────────────────────────────────────────────────

    private void renderShotList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<ShotRecord> shots = this.menu.getShots();
        int ox = this.leftPos + LIST_X;
        int oy = this.topPos + LIST_Y;
        int visibleRows = LIST_H / ROW_H;
        int maxOffset = Math.max(0, shots.size() - visibleRows);
        this.scrollOffset = Math.min(this.scrollOffset, maxOffset);

        g.text(this.font, "Point List", ox, this.topPos + 13, 0x333333);

        for (int i = 0; i < visibleRows; i++) {
            int idx = i + this.scrollOffset;
            if (idx >= shots.size()) break;
            int ry = oy + i * ROW_H;
            boolean hovered = mouseX >= ox && mouseX < ox + LIST_W
                    && mouseY >= ry && mouseY < ry + ROW_H;
            boolean selected = idx == this.selectedShot;
            if (selected) {
                g.fill(ox, ry, ox + LIST_W, ry + ROW_H, 0x8033AAFF);
            } else if (hovered) {
                g.fill(ox, ry, ox + LIST_W, ry + ROW_H, 0x44FFFFFF);
            }
            g.text(this.font, "Point " + (idx + 1), ox + 2, ry + 1, 0x222222);
        }
    }

    // ── Info box ─────────────────────────────────────────────────────────

    private void renderInfoBox(GuiGraphicsExtractor g) {
        int ox = this.leftPos + INFO_X;
        int oy = this.topPos + INFO_Y;
        List<ShotRecord> shots = this.menu.getShots();
        if (this.selectedShot >= 0 && this.selectedShot < shots.size()) {
            ShotRecord s = shots.get(this.selectedShot);
            g.text(this.font,
                    "Point #" + (this.selectedShot + 1)
                            + "  Dmg: " + String.format("%.2f", s.estimatedDamage())
                            + "  Spd: " + String.format("%.3f", s.speed()),
                    ox + 2, oy + 3, 0x222222);
            g.text(this.font,
                    "X: " + String.format("%.3f", s.x())
                            + "  Y: " + String.format("%.3f", s.y())
                            + "  Z: " + String.format("%.3f", s.z()),
                    ox + 2, oy + 13, 0x333333);
        } else {
            g.text(this.font, "Point infos :", ox + 2, oy + 3, 0x555555);
        }
    }

    // ── Scrollbar ────────────────────────────────────────────────────────

    private void renderScrollbar(GuiGraphicsExtractor g) {
        List<ShotRecord> shots = this.menu.getShots();
        int visibleRows = LIST_H / ROW_H;
        if (shots.size() <= visibleRows) return;
        int ox = this.leftPos + SB_X;
        int oy = this.topPos + SB_Y;
        float ratio = (float) this.scrollOffset / Math.max(1, shots.size() - visibleRows);
        int thumbY = oy + (int) (ratio * (SB_H - THUMB_H));
        g.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE,
                ox, thumbY, (float) THUMB_X, (float) THUMB_Y,
                THUMB_W, THUMB_H, 256, 256);
    }

    // ── Input ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int maxOffset = Math.max(0, this.menu.getShots().size() - LIST_H / ROW_H);
        this.scrollOffset = (int) Math.max(0, Math.min(maxOffset, this.scrollOffset - dy));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        // List-row click → toggle that row's selection.
        int lox = this.leftPos + LIST_X;
        int loy = this.topPos + LIST_Y;
        if (mouseX >= lox && mouseX < lox + LIST_W
                && mouseY >= loy && mouseY < loy + LIST_H) {
            int row = (int) ((mouseY - loy) / ROW_H);
            int idx = row + this.scrollOffset;
            if (idx < this.menu.getShots().size()) {
                this.selectedShot = (this.selectedShot == idx) ? -1 : idx;
                return true;
            }
        }

        // Click on a target dot → toggle that shot's selection.
        int tcox = this.leftPos + TC_X;
        int tcoy = this.topPos + TC_Y;
        List<ShotRecord> shots = this.menu.getShots();
        for (int i = 0; i < shots.size(); i++) {
            ShotRecord s = shots.get(i);
            int px = tcox + Math.round(s.u() * (TC_W - 1));
            int py = tcoy + Math.round(s.v() * (TC_H - 1));
            int dx = (int) mouseX - px;
            int dy = (int) mouseY - py;
            if (dx * dx + dy * dy <= 16) {
                this.selectedShot = (this.selectedShot == i) ? -1 : i;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
