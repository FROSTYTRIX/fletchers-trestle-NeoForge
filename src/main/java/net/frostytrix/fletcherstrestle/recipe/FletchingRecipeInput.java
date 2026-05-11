package net.frostytrix.fletcherstrestle.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record FletchingRecipeInput(ItemStack riser, ItemStack topLimb, ItemStack bottomLimb, ItemStack string) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> riser;
            case 1 -> topLimb;
            case 2 -> bottomLimb;
            case 3 -> string;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return riser.isEmpty() && topLimb.isEmpty() && bottomLimb.isEmpty() && string.isEmpty();
    }
}