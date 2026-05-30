package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.datagen.recipeBuilders.DippingRecipeBuilder;
import net.frostytrix.fletcherstrestle.datagen.recipeBuilders.ShavingRecipeBuilder;
import net.frostytrix.fletcherstrestle.datagen.recipeBuilders.SteamingRecipeBuilder;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
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

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DRAWKNIFE.get())
                .pattern("S  ")
                .pattern("I  ")
                .pattern("S  ")
                .define('S', Items.STICK)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(recipeOutput, FletcherTrestle.MOD_ID + ":drawknife_vertical");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HIGH_TENSION_STRING.get())
                .requires(Items.STRING, 2).requires(Items.IRON_NUGGET)
                .unlockedBy("has_iron_nugget", has(Items.IRON_NUGGET))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FLAX_STRING.get())
                .requires(ModItems.FLAX, 4)
                .unlockedBy("has_flax", has(ModItems.FLAX))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LEATHER_QUIVER.get())
                .pattern("SLS")
                .pattern("LCL")
                .pattern("SLS")
                .define('S', ModTags.Items.BOW_STRINGS)
                .define('L', Items.LEATHER)
                .define('C', Blocks.CHEST)
                .unlockedBy("has_arrow", has(ItemTags.ARROWS)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IRON_QUIVER.get())
                .pattern(" I ")
                .pattern("IQI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('Q', ModItems.LEATHER_QUIVER)
                .unlockedBy("has_leather_quiver", has(ModItems.LEATHER_QUIVER)).save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.ARCHERY_TARGET.get())
                .requires(Blocks.WHITE_WOOL).requires(Items.BLACK_DYE).requires(Items.BLUE_DYE).requires(Items.RED_DYE).requires(Items.YELLOW_DYE)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(recipeOutput);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FLETCHER_GUIDE.get())
                .requires(Items.BOOK).requires(Items.FEATHER)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(recipeOutput);

        // Bow -> crossbow assembly moved off the Smithing Table onto the
        // Crossbow Bench (handled in CrossbowBenchMenu). The bench is the single
        // source of truth, so the old smithing recipes are intentionally gone.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CROSSBOW_BENCH.get())
                .pattern("PPP")
                .pattern("PHP")
                .pattern("I I")
                .define('P', ItemTags.PLANKS)
                .define('H', Items.TRIPWIRE_HOOK)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_tripwire_hook", has(Items.TRIPWIRE_HOOK)).save(recipeOutput);

        // Magazine attachment — an iron clip with a redstone feed spring.
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MAGAZINE.get())
                .pattern("I I")
                .pattern("IRI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEAVY_DUMMY_ITEM.get())
                .pattern("HTH")
                .pattern("HTH")
                .pattern(" I ")
                .define('I', Blocks.IRON_BLOCK)
                .define('H', Blocks.HAY_BLOCK)
                .define('T', ModBlocks.ARCHERY_TARGET)
                .unlockedBy("has_target", has(ModBlocks.ARCHERY_TARGET)).save(recipeOutput);

        // Steaming

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_OAK_LIMB.get()), ModItems.PLIABLE_OAK_LIMB.get())
                .unlockedBy("has_rough_oak_limb", has(ModItems.ROUGH_OAK_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_oak_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_SPRUCE_LIMB.get()), ModItems.PLIABLE_SPRUCE_LIMB.get())
                .unlockedBy("has_rough_spruce_limb", has(ModItems.ROUGH_SPRUCE_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_spruce_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_BIRCH_LIMB.get()), ModItems.PLIABLE_BIRCH_LIMB.get())
                .unlockedBy("has_rough_birch_limb", has(ModItems.ROUGH_BIRCH_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_birch_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_JUNGLE_LIMB.get()), ModItems.PLIABLE_JUNGLE_LIMB.get())
                .unlockedBy("has_rough_jungle_limb", has(ModItems.ROUGH_JUNGLE_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_jungle_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_ACACIA_LIMB.get()), ModItems.PLIABLE_ACACIA_LIMB.get())
                .unlockedBy("has_rough_acacia_limb", has(ModItems.ROUGH_ACACIA_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_acacia_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_DARK_OAK_LIMB.get()), ModItems.PLIABLE_DARK_OAK_LIMB.get())
                .unlockedBy("has_rough_dark_oak_limb", has(ModItems.ROUGH_DARK_OAK_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_dark_oak_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_MANGROVE_LIMB.get()), ModItems.PLIABLE_MANGROVE_LIMB.get())
                .unlockedBy("has_rough_mangrove_limb", has(ModItems.ROUGH_MANGROVE_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_mangrove_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_CHERRY_LIMB.get()), ModItems.PLIABLE_CHERRY_LIMB.get())
                .unlockedBy("has_rough_cherry_limb", has(ModItems.ROUGH_CHERRY_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_cherry_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_CRIMSON_LIMB.get()), ModItems.PLIABLE_CRIMSON_LIMB.get())
                .unlockedBy("has_rough_crimson_limb", has(ModItems.ROUGH_CRIMSON_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_crimson_limb"));

        SteamingRecipeBuilder.steaming(Ingredient.of(ModItems.ROUGH_WARPED_LIMB.get()), ModItems.PLIABLE_WARPED_LIMB.get())
                .unlockedBy("has_rough_warped_limb", has(ModItems.ROUGH_WARPED_LIMB.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "steaming_warped_limb"));
        //test craft
        SteamingRecipeBuilder.steaming(Ingredient.of(Items.POTATO), Items.BAKED_POTATO)
                .unlockedBy("has_potato", has(Items.POTATO))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "test_potato"));

        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STICK), ModItems.ROUGH_OAK_LIMB.get())
                .shavesRequired(1)
                .unlockedBy("has_stick_stem", has(Items.STICK))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stick_stem"));

        // Shaving Horse

        // Oak
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.OAK_LOG), ModItems.ROUGH_OAK_LIMB.get())
                .unlockedBy("has_oak_log", has(Items.OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_oak_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.OAK_WOOD), ModItems.ROUGH_OAK_LIMB.get())
                .unlockedBy("has_oak_log", has(Items.OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_oak_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_OAK_LOG), ModItems.ROUGH_OAK_LIMB.get())
                .unlockedBy("has_oak_log", has(Items.OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_oak_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_OAK_WOOD), ModItems.ROUGH_OAK_LIMB.get())
                .unlockedBy("has_oak_log", has(Items.OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_strippe_oak_wood"));

        // Spruce
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.SPRUCE_LOG), ModItems.ROUGH_SPRUCE_LIMB.get())
                .unlockedBy("has_spruce_log", has(Items.SPRUCE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_spruce_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.SPRUCE_WOOD), ModItems.ROUGH_SPRUCE_LIMB.get())
                .unlockedBy("has_spruce_log", has(Items.SPRUCE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_spruce_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_SPRUCE_LOG), ModItems.ROUGH_SPRUCE_LIMB.get())
                .unlockedBy("has_spruce_log", has(Items.SPRUCE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_spruce_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_SPRUCE_WOOD), ModItems.ROUGH_SPRUCE_LIMB.get())
                .unlockedBy("has_spruce_log", has(Items.SPRUCE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_spruce_wood"));

        // Birch
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.BIRCH_LOG), ModItems.ROUGH_BIRCH_LIMB.get())
                .unlockedBy("has_birch_log", has(Items.BIRCH_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_birch_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.BIRCH_WOOD), ModItems.ROUGH_BIRCH_LIMB.get())
                .unlockedBy("has_birch_log", has(Items.BIRCH_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_birch_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_BIRCH_LOG), ModItems.ROUGH_BIRCH_LIMB.get())
                .unlockedBy("has_birch_log", has(Items.BIRCH_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_birch_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_BIRCH_WOOD), ModItems.ROUGH_BIRCH_LIMB.get())
                .unlockedBy("has_birch_log", has(Items.BIRCH_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_birch_wood"));

        // Jungle
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.JUNGLE_LOG), ModItems.ROUGH_JUNGLE_LIMB.get())
                .unlockedBy("has_jungle_log", has(Items.JUNGLE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_jungle_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.JUNGLE_WOOD), ModItems.ROUGH_JUNGLE_LIMB.get())
                .unlockedBy("has_jungle_log", has(Items.JUNGLE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_jungle_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_JUNGLE_LOG), ModItems.ROUGH_JUNGLE_LIMB.get())
                .unlockedBy("has_jungle_log", has(Items.JUNGLE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_jungle_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_JUNGLE_WOOD), ModItems.ROUGH_JUNGLE_LIMB.get())
                .unlockedBy("has_jungle_log", has(Items.JUNGLE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_jungle_wood"));

        // Acacia
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.ACACIA_LOG), ModItems.ROUGH_ACACIA_LIMB.get())
                .unlockedBy("has_acacia_log", has(Items.ACACIA_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_acacia_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.ACACIA_WOOD), ModItems.ROUGH_ACACIA_LIMB.get())
                .unlockedBy("has_acacia_log", has(Items.ACACIA_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_acacia_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_ACACIA_LOG), ModItems.ROUGH_ACACIA_LIMB.get())
                .unlockedBy("has_acacia_log", has(Items.ACACIA_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_acacia_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_ACACIA_WOOD), ModItems.ROUGH_ACACIA_LIMB.get())
                .unlockedBy("has_acacia_log", has(Items.ACACIA_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_acacia_wood"));

        // Dark Oak
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.DARK_OAK_LOG), ModItems.ROUGH_DARK_OAK_LIMB.get())
                .shavesRequired(5)
                .unlockedBy("has_dark_oak_log", has(Items.DARK_OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_dark_oak_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.DARK_OAK_WOOD), ModItems.ROUGH_DARK_OAK_LIMB.get())
                .shavesRequired(5)
                .unlockedBy("has_dark_oak_log", has(Items.DARK_OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_dark_oak_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_DARK_OAK_LOG), ModItems.ROUGH_DARK_OAK_LIMB.get())
                .shavesRequired(5)
                .unlockedBy("has_dark_oak_log", has(Items.DARK_OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_dark_oak_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_DARK_OAK_WOOD), ModItems.ROUGH_DARK_OAK_LIMB.get())
                .shavesRequired(5)
                .unlockedBy("has_dark_oak_log", has(Items.DARK_OAK_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_dark_oak_wood"));

        // Mangrove
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.MANGROVE_LOG), ModItems.ROUGH_MANGROVE_LIMB.get())
                .unlockedBy("has_mangrove_log", has(Items.MANGROVE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_mangrove_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.MANGROVE_LOG), ModItems.ROUGH_MANGROVE_LIMB.get())
                .unlockedBy("has_mangrove_log", has(Items.MANGROVE_WOOD))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_mangrove_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.MANGROVE_LOG), ModItems.ROUGH_MANGROVE_LIMB.get())
                .unlockedBy("has_mangrove_log", has(Items.STRIPPED_MANGROVE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_mangrove_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_MANGROVE_WOOD), ModItems.ROUGH_MANGROVE_LIMB.get())
                .unlockedBy("has_mangrove_log", has(Items.MANGROVE_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_mangrove_wood"));

        // Cherry
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.CHERRY_LOG), ModItems.ROUGH_CHERRY_LIMB.get())
                .unlockedBy("has_cherry_log", has(Items.CHERRY_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_cherry_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.CHERRY_WOOD), ModItems.ROUGH_CHERRY_LIMB.get())
                .unlockedBy("has_cherry_log", has(Items.CHERRY_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_cherry_wood"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_CHERRY_LOG), ModItems.ROUGH_CHERRY_LIMB.get())
                .unlockedBy("has_cherry_log", has(Items.CHERRY_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_cherry_log"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_CHERRY_WOOD), ModItems.ROUGH_CHERRY_LIMB.get())
                .unlockedBy("has_cherry_log", has(Items.CHERRY_LOG))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_cherry_wood"));

        // Crimson (Nether wood uses STEM instead of LOG)
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.CRIMSON_STEM), ModItems.ROUGH_CRIMSON_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_crimson_stem", has(Items.CRIMSON_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_crimson_stem"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.CRIMSON_HYPHAE), ModItems.ROUGH_CRIMSON_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_crimson_stem", has(Items.CRIMSON_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_crimson_hyphae"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.CRIMSON_STEM), ModItems.ROUGH_CRIMSON_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_crimson_stem", has(Items.STRIPPED_CRIMSON_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_crimson_stem"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.CRIMSON_STEM), ModItems.ROUGH_CRIMSON_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_crimson_stem", has(Items.STRIPPED_CRIMSON_HYPHAE))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_crimson_hyphae"));

        // Warped (Nether wood uses STEM instead of LOG)
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.WARPED_STEM), ModItems.ROUGH_WARPED_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_warped_stem", has(Items.WARPED_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_warped_stem"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.WARPED_HYPHAE), ModItems.ROUGH_WARPED_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_warped_stem", has(Items.WARPED_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_warped_hyphae"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_WARPED_STEM), ModItems.ROUGH_WARPED_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_warped_stem", has(Items.WARPED_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_warped_stem"));
        ShavingRecipeBuilder.shaving(Ingredient.of(Items.STRIPPED_WARPED_HYPHAE), ModItems.ROUGH_WARPED_LIMB.get())
                .shavesRequired(4)
                .unlockedBy("has_warped_stem", has(Items.WARPED_STEM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "shaving_stripped_warped_hyphae"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.WEIGHTED_HOOK.get())
                .pattern("III")
                .pattern(" S ")
                .define('S', ModTags.Items.BOW_STRINGS)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_arrow", has(ItemTags.ARROWS)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModBlocks.ROPE.get())
                .pattern("FF ")
                .pattern("FF ")
                .pattern("FF ")
                .define('F', ModItems.FLAX_STRING)
                .unlockedBy("has_flax", has(ModItems.FLAX_STRING)).save(recipeOutput);

        DippingRecipeBuilder.dipping(Ingredient.of(Items.ARROW), Items.TIPPED_ARROW, 16)
                .inputCount(16)
                .fluidAmount(1000)
                .unlockedBy("has_arrow", has(Items.ARROW))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "arrow_dipping"));

        // Modular potion-arrow dipping. DippingRecipe.matches() restricts
        // the modular_arrow input to those with a glass_vial head, so this
        // recipe doesn't accidentally affect e.g. broadhead arrows.
        DippingRecipeBuilder.dipping(Ingredient.of(ModItems.MODULAR_ARROW.get()), ModItems.MODULAR_ARROW.get(), 1)
                .inputCount(1)
                .fluidAmount(250)
                .unlockedBy("has_modular_arrow", has(ModItems.MODULAR_ARROW.get()))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "modular_potion_arrow_dipping"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.DIPPING_VAT.get())
                .requires(Items.BUCKET).requires(Blocks.CAULDRON)
                .unlockedBy("has_arrow", has(Items.ARROW)).save(recipeOutput);
    }
}
