package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.recipe.SteamingRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class SteamingRecipeBuilder implements RecipeBuilder {
    private final Ingredient input;
    private final ItemStack result;

    // Set your defaults right here!
    private int processingTime = 200;
    private int waterAmount = 250;

    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable private String group;

    private SteamingRecipeBuilder(Ingredient input, ItemLike result) {
        this.input = input;
        this.result = new ItemStack(result, 1);
    }

    // The single entry point
    public static SteamingRecipeBuilder steaming(Ingredient input, ItemLike result) {
        return new SteamingRecipeBuilder(input, result);
    }

    // Chainable method for Time
    public SteamingRecipeBuilder processingTime(int ticks) {
        this.processingTime = ticks;
        return this;
    }

    // Chainable method for Water Intake
    public SteamingRecipeBuilder waterAmount(int mb) {
        this.waterAmount = mb;
        return this;
    }

    @Override
    public SteamingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public SteamingRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, ResourceLocation id) {
        this.ensureValid(id);

        Advancement.Builder advancementBuilder = recipeOutput.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancementBuilder::addCriterion);

        SteamingRecipe recipe = new SteamingRecipe(this.input, this.result, this.waterAmount, this.processingTime);

        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/steaming/")));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }
}