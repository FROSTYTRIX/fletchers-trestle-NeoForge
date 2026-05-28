package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.recipe.ShavingHorseRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ShavingRecipeCategory implements IRecipeCategory<ShavingHorseRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving");
    public static final RecipeType<ShavingHorseRecipe> SHAVING_TYPE = new RecipeType<>(UID, ShavingHorseRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public ShavingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(110, 40);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.SHAVING_HORSE.get()));
        this.arrow = helper.createDrawable(ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png"), 79, 34, 24, 17);
    }

    @Override
    public RecipeType<ShavingHorseRecipe> getRecipeType() {
        return SHAVING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Shaving Horse");
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
    public void draw(ShavingHorseRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 45, 12);

        // Draw the required clicks text just below the arrow!
        Component text = Component.literal(recipe.getShavesRequired() + "x Clicks");
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.font.width(text);
        guiGraphics.drawString(minecraft.font, text, 57 - (width / 2), 32, 0xFF808080, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShavingHorseRecipe recipe, IFocusGroup focuses) {
        // Input Log Slot (Left)
        builder.addSlot(RecipeIngredientRole.INPUT, 15, 12)
                .addIngredients(recipe.getInput());

        // Output Limb Slot (Right)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 12)
                .addItemStack(recipe.getResultItem(null));

        // Let's add a visual-only Drawknife floating above the arrow so players know what tool to use!
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(new ItemStack(ModItems.DRAWKNIFE.get()));
    }
}