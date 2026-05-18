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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
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
                new SteamingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ShavingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new DippingRecipeCategory(registration.getJeiHelpers().getGuiHelper())
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

        // --- LOAD DIPPING RECIPES (AVEC ÉCLATEMENT DYNAMIQUE) ---
        List<DippingRecipe> dippingRecipes = new ArrayList<>();
        for (var holder : recipeManager.getAllRecipesFor(ModRecipes.DIPPING_TYPE.get())) {
            DippingRecipe recipe = holder.value();

            // Si c'est notre recette générique de flèches (pas de potion fixe demandée et le résultat est une flèche)
            if (recipe.requiredPotion.isEmpty() && recipe.output.is(Items.TIPPED_ARROW)) {

                // On boucle sur toutes les potions du jeu
                BuiltInRegistries.POTION.holders().forEach(potionHolder -> {
                    String potionId = BuiltInRegistries.POTION.getKey(potionHolder.value()).toString();

                    if (!potionId.equals("minecraft:empty") && !potionId.equals("minecraft:water")) {

                        // On fabrique l'objet de sortie avec l'effet précis
                        ItemStack specificArrow = recipe.output.copy();
                        specificArrow.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(potionHolder));

                        // On ajoute une fausse recette FIXE dans JEI pour cette potion
                        dippingRecipes.add(new DippingRecipe(
                                recipe.inputItem,
                                recipe.inputCount,
                                java.util.Optional.of(potionId), // On fixe la potion !
                                recipe.fluidAmount,
                                specificArrow
                        ));
                    }
                });
            } else {
                // Les recettes normales (comme la Pomme en Or) passent directement
                dippingRecipes.add(recipe);
            }
        }
        registration.addRecipes(DippingRecipeCategory.DIPPING_TYPE, dippingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.FLETCHING_TABLE), FletchingRecipeCategory.FLETCHING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.FLETCHING_TABLE), ArrowRecipeCategory.ARROW_TYPE);

        // Steam Box Catalyst
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STEAM_BOX.get()), SteamingRecipeCategory.STEAMING_TYPE);

        // Shaving Horse Catalysts
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SHAVING_HORSE.get()), ShavingRecipeCategory.SHAVING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.DRAWKNIFE.get()), ShavingRecipeCategory.SHAVING_TYPE);

        // Dipping Vat Catalyst
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DIPPING_VAT.get()), DippingRecipeCategory.DIPPING_TYPE);
    }
}