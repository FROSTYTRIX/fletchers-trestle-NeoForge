package net.frostytrix.fletcherstrestle.client.guide;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One renderable piece of a guide page.
 *
 * <ul>
 *   <li>HEADING / TEXT — text.</li>
 *   <li>ITEM — an icon plus a label.</li>
 *   <li>RECIPE — a crafting recipe, looked up by its result ({@code icon}).</li>
 *   <li>ASSEMBLY — an inputs -&gt; result strip for non-crafting builds (bow,
 *       arrow, crossbow); {@code stacks} are the inputs, {@code icon} the
 *       result.</li>
 * </ul>
 */
public record GuideElement(Type type, Component text, ItemStack icon, List<ItemStack> stacks) {

    public enum Type { HEADING, TEXT, ITEM, RECIPE, ASSEMBLY }

    public static GuideElement heading(String key) {
        return new GuideElement(Type.HEADING, Component.translatable(key), ItemStack.EMPTY, List.of());
    }

    public static GuideElement text(String key) {
        return new GuideElement(Type.TEXT, Component.translatable(key), ItemStack.EMPTY, List.of());
    }

    public static GuideElement item(ItemStack icon, String key) {
        return new GuideElement(Type.ITEM, Component.translatable(key), icon, List.of());
    }

    public static GuideElement recipe(ItemStack result) {
        return new GuideElement(Type.RECIPE, Component.empty(), result, List.of());
    }

    /** An inputs -> result assembly strip (for station/bench builds). */
    public static GuideElement assembly(ItemStack result, ItemStack... inputs) {
        return new GuideElement(Type.ASSEMBLY, Component.empty(), result, List.of(inputs));
    }
}
