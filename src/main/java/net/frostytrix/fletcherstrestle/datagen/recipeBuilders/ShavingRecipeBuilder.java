package net.frostytrix.fletcherstrestle.datagen.recipeBuilders;

import net.frostytrix.fletcherstrestle.recipe.ShavingHorseRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShavingRecipeBuilder implements RecipeBuilder {
    private final Ingredient input;
    private final ItemStack result;

    // Default to 3 drawknife clicks
    private int shavesRequired = 3;

    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable private String group;

    private ShavingRecipeBuilder(Ingredient input, ItemLike result) {
        this.input = input;
        this.result = new ItemStack(result, 1);
    }

    public static ShavingRecipeBuilder shaving(Ingredient input, ItemLike result) {
        return new ShavingRecipeBuilder(input, result);
    }

    public ShavingRecipeBuilder shavesRequired(int clicks) {
        this.shavesRequired = clicks;
        return this;
    }

    @Override
    public ShavingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public ShavingRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput recipeOutput, Identifier id) {
        this.ensureValid(id);
        Advancement.Builder advancementBuilder = recipeOutput.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancementBuilder::addCriterion);

        ShavingHorseRecipe recipe = new ShavingHorseRecipe(this.input, this.result, this.shavesRequired);
        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/shaving/")));
    }

    private void ensureValid(Identifier id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }
}