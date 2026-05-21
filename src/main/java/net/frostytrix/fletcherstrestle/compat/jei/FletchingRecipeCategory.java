package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.recipe.ModularWeaponRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class FletchingRecipeCategory implements IRecipeCategory<ModularWeaponRecipe> {
    public static final Identifier UID = Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "modular_weapon_assembly");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/gui/fletching_table.png");

    // This creates the "Type" identifier JEI uses internally
    public static final RecipeType<ModularWeaponRecipe> FLETCHING_TYPE = new RecipeType<>(UID, ModularWeaponRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FletchingRecipeCategory(IGuiHelper helper) {
        // We cut out a 150x60 rectangle from your GUI texture to use as the background
        this.background = helper.createDrawable(TEXTURE, 10, 10, 150, 60);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Blocks.FLETCHING_TABLE));
    }

    @Override
    public RecipeType<ModularWeaponRecipe> getRecipeType() {
        return FLETCHING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Fletching"); // Or use Component.translatable("block.fletcherstrestle.fletching_trestle")
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
    public void setRecipe(IRecipeLayoutBuilder builder, ModularWeaponRecipe recipe, IFocusGroup focuses) {
        // 1. ADD ALL FOUR SLOTS EXPLICITLY

        // Riser slot
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 25).addIngredients(recipe.getRiser());
        // Top Limb slot (uses the limbs ingredient)
        builder.addSlot(RecipeIngredientRole.INPUT, 34, 25-18).addIngredients(recipe.getLimbs());

        // Bottom Limb slot (uses the exact same limbs ingredient, but at a different Y coordinate)
        builder.addSlot(RecipeIngredientRole.INPUT, 34, 25+18).addIngredients(recipe.getLimbs());

        // String slot
        builder.addSlot(RecipeIngredientRole.INPUT, 58, 25).addIngredients(recipe.getString());


        // 2. GENERATE DYNAMIC OUTPUT
        List<ItemStack> outputPermutations = new java.util.ArrayList<>();

        ItemStack[] risers = recipe.getRiser().getItems();
        ItemStack[] limbs = recipe.getLimbs().getItems();
        ItemStack[] strings = recipe.getString().getItems();

        // Ensure we rotate through everything
        int maxCombinations = Math.max(risers.length, Math.max(limbs.length, strings.length));

        for (int i = 0; i < maxCombinations; i++) {
            ItemStack currentRiser = risers[i % risers.length];
            ItemStack currentLimb = limbs[i % limbs.length];
            ItemStack currentString = strings[i % strings.length];

            ItemStack out = recipe.getResultItem(null).copy();

            String riserMat = ModularWeaponRecipe.getMaterialName(currentRiser);
            String limbMat = ModularWeaponRecipe.getMaterialName(currentLimb);
            String stringMat = ModularWeaponRecipe.getMaterialName(currentString);

            out.set(ModDataComponents.BOW_ASSEMBLY.get(), new net.frostytrix.fletcherstrestle.component.BowAssembly(limbMat, riserMat, stringMat, 0.0f));
            outputPermutations.add(out);
        }

        // 3. ADD OUTPUT SLOT
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 25).addItemStacks(outputPermutations);
    }
}