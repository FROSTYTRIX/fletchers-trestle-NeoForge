package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DRAWKNIFE.get())
                .pattern("SIS")
                .define('S', Items.STICK)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,ModItems.HIGH_TENSION_STRING.get())
                .requires(Items.STRING,2).requires(Items.IRON_NUGGET)
                .unlockedBy("has_iron_nugget", has(Items.IRON_NUGGET))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SHAVING_HORSE.get())
                .pattern("SS ")
                .pattern("sLs")
                .pattern("s s")
                .define('s', Items.STICK)
                .define('S', ItemTags.WOODEN_SLABS)
                .define('L', ItemTags.LOGS)
                .unlockedBy("has_slab", has(ItemTags.WOODEN_SLABS)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STEAM_BOX.get())
                .pattern("III")
                .pattern("CBC")
                .pattern("PPP")
                .define('I', Items.IRON_BARS)
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BUCKET)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_bucket", has(Items.BUCKET)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WOOD_RISER.get())
                .pattern("SSS")
                .define('S', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COPPER_RISER.get())
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IRON_RISER.get())
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(recipeOutput);
    }
}
