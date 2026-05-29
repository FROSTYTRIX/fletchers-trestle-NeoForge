package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CrossbowBenchRecipeCategory implements IRecipeCategory<CrossbowBenchJeiRecipe> {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "crossbow_bench");
    public static final RecipeType<CrossbowBenchJeiRecipe> TYPE =
            new RecipeType<>(UID, CrossbowBenchJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public CrossbowBenchRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(110, 46);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.CROSSBOW_BENCH.get()));
        this.arrow = helper.createDrawable(ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png"), 79, 34, 24, 17);
    }

    @Override
    public RecipeType<CrossbowBenchJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.fletcherstrestle.crossbow_bench");
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
    public void draw(CrossbowBenchJeiRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 50, 15);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrossbowBenchJeiRecipe recipe, IFocusGroup focuses) {
        int y = 4;
        for (ItemStack input : recipe.inputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 15, y).addItemStack(input);
            y += 20;
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 85, 14).addItemStack(recipe.output());
    }
}
