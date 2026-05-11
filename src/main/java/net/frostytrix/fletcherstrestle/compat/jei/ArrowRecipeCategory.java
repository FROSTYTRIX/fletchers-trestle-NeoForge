package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.recipe.ModularArrowRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArrowRecipeCategory implements IRecipeCategory<ModularArrowRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "modular_arrow_assembly");
    public static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table_arrow.png");

    public static final RecipeType<ModularArrowRecipe> ARROW_TYPE = new RecipeType<>(UID, ModularArrowRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ArrowRecipeCategory(IGuiHelper helper) {
        // Cut out the same 150x60 rectangle, but from your Arrow tab texture!
        this.background = helper.createDrawable(ARROW_TEXTURE, 10, 10, 150, 60);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.FLETCHING_TABLE));
    }

    @Override
    public RecipeType<ModularArrowRecipe> getRecipeType() {
        return ARROW_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Fletching (Arrows)");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ModularArrowRecipe recipe, IFocusGroup focuses) {
        // Menu coords offset by -10, -10

        // 1. Arrow Head (Menu was 66, 17 -> JEI is 56, 7)
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 7)
                .addIngredients(recipe.getHead());

        // 2. Arrow Shaft (Menu was 48, 35 -> JEI is 38, 25)
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 25)
                .addIngredients(recipe.getShaft());

        // 3. Arrow Fletching (Menu was 30, 53 -> JEI is 20, 43)
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 43)
                .addIngredients(recipe.getFletching());

        // 4. Output Slot (Menu was 124, 35 -> JEI is 114, 25)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 25)
                .addItemStack(recipe.getResultItem(null).copyWithCount(4)); // Show 4 arrows in JEI output
    }
}