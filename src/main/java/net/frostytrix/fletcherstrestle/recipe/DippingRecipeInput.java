package net.frostytrix.fletcherstrestle.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record DippingRecipeInput(ItemStack item, FluidStack fluid) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? item : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}