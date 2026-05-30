package net.frostytrix.fletcherstrestle.client.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A guide chapter: a title, an icon for the chapter list, and its content
 * elements. {@code skillTree} marks the special interactive Archery Skills
 * chapter, which the screen renders with its spend UI instead of elements.
 */
public record GuideChapter(String titleKey, ItemStack icon, List<GuideElement> elements, boolean skillTree) {

    public GuideChapter(String titleKey, ItemStack icon, List<GuideElement> elements) {
        this(titleKey, icon, elements, false);
    }

    public Component title() {
        return Component.translatable(titleKey);
    }
}
