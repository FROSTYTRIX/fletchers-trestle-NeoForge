package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.recipe.SteamingRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

public class SteamingRecipeCategory implements IRecipeCategory<SteamingRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming");
    public static final RecipeType<SteamingRecipe> STEAMING_TYPE = new RecipeType<>(UID, SteamingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public SteamingRecipeCategory(IGuiHelper helper) {
        // Made the background slightly taller (60px) to fit everything comfortably
        this.background = helper.createBlankDrawable(110, 60);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.STEAM_BOX.get()));

        IDrawableStatic staticArrow = helper.createDrawable(ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png"), 79, 34, 24, 17);
        this.arrow = helper.createAnimatedDrawable(staticArrow, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public RecipeType<SteamingRecipe> getRecipeType() { return STEAMING_TYPE; }
    @Override public Component getTitle() { return Component.literal("Steam Box"); }
    @Override public IDrawable getBackground() { return this.background; }
    @Override public IDrawable getIcon() { return this.icon; }

    @Override
    public void draw(SteamingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Shifted the arrow down to Y: 22
        this.arrow.draw(guiGraphics, 45, 22);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SteamingRecipe recipe, IFocusGroup focuses) {
        // Fluid Slot (Hovering above the input)
        builder.addSlot(RecipeIngredientRole.CATALYST, 15, 2)
                .addFluidStack(Fluids.WATER, recipe.getWaterAmount())
                .setFluidRenderer(recipe.getWaterAmount(), false, 16, 16);

        // Input Slot (Middle Left)
        builder.addSlot(RecipeIngredientRole.INPUT, 15, 22)
                .addIngredients(recipe.getInput());

        // Output Slot (Right)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 22)
                .addItemStack(recipe.getResultItem(null));

        // HEAT CATALYST (Hovering below the arrow)
        // This puts a campfire icon underneath the process so players know it needs heat!
        builder.addSlot(RecipeIngredientRole.CATALYST, 49, 42)
                .addItemStack(new ItemStack(Items.CAMPFIRE));
    }
}