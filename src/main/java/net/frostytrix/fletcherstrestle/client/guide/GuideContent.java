package net.frostytrix.fletcherstrestle.client.guide;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * In-code guide chapters (Phase 4 foundation). Kept as a Java registry for
 * now; a datapack-driven loader can replace this later without changing the
 * screen. Text is translatable so it localises to en/fr.
 */
public final class GuideContent {
    private GuideContent() {
    }

    private static final String P = "gui.fletcherstrestle.guide.";

    public static List<GuideChapter> chapters() {
        return List.of(
                new GuideChapter(P + "getting_started.title",
                        new ItemStack(ModItems.FLETCHER_GUIDE.get()),
                        List.of(
                                GuideElement.text(P + "getting_started.b1"),
                                GuideElement.item(new ItemStack(ModItems.DRAWKNIFE.get()), P + "getting_started.drawknife"),
                                GuideElement.text(P + "getting_started.b2"))),

                new GuideChapter(P + "woodworking.title",
                        new ItemStack(ModBlocks.STEAM_BOX.get()),
                        List.of(
                                GuideElement.heading(P + "woodworking.h1"),
                                GuideElement.item(new ItemStack(ModBlocks.SHAVING_HORSE.get()), P + "woodworking.shave"),
                                GuideElement.item(new ItemStack(ModBlocks.STEAM_BOX.get()), P + "woodworking.steam"),
                                GuideElement.item(new ItemStack(ModItems.MODULAR_BOW.get()), P + "woodworking.fletch"))),

                new GuideChapter(P + "arrows.title",
                        new ItemStack(ModItems.MODULAR_ARROW.get()),
                        List.of(
                                GuideElement.text(P + "arrows.b1"),
                                GuideElement.item(new ItemStack(ModBlocks.DIPPING_VAT.get()), P + "arrows.dip"))),

                new GuideChapter(P + "crossbow.title",
                        new ItemStack(ModBlocks.CROSSBOW_BENCH.get()),
                        List.of(
                                GuideElement.text(P + "crossbow.b1"),
                                GuideElement.item(new ItemStack(ModItems.MECHANICAL_TRIGGER.get()), P + "crossbow.trigger"),
                                GuideElement.item(new ItemStack(Items.SPYGLASS), P + "crossbow.scope"),
                                GuideElement.item(new ItemStack(ModItems.MAGAZINE.get()), P + "crossbow.magazine"))),

                new GuideChapter(P + "skills.title",
                        new ItemStack(Items.EXPERIENCE_BOTTLE),
                        List.of(GuideElement.text(P + "skills.b1")),
                        true),

                new GuideChapter(P + "eagles.title",
                        new ItemStack(ModItems.EAGLE_SPAWN_EGG.get()),
                        List.of(
                                GuideElement.text(P + "eagles.b1"),
                                GuideElement.item(new ItemStack(ModItems.EAGLE_WHISTLE.get()), P + "eagles.whistle")))
        );
    }
}
