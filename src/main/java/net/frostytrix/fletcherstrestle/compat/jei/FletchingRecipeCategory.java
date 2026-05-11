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
import net.frostytrix.fletcherstrestle.recipe.ModularWeaponRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class FletchingRecipeCategory implements IRecipeCategory<ModularWeaponRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "modular_weapon_assembly");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");

    // This creates the "Type" identifier JEI uses internally
    public static final RecipeType<ModularWeaponRecipe> FLETCHING_TYPE = new RecipeType<>(UID, ModularWeaponRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FletchingRecipeCategory(IGuiHelper helper) {
        // We cut out a 150x60 rectangle from your GUI texture to use as the background
        this.background = helper.createDrawable(TEXTURE, 10, 10, 150, 60);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.FLETCHING_TABLE));
    }

    @Override
    public RecipeType<ModularWeaponRecipe> getRecipeType() {
        return FLETCHING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Fletching"); // Or use Component.translatable("block.fletcherstrestle.fletching_trestle")
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
    public void setRecipe(IRecipeLayoutBuilder builder, ModularWeaponRecipe recipe, IFocusGroup focuses) {
        // JEI offsets the coordinates by exactly -10, -10 based on our background cut-out.

        // 1. Riser Slot (Menu was 21, 35 -> JEI is 11, 25)
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 25)
                .addIngredients(recipe.getRiser());

        // 2. Top Limb Slot (Menu was 45, 17 -> JEI is 35, 7)
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 7)
                .addIngredients(recipe.getLimbs());

        // 3. Bottom Limb Slot (Menu was 45, 53 -> JEI is 35, 43)
        builder.addSlot(RecipeIngredientRole.INPUT, 35, 43)
                .addIngredients(recipe.getLimbs());

        // 4. String Slot (Menu was 69, 35 -> JEI is 59, 25)
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 25)
                .addIngredients(recipe.getString());

        // 5. Output Slot (Menu was 124, 35 -> JEI is 114, 25)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 25)
                .addItemStack(recipe.getResultItem(null));
    }
}