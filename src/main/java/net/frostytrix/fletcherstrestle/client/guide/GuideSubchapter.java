package net.frostytrix.fletcherstrestle.client.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A sub-chapter: a titled, icon'd group of one or more pages. {@code skillTree}
 * marks the interactive Archery Skills sub-chapter.
 */
public record GuideSubchapter(String titleKey, ItemStack icon, List<GuidePage> pages, boolean skillTree) {

    public GuideSubchapter(String titleKey, ItemStack icon, List<GuidePage> pages) {
        this(titleKey, icon, pages, false);
    }

    public Component title() {
        return Component.translatable(titleKey);
    }
}
