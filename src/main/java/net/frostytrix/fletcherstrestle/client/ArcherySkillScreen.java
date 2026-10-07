package net.frostytrix.fletcherstrestle.client;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.progression.ArcherySkills;
import net.frostytrix.fletcherstrestle.network.SpendSkillPacket;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Simple archery skill-tree screen. Reads the synced
 * {@link ClientArcheryData} and lets the player spend points across the three
 * branches via a {@link SpendSkillPacket} to the server.
 */
public class ArcherySkillScreen extends Screen {

    private static final int WIDTH = 230;
    private static final int HEIGHT = 176;
    private static final int ROW_H = 42;
    private static final int FIRST_ROW = 38;

    private static final Component PLUS = Component.literal("+");
    private static final Component STAR = Component.literal("\u2605");

    private int left;
    private int top;
    private final Button[] buttons = new Button[ArcherySkill.values().length];

    public ArcherySkillScreen() {
        super(Component.translatable("gui.fletcherstrestle.skill_screen_title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;

        ArcherySkill[] skills = ArcherySkill.values();
        for (int i = 0; i < skills.length; i++) {
            final ArcherySkill skill = skills[i];
            int rowY = this.top + FIRST_ROW + i * ROW_H;
            // One button per branch: "+" while ranking up, then the star that buys
            // the capstone once the branch is maxed.
            this.buttons[i] = Button.builder(PLUS, b -> spend(skill))
                    .bounds(this.left + WIDTH - 30, rowY + 4, 20, 20)
                    .build();
            this.addRenderableWidget(this.buttons[i]);
        }
    }

    private void spend(ArcherySkill skill) {
        boolean capstone = ClientArcheryData.rank(skill) >= ArcherySkill.MAX_RANK;
        PacketDistributor.sendToServer(new SpendSkillPacket(skill.ordinal(), capstone));
        // The server validates, applies, and syncs ClientArcheryData back.
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Plain dim overlay. Not calling super, which would apply 1.21's
        // gaussian blur to the whole screen.
        g.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        // Flat fully-opaque panel so text renders crisp.
        g.fill(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, 0xFF1A130C);
        g.fill(this.left, this.top, this.left + WIDTH, this.top + 1, 0xFF5A4632);            // top border
        g.fill(this.left, this.top + HEIGHT - 1, this.left + WIDTH, this.top + HEIGHT, 0xFF5A4632);
        g.fill(this.left, this.top, this.left + 1, this.top + HEIGHT, 0xFF5A4632);           // left border
        g.fill(this.left + WIDTH - 1, this.top, this.left + WIDTH, this.top + HEIGHT, 0xFF5A4632);

        int points = ClientArcheryData.pointsAvailable();
        ArcherySkills owned = ClientArcheryData.skills();

        // Title + points (shadowed for legibility).
        g.drawCenteredString(this.font, this.title, this.left + WIDTH / 2, this.top + 8, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("gui.fletcherstrestle.skill_points", points),
                this.left + WIDTH / 2, this.top + 20, 0xFFD700);

        List<Component> tooltip = null;
        ArcherySkill[] skills = ArcherySkill.values();
        for (int i = 0; i < skills.length; i++) {
            ArcherySkill skill = skills[i];
            int rank = ClientArcheryData.rank(skill);
            boolean maxed = rank >= ArcherySkill.MAX_RANK;
            CapstoneState state = capstoneState(skill, owned, points);

            Button button = this.buttons[i];
            button.setMessage(maxed ? STAR : PLUS);
            button.active = maxed ? state == CapstoneState.AVAILABLE : points > 0;
            button.visible = state != CapstoneState.OWNED;

            int rowY = this.top + FIRST_ROW + i * ROW_H;
            g.drawString(this.font, branchName(skill), this.left + 12, rowY, 0xFFFFFF, true);
            g.drawString(this.font, Component.literal(rank + "/" + ArcherySkill.MAX_RANK + "  ")
                    .append(effectText(skill, rank)), this.left + 12, rowY + 11, 0xB0B0B0, true);

            Component capstone = Component.literal("\u2605 ").append(
                    Component.translatable("gui.fletcherstrestle.capstone." + skill.capstoneId()));
            g.drawString(this.font, capstone, this.left + 12, rowY + 23, state.colour, true);

            boolean overLine = mouseX >= this.left + 8 && mouseX < this.left + WIDTH - 34
                    && mouseY >= rowY + 21 && mouseY < rowY + 33;
            if (overLine || (button.visible && button.isHovered() && maxed)) {
                tooltip = capstoneTooltip(skill, state, owned);
            }
        }

        // Render widgets directly: calling super.render() would re-draw the
        // dim overlay over the panel.
        for (net.minecraft.client.gui.components.Renderable r : this.renderables) {
            r.render(g, mouseX, mouseY, partialTick);
        }
        if (tooltip != null) {
            g.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /** Where a capstone stands for this archer, and the colour its line is drawn in. */
    private enum CapstoneState {
        OWNED(0xFFD700),
        AVAILABLE(0xFFFFFF),
        NOT_ENOUGH_POINTS(0x9A8A6A),
        AT_LIMIT(0x6A6A6A),
        LOCKED(0x5A5A5A);

        final int colour;

        CapstoneState(int colour) {
            this.colour = colour;
        }
    }

    private static CapstoneState capstoneState(ArcherySkill skill, ArcherySkills owned, int points) {
        if (owned.hasCapstone(skill)) return CapstoneState.OWNED;
        if (ClientArcheryData.rank(skill) < ArcherySkill.MAX_RANK) return CapstoneState.LOCKED;
        if (owned.capstoneCount() >= FletcherConfig.MAX_CAPSTONES.get()) return CapstoneState.AT_LIMIT;
        if (points < FletcherConfig.CAPSTONE_COST.get()) return CapstoneState.NOT_ENOUGH_POINTS;
        return CapstoneState.AVAILABLE;
    }

    private static List<Component> capstoneTooltip(ArcherySkill skill, CapstoneState state, ArcherySkills owned) {
        String id = skill.capstoneId();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.fletcherstrestle.capstone." + id).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("gui.fletcherstrestle.capstone." + id + ".desc").withStyle(ChatFormatting.GRAY));
        Component status = switch (state) {
            case OWNED -> Component.translatable("gui.fletcherstrestle.capstone.owned").withStyle(ChatFormatting.GREEN);
            case LOCKED -> Component.translatable("gui.fletcherstrestle.capstone.locked").withStyle(ChatFormatting.DARK_GRAY);
            case AT_LIMIT -> Component.translatable("gui.fletcherstrestle.capstone.limit",
                    FletcherConfig.MAX_CAPSTONES.get()).withStyle(ChatFormatting.RED);
            case NOT_ENOUGH_POINTS, AVAILABLE -> Component.translatable("gui.fletcherstrestle.capstone.cost",
                    FletcherConfig.CAPSTONE_COST.get(), owned.capstoneCount(),
                    FletcherConfig.MAX_CAPSTONES.get()).withStyle(ChatFormatting.YELLOW);
        };
        lines.add(status);
        return lines;
    }

    private static Component branchName(ArcherySkill skill) {
        return switch (skill) {
            case DRAW -> Component.translatable("gui.fletcherstrestle.skill_draw");
            case CRIT -> Component.translatable("gui.fletcherstrestle.skill_crit");
            case AIM -> Component.translatable("gui.fletcherstrestle.skill_aim");
        };
    }

    private static Component effectText(ArcherySkill skill, int rank) {
        return switch (skill) {
            case DRAW -> Component.translatable("gui.fletcherstrestle.skill_effect.draw",
                    String.format("%.2f", 1.0f - 0.02f * rank));
            case CRIT -> Component.translatable("gui.fletcherstrestle.skill_effect.crit",
                    (int) (0.03f * rank * 100));
            case AIM -> Component.translatable("gui.fletcherstrestle.skill_effect.aim",
                    String.format("%.2f", 1.0f - 0.03f * rank));
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
