package net.frostytrix.fletcherstrestle.component;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The maker's stamp: who built a weapon.
 *
 * <p>Set when a weapon is finished at the Fletching Table, and on every weapon a
 * Fletcher sells. Restringing, retuning and the Crossbow Bench all keep it.</p>
 */
public final class MakersStamp {
    private MakersStamp() {
    }

    /** Stored instead of a player name on weapons a Fletcher villager sells. */
    public static final String VILLAGE = "#village";

    /** Adds the "Made by" line, if the weapon has a maker. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        String maker = stack.get(ModDataComponents.CRAFTED_BY.get());
        if (maker == null || maker.isEmpty()) {
            return;
        }
        Component name = VILLAGE.equals(maker)
                ? Component.translatable("gui.fletcherstrestle.made_by_village")
                : Component.literal(maker);
        tooltip.add(Component.translatable("gui.fletcherstrestle.made_by", name)
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
