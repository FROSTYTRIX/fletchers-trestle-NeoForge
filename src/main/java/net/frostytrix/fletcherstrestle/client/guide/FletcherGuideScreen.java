package net.frostytrix.fletcherstrestle.client.guide;

import net.frostytrix.fletcherstrestle.client.ClientArcheryData;
import net.frostytrix.fletcherstrestle.network.SpendSkillPacket;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** The in-game Fletcher's Guide (Phase 4): chapters -> sub-chapters -> pages. */
public class FletcherGuideScreen extends Screen {

    private static final String WIKI_URL =
            "https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge/wiki";

    private static final int WIDTH = 256;
    private static final int HEIGHT = 200;

    // Parchment palette — light page, near-black ink for strong contrast.
    private static final int PAGE = 0xFFF6EDD6;
    private static final int PAGE_EDGE = 0xFF8A6A3A;
    private static final int INK = 0xFF201509;
    private static final int INK_HEAD = 0xFF7A2E0A;
    private static final int INK_FAINT = 0xFF574730;
    private static final int SLOT_BG = 0xFFCBB78A;

    private enum View { CHAPTERS, SUBS, PAGES }

    private final List<GuideChapter> chapters = GuideContent.chapters();
    private int left;
    private int top;

    private View view = View.CHAPTERS;
    private int chapterIdx = 0;
    private int subIdx = 0;
    private int pageIdx = 0;

    private final Button[] skillButtons = new Button[ArcherySkill.values().length];

    public FletcherGuideScreen() {
        super(Component.translatable("gui.fletcherstrestle.guide.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        int cx = this.left + WIDTH / 2;

        // Back / wiki on the footer row.
        if (this.view != View.CHAPTERS) {
            this.addRenderableWidget(Button.builder(Component.literal("←"), b -> goBack())
                    .bounds(this.left + 8, this.top + HEIGHT - 24, 20, 18).build());
        }
        this.addRenderableWidget(Button.builder(Component.translatable("gui.fletcherstrestle.guide.open_wiki"),
                        b -> openWiki())
                .bounds(this.left + WIDTH - 88, this.top + HEIGHT - 24, 80, 18).build());

        switch (this.view) {
            case CHAPTERS -> {
                for (int i = 0; i < chapters.size(); i++) {
                    final int idx = i;
                    this.addRenderableWidget(Button.builder(chapters.get(i).title(), b -> openChapter(idx))
                            .bounds(cx - 90, this.top + 30 + i * 21, 180, 18).build());
                }
            }
            case SUBS -> {
                List<GuideSubchapter> subs = chapters.get(chapterIdx).subchapters();
                for (int i = 0; i < subs.size(); i++) {
                    final int idx = i;
                    this.addRenderableWidget(Button.builder(subs.get(i).title(), b -> openSub(idx))
                            .bounds(cx - 90, this.top + 30 + i * 21, 180, 18).build());
                }
            }
            case PAGES -> {
                GuideSubchapter sub = currentSub();
                if (sub.skillTree()) {
                    buildSkillButtons();
                } else if (sub.pages().size() > 1) {
                    // Page arrows on the left/right edges (clear of the footer/wiki button).
                    int arrowY = this.top + HEIGHT / 2 - 9;
                    this.addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (pageIdx > 0) pageIdx--; })
                            .bounds(this.left + 4, arrowY, 16, 18).build());
                    this.addRenderableWidget(Button.builder(Component.literal(">"),
                                    b -> { if (pageIdx < sub.pages().size() - 1) pageIdx++; })
                            .bounds(this.left + WIDTH - 20, arrowY, 16, 18).build());
                }
            }
        }
    }

    private void buildSkillButtons() {
        ArcherySkill[] skills = ArcherySkill.values();
        int rowTop = this.top + 70;
        for (int i = 0; i < skills.length; i++) {
            final int branch = i;
            this.skillButtons[i] = Button.builder(Component.literal("+"),
                            b -> PacketDistributor.sendToServer(new SpendSkillPacket(branch)))
                    .bounds(this.left + WIDTH - 40, rowTop + i * 24, 20, 20).build();
            this.addRenderableWidget(this.skillButtons[i]);
        }
    }

    private void goBack() {
        if (this.view == View.PAGES) {
            this.view = View.SUBS;
        } else if (this.view == View.SUBS) {
            this.view = View.CHAPTERS;
        }
        rebuild();
    }

    private void openChapter(int idx) { this.chapterIdx = idx; this.view = View.SUBS; rebuild(); }
    private void openSub(int idx) { this.subIdx = idx; this.pageIdx = 0; this.view = View.PAGES; rebuild(); }

    private void openWiki() {
        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(WIKI_URL);
            }
            this.minecraft.setScreen(this);
        }, WIKI_URL, true));
    }

    private GuideSubchapter currentSub() {
        return chapters.get(chapterIdx).subchapters().get(subIdx);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        // Parchment page.
        g.fill(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, PAGE);
        g.fill(this.left, this.top, this.left + WIDTH, this.top + 1, PAGE_EDGE);
        g.fill(this.left, this.top + HEIGHT - 1, this.left + WIDTH, this.top + HEIGHT, PAGE_EDGE);
        g.fill(this.left, this.top, this.left + 1, this.top + HEIGHT, PAGE_EDGE);
        g.fill(this.left + WIDTH - 1, this.top, this.left + WIDTH, this.top + HEIGHT, PAGE_EDGE);

        Component header = switch (this.view) {
            case CHAPTERS -> Component.translatable("gui.fletcherstrestle.guide.title");
            case SUBS -> chapters.get(chapterIdx).title();
            case PAGES -> currentSub().title();
        };
        drawCenteredNoShadow(g, header, this.left + WIDTH / 2, this.top + 12, INK_HEAD);
        g.fill(this.left + 16, this.top + 24, this.left + WIDTH - 16, this.top + 25, PAGE_EDGE);

        if (this.view == View.PAGES) {
            GuideSubchapter sub = currentSub();
            if (sub.skillTree()) {
                renderSkillPage(g);
            } else {
                renderPage(g, sub.pages().get(pageIdx));
                if (sub.pages().size() > 1) {
                    drawCenteredNoShadow(g, Component.literal((pageIdx + 1) + "/" + sub.pages().size()),
                            this.left + WIDTH / 2, this.top + HEIGHT - 20, INK_FAINT);
                }
            }
        }

        // Render widgets directly. Do NOT call super.render(), which calls
        // renderBackground() again and would re-draw the dim overlay over the
        // parchment (turning it gray).
        for (net.minecraft.client.gui.components.Renderable r : this.renderables) {
            r.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void renderPage(GuiGraphics g, GuidePage page) {
        int x = this.left + 18;
        int y = this.top + 32;
        int wrap = WIDTH - 36;
        for (GuideElement el : page.elements()) {
            switch (el.type()) {
                case HEADING -> {
                    g.drawString(this.font, el.text(), x, y, INK_HEAD, false);
                    y += 13;
                }
                case TEXT -> {
                    for (FormattedCharSequence line : this.font.split(el.text(), wrap)) {
                        g.drawString(this.font, line, x, y, INK, false);
                        y += 10;
                    }
                    y += 4;
                }
                case ITEM -> {
                    g.renderItem(el.icon(), x, y);
                    int ty = y + 1;
                    for (FormattedCharSequence line : this.font.split(el.text(), wrap - 22)) {
                        g.drawString(this.font, line, x + 22, ty, INK, false);
                        ty += 10;
                    }
                    y = Math.max(y + 20, ty) + 4;
                }
                case RECIPE -> y = renderRecipe(g, el.icon(), x, y) + 6;
                case ASSEMBLY -> y = renderAssembly(g, el, x, y) + 6;
            }
        }
    }

    private void drawCenteredNoShadow(GuiGraphics g, Component c, int cx, int y, int color) {
        g.drawString(this.font, c, cx - this.font.width(c) / 2, y, color, false);
    }

    /** Draws a centered inputs -> result strip (e.g. limbs + riser + string -> bow). */
    private int renderAssembly(GuiGraphics g, GuideElement el, int unusedX, int y) {
        int n = el.stacks().size();
        int totalW = n * 18 + 14 + 16;        // inputs + arrow + result
        int sx = this.left + (WIDTH - totalW) / 2;
        for (ItemStack in : el.stacks()) {
            g.fill(sx, y, sx + 16, y + 16, SLOT_BG);
            g.renderItem(in, sx, y);
            sx += 18;
        }
        g.drawString(this.font, "→", sx + 2, y + 4, INK, false);
        sx += 14;
        g.fill(sx, y, sx + 16, y + 16, SLOT_BG);
        g.renderItem(el.icon(), sx, y);
        return y + 20;
    }

    /** Draws a centered crafting recipe for {@code result}; returns the y below it. */
    private int renderRecipe(GuiGraphics g, ItemStack result, int unusedX, int y) {
        CraftingRecipe recipe = findCrafting(result);
        // Layout: 3x3 grid (54) + arrow (~20) + result slot (16) = 90 wide.
        int totalW = 90;
        int x = this.left + (WIDTH - totalW) / 2;
        if (recipe == null) {
            g.fill(x + 74, y + 16, x + 90, y + 32, SLOT_BG);
            g.renderItem(result, x + 74, y + 16);
            return y + 40;
        }
        // 3x3 grid.
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = x + c * 18;
                int sy = y + r * 18;
                g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
            }
        }
        List<Ingredient> ings = recipe.getIngredients();
        int w = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        for (int i = 0; i < ings.size(); i++) {
            ItemStack[] items = ings.get(i).getItems();
            if (items.length == 0) {
                continue;
            }
            int r = recipe instanceof ShapedRecipe ? i / w : i / 3;
            int c = recipe instanceof ShapedRecipe ? i % w : i % 3;
            g.renderItem(items[0], x + c * 18, y + r * 18);
        }
        // Arrow + result.
        g.drawString(this.font, "→", x + 58, y + 20, INK, false);
        HolderLookup.Provider reg = this.minecraft.level.registryAccess();
        g.fill(x + 74, y + 16, x + 90, y + 32, SLOT_BG);
        g.renderItem(recipe.getResultItem(reg), x + 74, y + 16);
        return y + 56;
    }

    private CraftingRecipe findCrafting(ItemStack result) {
        if (this.minecraft.level == null) {
            return null;
        }
        HolderLookup.Provider reg = this.minecraft.level.registryAccess();
        for (var holder : this.minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (holder.value().getResultItem(reg).is(result.getItem())) {
                return holder.value();
            }
        }
        return null;
    }

    private void renderSkillPage(GuiGraphics g) {
        int x = this.left + 18;
        int y = this.top + 32;
        for (FormattedCharSequence line : this.font.split(
                Component.translatable("gui.fletcherstrestle.guide.skills.b1"), WIDTH - 36)) {
            g.drawString(this.font, line, x, y, INK, false);
            y += 10;
        }
        int points = ClientArcheryData.pointsAvailable();
        g.drawString(this.font, Component.translatable("gui.fletcherstrestle.skill_points", points),
                x, this.top + 56, INK_HEAD, false);

        ArcherySkill[] skills = ArcherySkill.values();
        int rowTop = this.top + 70;
        for (int i = 0; i < skills.length; i++) {
            int rank = ClientArcheryData.rank(skills[i]);
            if (this.skillButtons[i] != null) {
                this.skillButtons[i].active = points > 0 && rank < ArcherySkill.MAX_RANK;
            }
            int rowY = rowTop + i * 24;
            g.drawString(this.font, branchName(skills[i]), x, rowY + 1, INK, false);
            g.drawString(this.font, rank + "/" + ArcherySkill.MAX_RANK, x, rowY + 11, INK_FAINT, false);
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
    public boolean isPauseScreen() {
        return false;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new FletcherGuideScreen());
    }
}
