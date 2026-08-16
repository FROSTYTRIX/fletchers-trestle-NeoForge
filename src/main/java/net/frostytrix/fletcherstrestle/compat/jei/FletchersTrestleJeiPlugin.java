package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
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
                new DippingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrossbowBenchRecipeCategory(registration.getJeiHelpers().getGuiHelper())
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

            // Generic potion-arrow recipes (no required_potion, output is the
            // generic "any tipped arrow / any modular potion arrow"): explode
            // into one synthetic recipe per vanilla potion so JEI indexes them
            // by their final item: searching "Jump Boost Arrow" finds the
            // jump-boost-specific variant.
            boolean isGenericTipped = recipe.requiredPotion().isEmpty() && recipe.output().is(Items.TIPPED_ARROW);
            boolean isGenericModular = recipe.requiredPotion().isEmpty() && recipe.output().is(ModItems.MODULAR_ARROW.get());

            if (isGenericTipped || isGenericModular) {
                BuiltInRegistries.POTION.holders().forEach(potionHolder -> {
                    String potionId = BuiltInRegistries.POTION.getKey(potionHolder.value()).toString();
                    if (potionId.equals("minecraft:empty") || potionId.equals("minecraft:water")) return;

                    ItemStack specificArrow = recipe.output().copy();
                    specificArrow.set(DataComponents.POTION_CONTENTS,
                            new net.minecraft.world.item.alchemy.PotionContents(potionHolder));
                    // For modular arrows, set a sample assembly so the model
                    // renders the head as a glass vial (otherwise JEI shows
                    // an "unfinished" arrow icon).
                    if (isGenericModular) {
                        specificArrow.set(ModDataComponents.ARROW_ASSEMBLY.get(),
                                new ArrowAssembly("glass_vial", "oak", "feather"));
                    }

                    dippingRecipes.add(new DippingRecipe(
                            recipe.inputItem(),
                            recipe.inputCount(),
                            java.util.Optional.of(potionId),
                            recipe.fluidAmount(),
                            specificArrow
                    ));
                });
            } else {
                // Recipes with a specific required_potion (e.g. golden apple
                // dipping for some custom effect) pass through unchanged.
                dippingRecipes.add(recipe);
            }
        }
        registration.addRecipes(DippingRecipeCategory.DIPPING_TYPE, dippingRecipes);

        // Crossbow Bench: bow + trigger -> crossbow, and one entry per
        // attachment def (crossbow + attachment -> attached crossbow).
        registration.addRecipes(CrossbowBenchRecipeCategory.TYPE, buildCrossbowBenchRecipes());
    }

    private static List<CrossbowBenchJeiRecipe> buildCrossbowBenchRecipes() {
        List<CrossbowBenchJeiRecipe> recipes = new ArrayList<>();
        var assembly = new net.frostytrix.fletcherstrestle.component.BowAssembly("oak", "wood", "flax", 1.0f);

        ItemStack sampleBow = new ItemStack(ModItems.MODULAR_BOW.get());
        sampleBow.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);
        ItemStack sampleCrossbow = new ItemStack(ModItems.MODULAR_CROSSBOW.get());
        sampleCrossbow.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);

        // Assembly: bow + mechanical trigger -> crossbow.
        recipes.add(new CrossbowBenchJeiRecipe(
                List.of(sampleBow, new ItemStack(ModItems.MECHANICAL_TRIGGER.get())),
                sampleCrossbow.copy()));

        // One entry per attachment def in the datapack registry.
        var registry = Minecraft.getInstance().level.registryAccess()
                .registryOrThrow(net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments.CROSSBOW_ATTACHMENT);
        for (var entry : registry.entrySet()) {
            ItemStack[] items = entry.getValue().ingredient().getItems();
            if (items.length == 0) continue;
            ItemStack attached = sampleCrossbow.copy();
            attached.set(ModDataComponents.CROSSBOW_ATTACHMENT.get(), entry.getKey().location());
            recipes.add(new CrossbowBenchJeiRecipe(
                    List.of(sampleCrossbow.copy(), items[0].copy()),
                    attached));
        }
        return recipes;
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

        // Crossbow Bench Catalyst
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CROSSBOW_BENCH.get()), CrossbowBenchRecipeCategory.TYPE);
    }
}