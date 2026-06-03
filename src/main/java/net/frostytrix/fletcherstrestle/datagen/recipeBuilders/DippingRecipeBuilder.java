package net.frostytrix.fletcherstrestle.datagen.recipeBuilders;

import net.frostytrix.fletcherstrestle.recipe.DippingRecipe;
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
import java.util.Optional;

public class DippingRecipeBuilder implements RecipeBuilder {
    private final Ingredient input;
    private final ItemStack result;

    // Defaults
    private int inputCount = 1;
    private int fluidAmount = 1000;
    private Optional<String> requiredPotion = Optional.empty();

    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    private DippingRecipeBuilder(Ingredient input, ItemLike result, int resultCount) {
        this.input = input;
        this.result = new ItemStack(result, resultCount);
    }

    // Basic entry point (yields 1 item).
    public static DippingRecipeBuilder dipping(Ingredient input, ItemLike result) {
        return new DippingRecipeBuilder(input, result, 1);
    }

    // Entry point for stacked results (e.g. 16 arrows).
    public static DippingRecipeBuilder dipping(Ingredient input, ItemLike result, int resultCount) {
        return new DippingRecipeBuilder(input, result, resultCount);
    }

    // --- METHODES CHAINABLES (CHAINING) ---
    public DippingRecipeBuilder inputCount(int count) {
        this.inputCount = count;
        return this;
    }

    public DippingRecipeBuilder fluidAmount(int mb) {
        this.fluidAmount = mb;
        return this;
    }

    public DippingRecipeBuilder requiredPotion(String potionId) {
        this.requiredPotion = Optional.of(potionId);
        return this;
    }

    // --- IMPLEMENTATION DU RECIPEBUILDER ---
    @Override
    public DippingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public DippingRecipeBuilder group(@Nullable String group) {
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

        // Build the recipe object.
        DippingRecipe recipe = new DippingRecipe(this.input, this.inputCount, this.requiredPotion, this.fluidAmount, this.result);

        // Sauvegarde de la recette ET de l'avancement dans le Datagen
        recipeOutput.accept(id, recipe, advancementBuilder.build(id.withPrefix("recipes/dipping/")));
    }

    private void ensureValid(ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
    }
}