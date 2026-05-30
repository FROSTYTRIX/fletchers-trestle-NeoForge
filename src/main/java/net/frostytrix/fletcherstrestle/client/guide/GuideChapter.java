package net.frostytrix.fletcherstrestle.client.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A top-level chapter: a title, an icon, and its sub-chapters. */
public record GuideChapter(String titleKey, ItemStack icon, List<GuideSubchapter> subchapters) {

    public Component title() {
        return Component.translatable(titleKey);
    }
}
