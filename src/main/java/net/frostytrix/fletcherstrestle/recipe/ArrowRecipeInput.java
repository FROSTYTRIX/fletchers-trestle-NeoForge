package net.frostytrix.fletcherstrestle.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ArrowRecipeInput(ItemStack head, ItemStack shaft, ItemStack fletching) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> head;
            case 1 -> shaft;
            case 2 -> fletching;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return head.isEmpty() && shaft.isEmpty() && fletching.isEmpty();
    }
}