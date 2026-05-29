package net.frostytrix.fletcherstrestle.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A display-only recipe for the Crossbow Bench JEI category. The bench's
 * transformations are menu-driven (not Minecraft recipes), so these POJOs
 * exist purely to show players what the bench can do: bow + trigger ->
 * crossbow, and crossbow + attachment -> attached crossbow.
 */
public record CrossbowBenchJeiRecipe(List<ItemStack> inputs, ItemStack output) {
}
