package net.frostytrix.fletcherstrestle.client.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One renderable piece of a guide chapter: a heading, a paragraph, or an
 * item showcase (icon + label). Kept deliberately small; richer element
 * types (live recipe widgets, entity viewer) can be added later.
 */
public record GuideElement(Type type, Component text, ItemStack icon) {

    public enum Type { HEADING, TEXT, ITEM }

    public static GuideElement heading(String key) {
        return new GuideElement(Type.HEADING, Component.translatable(key), ItemStack.EMPTY);
    }

    public static GuideElement text(String key) {
        return new GuideElement(Type.TEXT, Component.translatable(key), ItemStack.EMPTY);
    }

    public static GuideElement item(ItemStack icon, String key) {
        return new GuideElement(Type.ITEM, Component.translatable(key), icon);
    }
}
