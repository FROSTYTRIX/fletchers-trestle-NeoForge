package net.frostytrix.fletcherstrestle.client.guide;

import net.frostytrix.fletcherstrestle.client.ClientArcheryData;
import net.frostytrix.fletcherstrestle.network.SpendSkillPacket;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** The in-game Fletcher's Guide (Phase 4). */
public class FletcherGuideScreen extends Screen {

    private static final String WIKI_URL =
            "https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge/wiki";

    private static final int WIDTH = 260;
    private static final int HEIGHT = 188;
    private static final int CONTENT_X = 96;     // content pane left, relative to panel
    private static final int CONTENT_W = 156;
    private static final int CONTENT_TOP = 26;
    private static final int CONTENT_H = 140;

    private final List<GuideChapter> chapters = GuideContent.chapters();
    private int left;
    private int top;
    private int selected = 0;
    private int scrollY = 0;
    private int contentHeight = 0;

    private final Button[] skillButtons = new Button[ArcherySkill.values().length];

    public FletcherGuideScreen() {
        super(Component.translatable("gui.fletcherstrestle.guide.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;

        // Chapter list (left column).
        for (int i = 0; i < chapters.size(); i++) {
            final int idx = i;
            this.addRenderableWidget(Button.builder(chapters.get(i).title(), b -> select(idx))
                    .bounds(this.left + 8, this.top + 26 + i * 20, 80, 18)
                    .build());
        }

        // Open-wiki button (bottom-left).
        this.addRenderableWidget(Button.builder(Component.translatable("gui.fletcherstrestle.guide.open_wiki"),
                        b -> this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                            if (confirmed) {
                                Util.getPlatform().openUri(WIKI_URL);
                            }
                            this.minecraft.setScreen(this);
                        }, WIKI_URL, true)))
                .bounds(this.left + 8, this.top + HEIGHT - 22, 80, 18)
                .build());

        // Skill-tree spend buttons (only shown on the skills chapter).
        ArcherySkill[] skills = ArcherySkill.values();
        for (int i = 0; i < skills.length; i++) {
            final int branch = i;
            this.skillButtons[i] = Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new SpendSkillPacket(branch)))
                    .bounds(this.left + CONTENT_X + CONTENT_W - 22, this.top + CONTENT_TOP + 24 + i * 24, 20, 20)
                    .build();
            this.addRenderableWidget(this.skillButtons[i]);
        }
    }

    private void select(int idx) {
        this.selected = idx;
        this.scrollY = 0;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xB0000000); // plain dim, no blur
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        // Panel + borders.
        g.fill(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, 0xFF1A130C);
        g.fill(this.left, this.top, this.left + WIDTH, this.top + 1, 0xFF5A4632);
        g.fill(this.left, this.top + HEIGHT - 1, this.left + WIDTH, this.top + HEIGHT, 0xFF5A4632);
        g.fill(this.left, this.top, this.left + 1, this.top + HEIGHT, 0xFF5A4632);
        g.fill(this.left + WIDTH - 1, this.top, this.left + WIDTH, this.top + HEIGHT, 0xFF5A4632);
        g.fill(this.left + CONTENT_X - 4, this.top + 24, this.left + CONTENT_X - 3, this.top + HEIGHT - 6, 0xFF5A4632); // divider

        GuideChapter chapter = this.chapters.get(this.selected);
        g.drawString(this.font, chapter.title(), this.left + CONTENT_X, this.top + 12, 0xFFD27D, false);

        boolean skill = chapter.skillTree();
        for (Button b : this.skillButtons) {
            b.visible = skill;
        }

        int cx = this.left + CONTENT_X;
        int cyTop = this.top + CONTENT_TOP;
        g.enableScissor(cx, cyTop, cx + CONTENT_W, cyTop + CONTENT_H);
        if (skill) {
            renderSkillChapter(g, cx, cyTop - this.scrollY);
        } else {
            this.contentHeight = renderElements(g, chapter, cx, cyTop - this.scrollY);
        }
        g.disableScissor();

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Draws a chapter's elements; returns total content height for scroll clamping. */
    private int renderElements(GuiGraphics g, GuideChapter chapter, int x, int y) {
        int cursor = y;
        for (GuideElement el : chapter.elements()) {
            switch (el.type()) {
                case HEADING -> {
                    g.drawString(this.font, el.text(), x, cursor, 0xFFE0A0, false);
                    cursor += 14;
                }
                case TEXT -> {
                    for (FormattedCharSequence line : this.font.split(el.text(), CONTENT_W - 4)) {
                        g.drawString(this.font, line, x, cursor, 0xCFCFCF, false);
                        cursor += 10;
                    }
                    cursor += 4;
                }
                case ITEM -> {
                    g.renderItem(el.icon(), x, cursor);
                    int textX = x + 20;
                    for (FormattedCharSequence line : this.font.split(el.text(), CONTENT_W - 24)) {
                        g.drawString(this.font, line, textX, cursor + 1, 0xCFCFCF, false);
                        cursor += 10;
                    }
                    cursor = Math.max(cursor, cursor) + 8;
                }
            }
        }
        return cursor - y;
    }

    private void renderSkillChapter(GuiGraphics g, int x, int y) {
        for (FormattedCharSequence line : this.font.split(
                Component.translatable("gui.fletcherstrestle.guide.skills.b1"), CONTENT_W - 4)) {
            g.drawString(this.font, line, x, y, 0xCFCFCF, false);
            y += 10;
        }
        y += 6;
        int points = ClientArcheryData.pointsAvailable();
        g.drawString(this.font, Component.translatable("gui.fletcherstrestle.skill_points", points), x, y, 0xFFD700, false);

        ArcherySkill[] skills = ArcherySkill.values();
        for (int i = 0; i < skills.length; i++) {
            int rank = ClientArcheryData.rank(skills[i]);
            this.skillButtons[i].active = points > 0 && rank < ArcherySkill.MAX_RANK;
            int rowY = this.top + CONTENT_TOP + 24 + i * 24; // matches button bounds (unscrolled)
            g.drawString(this.font, branchName(skills[i]), x, rowY + 1, 0xFFFFFF, false);
            g.drawString(this.font, rank + "/" + ArcherySkill.MAX_RANK, x, rowY + 11, 0xA0A0A0, false);
        }
    }

    private static Component branchName(ArcherySkill skill) {
        return switch (skill) {
            case DRAW -> Component.translatable("gui.fletcherstrestle.skill_draw");
            case CRIT -> Component.translatable("gui.fletcherstrestle.skill_crit");
            case AIM -> Component.translatable("gui.fletcherstrestle.skill_aim");
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.chapters.get(this.selected).skillTree()) {
            int max = Math.max(0, this.contentHeight - CONTENT_H);
            this.scrollY = (int) Math.max(0, Math.min(max, this.scrollY - scrollY * 12));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new FletcherGuideScreen());
    }
}
