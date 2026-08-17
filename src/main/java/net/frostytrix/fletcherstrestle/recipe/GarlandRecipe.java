package net.frostytrix.fletcherstrestle.recipe;

import net.frostytrix.fletcherstrestle.component.GarlandColours;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One string plus seven feathers makes a garland. Which feathers you use decides
 * the colours it hangs in, so the recipe has to read the grid rather than have a
 * fixed output: the same pattern vanilla uses for firework rockets.
 */
public class GarlandRecipe extends CustomRecipe {

    /** Feathers required per garland. */
    private static final int FEATHERS_NEEDED = 7;

    /** Feather item path to the colour it dyes the bunting. */
    private static final Map<String, Integer> FEATHER_COLOURS = new HashMap<>();

    static {
        FEATHER_COLOURS.put("feather", 0xF2F2F2);
        FEATHER_COLOURS.put("red_feather", 0xB02E26);
        FEATHER_COLOURS.put("blue_feather", 0x3C44AA);
        FEATHER_COLOURS.put("green_feather", 0x5E7C16);
        FEATHER_COLOURS.put("cyan_feather", 0x169C9C);
        FEATHER_COLOURS.put("light_gray_feather", 0x9D9D97);
        FEATHER_COLOURS.put("brown_feather", 0x835432);
    }

    public GarlandRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static Integer colourOf(ItemStack stack) {
        if (stack.is(Items.FEATHER)) {
            return FEATHER_COLOURS.get("feather");
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return FEATHER_COLOURS.get(id.getPath());
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
            if (stack.is(Items.STRING) || stack.is(ModItems.FLAX_STRING.get())) {
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
