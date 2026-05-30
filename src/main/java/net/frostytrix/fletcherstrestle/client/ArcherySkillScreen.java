package net.frostytrix.fletcherstrestle.client;

import net.frostytrix.fletcherstrestle.network.SpendSkillPacket;
import net.frostytrix.fletcherstrestle.progression.ArcherySkill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Simple archery skill-tree screen (Phase 2). Reads the synced
 * {@link ClientArcheryData} and lets the player spend points across the three
 * branches via a {@link SpendSkillPacket} to the server.
 */
public class ArcherySkillScreen extends Screen {

    private static final int WIDTH = 200;
    private static final int HEIGHT = 140;
    private static final int ROW_H = 28;

    private int left;
    private int top;
    private final Button[] plusButtons = new Button[ArcherySkill.values().length];

    public ArcherySkillScreen() {
        super(Component.translatable("gui.fletcherstrestle.skill_screen_title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;

        ArcherySkill[] skills = ArcherySkill.values();
        for (int i = 0; i < skills.length; i++) {
            final int branch = i;
            int rowY = this.top + 36 + i * ROW_H;
            this.plusButtons[i] = Button.builder(Component.literal("+"), b -> spend(branch))
                    .bounds(this.left + WIDTH - 30, rowY, 20, 20)
                    .build();
            this.addRenderableWidget(this.plusButtons[i]);
        }
    }

    private void spend(int branch) {
        PacketDistributor.sendToServer(new SpendSkillPacket(branch));
        // The server validates, applies, and syncs ClientArcheryData back.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        // Flat opaque panel so text renders crisp instead of over the live,
        // blurred world background.
        g.fill(this.left, this.top, this.left + WIDTH, this.top + HEIGHT, 0xF0100A06);
        g.fill(this.left, this.top, this.left + WIDTH, this.top + 1, 0xFF5A4632);            // top border
        g.fill(this.left, this.top + HEIGHT - 1, this.left + WIDTH, this.top + HEIGHT, 0xFF5A4632);
        g.fill(this.left, this.top, this.left + 1, this.top + HEIGHT, 0xFF5A4632);           // left border
        g.fill(this.left + WIDTH - 1, this.top, this.left + WIDTH, this.top + HEIGHT, 0xFF5A4632);

        int points = ClientArcheryData.pointsAvailable();

        // Title + points (shadowed for legibility).
        g.drawCenteredString(this.font, this.title, this.left + WIDTH / 2, this.top + 8, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.translatable("gui.fletcherstrestle.skill_points", points),
                this.left + WIDTH / 2, this.top + 20, 0xFFD700);

        ArcherySkill[] skills = ArcherySkill.values();
        for (int i = 0; i < skills.length; i++) {
            int rank = ClientArcheryData.rank(skills[i]);
            boolean maxed = rank >= ArcherySkill.MAX_RANK;
            this.plusButtons[i].active = points > 0 && !maxed;

            int rowY = this.top + 36 + i * ROW_H;
            g.drawString(this.font, branchName(skills[i]), this.left + 12, rowY + 1, 0xFFFFFF, true);
            g.drawString(this.font, rank + "/" + ArcherySkill.MAX_RANK + "  " + effectText(skills[i], rank),
                    this.left + 12, rowY + 12, 0xB0B0B0, true);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static Component branchName(ArcherySkill skill) {
        return switch (skill) {
            case DRAW -> Component.translatable("gui.fletcherstrestle.skill_draw");
            case CRIT -> Component.translatable("gui.fletcherstrestle.skill_crit");
            case AIM -> Component.translatable("gui.fletcherstrestle.skill_aim");
        };
    }

    private static String effectText(ArcherySkill skill, int rank) {
        return switch (skill) {
            case DRAW -> String.format("x%.2f draw", 1.0f - 0.02f * rank);
            case CRIT -> (int) (0.03f * rank * 100) + "% crit";
            case AIM -> String.format("x%.2f spread", 1.0f - 0.03f * rank);
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
