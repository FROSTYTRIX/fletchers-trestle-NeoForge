package net.frostytrix.fletcherstrestle.client.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One renderable piece of a guide page: a heading, a paragraph, an item
 * showcase (icon + label), or a crafting recipe (looked up by its result).
 */
public record GuideElement(Type type, Component text, ItemStack icon) {

    public enum Type { HEADING, TEXT, ITEM, RECIPE }

    public static GuideElement heading(String key) {
        return new GuideElement(Type.HEADING, Component.translatable(key), ItemStack.EMPTY);
    }

    public static GuideElement text(String key) {
        return new GuideElement(Type.TEXT, Component.translatable(key), ItemStack.EMPTY);
    }

    public static GuideElement item(ItemStack icon, String key) {
        return new GuideElement(Type.ITEM, Component.translatable(key), icon);
    }

    /** A crafting-recipe widget; {@code result} is the recipe output to look up. */
    public static GuideElement recipe(ItemStack result) {
        return new GuideElement(Type.RECIPE, Component.empty(), result);
    }
}
