package net.frostytrix.fletcherstrestle.client.guide;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * In-code guide content (Phase 4): Chapters -> Sub-chapters -> Pages. Kept as
 * a Java registry for now; a datapack loader can replace it later without
 * touching the screen.
 */
public final class GuideContent {
    private GuideContent() {
    }

    private static final String P = "gui.fletcherstrestle.guide.";

    private static ItemStack of(net.minecraft.world.level.ItemLike item) {
        return new ItemStack(item);
    }

    /** A modular arrow with a glass-vial head, so it renders as a potion arrow. */
    private static ItemStack glassVialArrow() {
        ItemStack arrow = new ItemStack(ModItems.MODULAR_ARROW.get());
        arrow.set(ModDataComponents.ARROW_ASSEMBLY.get(), new ArrowAssembly("glass_vial", "oak", "feather"));
        return arrow;
    }

    public static List<GuideChapter> chapters() {
        return List.of(
                // --- Getting Started ---
                new GuideChapter(P + "getting_started.title", of(ModItems.FLETCHER_GUIDE.get()), List.of(
                        new GuideSubchapter(P + "sub.overview", of(ModItems.FLETCHER_GUIDE.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "getting_started.b1"),
                                        GuideElement.item(of(ModItems.DRAWKNIFE.get()), P + "getting_started.drawknife"),
                                        GuideElement.recipe(of(ModItems.DRAWKNIFE.get())),
                                        GuideElement.text(P + "getting_started.b2")))))),

                // --- Woodworking ---
                new GuideChapter(P + "woodworking.title", of(ModBlocks.STEAM_BOX.get()), List.of(
                        new GuideSubchapter(P + "sub.shaving_horse", of(ModBlocks.SHAVING_HORSE.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "woodworking.shave.desc"),
                                        GuideElement.recipe(of(ModBlocks.SHAVING_HORSE.get()))))),
                        new GuideSubchapter(P + "sub.steam_box", of(ModBlocks.STEAM_BOX.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "woodworking.steam.desc"),
                                        GuideElement.recipe(of(ModBlocks.STEAM_BOX.get()))))),
                        new GuideSubchapter(P + "sub.fletching_table", of(Items.FLETCHING_TABLE), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "woodworking.fletch.desc"),
                                        GuideElement.recipe(of(Items.FLETCHING_TABLE))),
                                GuidePage.of(
                                        GuideElement.heading(P + "woodworking.bow_example"),
                                        GuideElement.assembly(of(ModItems.MODULAR_BOW.get()),
                                                of(ModItems.PLIABLE_OAK_LIMB.get()), of(ModItems.PLIABLE_OAK_LIMB.get()),
                                                of(ModItems.WOOD_RISER.get()), of(Items.STRING))))))),

                // --- Arrows ---
                new GuideChapter(P + "arrows.title", of(ModItems.MODULAR_ARROW.get()), List.of(
                        new GuideSubchapter(P + "sub.modular_arrows", of(ModItems.MODULAR_ARROW.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "arrows.b1"),
                                        GuideElement.heading(P + "arrows.example"),
                                        GuideElement.assembly(of(ModItems.MODULAR_ARROW.get()),
                                                of(Items.FLINT), of(Items.STICK), of(Items.FEATHER))))),
                        new GuideSubchapter(P + "sub.potion_arrows", glassVialArrow(), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "arrows.dip.desc"),
                                        GuideElement.heading(P + "arrows.vat_craft"),
                                        GuideElement.recipe(of(ModBlocks.DIPPING_VAT.get()))),
                                GuidePage.of(
                                        GuideElement.heading(P + "arrows.vial_craft"),
                                        GuideElement.assembly(glassVialArrow(),
                                                of(Items.GLASS_BOTTLE), of(Items.STICK), of(Items.FEATHER))))))),

                // --- Crossbow Bench ---
                new GuideChapter(P + "crossbow.title", of(ModBlocks.CROSSBOW_BENCH.get()), List.of(
                        new GuideSubchapter(P + "sub.bench", of(ModBlocks.CROSSBOW_BENCH.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "crossbow.b1"),
                                        GuideElement.recipe(of(ModBlocks.CROSSBOW_BENCH.get()))),
                                GuidePage.of(
                                        GuideElement.item(of(ModItems.MECHANICAL_TRIGGER.get()), P + "crossbow.trigger"),
                                        GuideElement.heading(P + "crossbow.example"),
                                        GuideElement.assembly(of(ModItems.MODULAR_CROSSBOW.get()),
                                                of(ModItems.MODULAR_BOW.get()), of(ModItems.MECHANICAL_TRIGGER.get()))))),
                        new GuideSubchapter(P + "sub.attachments", of(Items.SPYGLASS), List.of(
                                GuidePage.of(
                                        GuideElement.item(of(Items.SPYGLASS), P + "crossbow.scope"),
                                        GuideElement.item(of(ModItems.MAGAZINE.get()), P + "crossbow.magazine"),
                                        GuideElement.recipe(of(ModItems.MAGAZINE.get()))))))),

                // --- Archery Skills (interactive) ---
                new GuideChapter(P + "skills.title", of(Items.EXPERIENCE_BOTTLE), List.of(
                        new GuideSubchapter(P + "sub.skill_tree", of(Items.EXPERIENCE_BOTTLE),
                                List.of(GuidePage.of(GuideElement.text(P + "skills.b1"))), true))),

                // --- Eagles ---
                new GuideChapter(P + "eagles.title", of(ModItems.EAGLE_SPAWN_EGG.get()), List.of(
                        new GuideSubchapter(P + "sub.taming", of(ModItems.EAGLE_SPAWN_EGG.get()), List.of(
                                GuidePage.of(GuideElement.text(P + "eagles.taming")))),
                        new GuideSubchapter(P + "sub.fetch_hunt", of(ModItems.MODULAR_ARROW.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "eagles.fetch"),
                                        GuideElement.text(P + "eagles.hunt")))),
                        new GuideSubchapter(P + "sub.perch", of(ModBlocks.EAGLE_PERCH.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "eagles.perch.desc"),
                                        GuideElement.recipe(of(ModBlocks.EAGLE_PERCH.get()))))),
                        new GuideSubchapter(P + "sub.nest", of(ModBlocks.EAGLE_NEST.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "eagles.nest.desc"),
                                        GuideElement.recipe(of(ModBlocks.EAGLE_NEST.get()))))),
                        new GuideSubchapter(P + "sub.whistle", of(ModItems.EAGLE_WHISTLE.get()), List.of(
                                GuidePage.of(
                                        GuideElement.text(P + "eagles.whistle.desc"),
                                        GuideElement.recipe(of(ModItems.EAGLE_WHISTLE.get())))))))
        );
    }
}
