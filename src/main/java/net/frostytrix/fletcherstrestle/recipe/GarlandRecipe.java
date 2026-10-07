package net.frostytrix.fletcherstrestle.recipe;

import net.frostytrix.fletcherstrestle.component.GarlandColours;
import net.frostytrix.fletcherstrestle.component.GarlandFeather;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.frostytrix.fletcherstrestle.tags.ModTags;

import java.util.ArrayList;
import java.util.List;

/**
 * One string plus seven feathers makes a garland. Strings are the
 * {@code fletcherstrestle:garland_strings} item tag; feathers are any item in the
 * {@code garland_feather} data map, which also gives their colour. Which feathers you use decides
 * the colours it hangs in, so the recipe has to read the grid rather than have a
 * fixed output: the same pattern vanilla uses for firework rockets.
 */
public class GarlandRecipe extends CustomRecipe {

    /** Feathers required per garland. */
    private static final int FEATHERS_NEEDED = 7;

    public GarlandRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The colour a feather dyes the bunting, from the garland_feather data map; null if it isn't one. */
    private static Integer colourOf(ItemStack stack) {
        GarlandFeather feather = stack.getItemHolder().getData(GarlandFeather.DATA_MAP);
        return feather != null ? feather.colour() : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int strings = 0;
        int feathers = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModTags.Items.GARLAND_STRINGS)) {
                strings++;
            } else if (colourOf(stack) != null) {
                feathers++;
            } else {
                return false;
            }
        }
        return strings == 1 && feathers == FEATHERS_NEEDED;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        List<Integer> colours = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Integer colour = colourOf(input.getItem(i));
            if (colour != null) {
                colours.add(colour);
            }
        }
        ItemStack result = new ItemStack(ModItems.GARLAND.get());
        result.set(ModDataComponents.GARLAND_COLOURS.get(), new GarlandColours(List.copyOf(colours)));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= FEATHERS_NEEDED + 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GARLAND_SERIALIZER.get();
    }
}
