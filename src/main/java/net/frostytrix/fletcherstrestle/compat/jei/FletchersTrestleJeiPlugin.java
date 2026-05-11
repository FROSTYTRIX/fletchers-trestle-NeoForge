package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.recipe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class FletchersTrestleJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FletchingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ArrowRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SteamingRecipeCategory(registration.getJeiHelpers().getGuiHelper()), // Added Steaming
                new ShavingRecipeCategory(registration.getJeiHelpers().getGuiHelper())   // Added Shaving
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<ModularWeaponRecipe> bowRecipes = recipeManager.getAllRecipesFor(ModRecipes.MODULAR_WEAPON_TYPE.get())
                .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList();
        registration.addRecipes(FletchingRecipeCategory.FLETCHING_TYPE, bowRecipes);

        List<ModularArrowRecipe> arrowRecipes = recipeManager.getAllRecipesFor(ModRecipes.MODULAR_ARROW_TYPE.get())
                .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList();
        registration.addRecipes(ArrowRecipeCategory.ARROW_TYPE, arrowRecipes);

        // Load Steaming Recipes
        List<SteamingRecipe> steamingRecipes = recipeManager.getAllRecipesFor(ModRecipes.STEAMING_TYPE.get())
                .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList();
        registration.addRecipes(SteamingRecipeCategory.STEAMING_TYPE, steamingRecipes);

        // Load Shaving Recipes
        List<ShavingHorseRecipe> shavingRecipes = recipeManager.getAllRecipesFor(ModRecipes.SHAVING_TYPE.get())
                .stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList();
        registration.addRecipes(ShavingRecipeCategory.SHAVING_TYPE, shavingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.FLETCHING_TABLE), FletchingRecipeCategory.FLETCHING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.FLETCHING_TABLE), ArrowRecipeCategory.ARROW_TYPE);

        // Steam Box Catalyst
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STEAM_BOX.get()), SteamingRecipeCategory.STEAMING_TYPE);

        // Shaving Horse Catalysts (Both the block and the tool!)
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SHAVING_HORSE.get()), ShavingRecipeCategory.SHAVING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.DRAWKNIFE.get()), ShavingRecipeCategory.SHAVING_TYPE);
    }
}