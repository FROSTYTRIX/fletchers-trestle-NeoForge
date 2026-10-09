package net.frostytrix.fletcherstrestle.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.fluid.ModFluids;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.recipe.DippingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class DippingRecipeCategory implements IRecipeCategory<DippingRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "dipping");
    public static final RecipeType<DippingRecipe> DIPPING_TYPE = new RecipeType<>(UID, DippingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public DippingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(135, 60);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.DIPPING_VAT.get()));

        IDrawableStatic staticArrow = helper.createDrawable(ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png"), 79, 34, 24, 17);
        this.arrow = helper.createAnimatedDrawable(staticArrow, 200, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<DippingRecipe> getRecipeType() {
        return DIPPING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.fletcherstrestle.dipping_vat");
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
    public void draw(DippingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        this.arrow.draw(guiGraphics, 69, 22);
    }

    @SuppressWarnings("removal")
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DippingRecipe recipe, IFocusGroup focuses) {

        // Special-case the modular potion-arrow recipe so JEI shows a real
        // glass-vial arrow + sample potion + filled output instead of three
        // empty modular_arrow icons. Detected via the output item.
        boolean isModularPotion = recipe.output().is(ModItems.MODULAR_ARROW.get());

        // Input slot
        List<ItemStack> inputStacks = new ArrayList<>();
        for (ItemStack stack : recipe.inputItem().getItems()) {
            ItemStack copy = stack.copy();
            copy.setCount(recipe.inputCount());
            if (isModularPotion && copy.is(ModItems.MODULAR_ARROW.get())) {
                // Empty glass-vial arrow (no potion yet): visualizes what
                // you'd put in the vat.
                copy.set(ModDataComponents.ARROW_ASSEMBLY.get(),
                        new ArrowAssembly("glass_vial", "oak", "feather"));
            }
            inputStacks.add(copy);
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 15, 22)
                .addIngredients(VanillaTypes.ITEM_STACK, inputStacks);

        // Fluid slot
        // Pick a sample potion ID for the modular case so the fluid shows
        // a real color and the tooltip names it. Defaults to regeneration
        // because it's instantly recognisable pink.
        String fluidPotionId = recipe.requiredPotion().orElse(
                isModularPotion ? "minecraft:strong_regeneration" : null);

        FluidStack fluidToDisplay = new FluidStack(
                ModFluids.LIQUID_POTION_SOURCE.get(), recipe.fluidAmount());
        if (fluidPotionId != null) {
            BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(fluidPotionId)).ifPresent(potion ->
                    fluidToDisplay.set(DataComponents.POTION_CONTENTS, new PotionContents(potion)));
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 43, 22)
                .addIngredient(NeoForgeTypes.FLUID_STACK, fluidToDisplay)
                .setFluidRenderer(recipe.fluidAmount(), false, 16, 16)
                .addTooltipCallback((recipeSlotView, tooltip) -> {
                    recipeSlotView.getDisplayedIngredient(NeoForgeTypes.FLUID_STACK).ifPresent(fluidStack -> {
                        PotionContents contents = net.frostytrix.fletcherstrestle.fluid.PotionFluid.contents(fluidStack);
                        if (!contents.equals(PotionContents.EMPTY)) {
                            tooltip.clear(); // drop the default "Water" line
                            ItemStack dummyPotion = new ItemStack(Items.POTION);
                            dummyPotion.set(DataComponents.POTION_CONTENTS, contents);
                            // For modular arrows, the fluid is just an example,
                            // any potion works. Make that clear in the tooltip.
                            if (recipe.requiredPotion().isEmpty()) {
                                tooltip.add(0, Component.translatable("jei.fletcherstrestle.any_potion", dummyPotion.getHoverName())
                                        .withStyle(ChatFormatting.AQUA));
                            } else {
                                tooltip.add(0, Component.translatable("jei.fletcherstrestle.fluid", dummyPotion.getHoverName()));
                            }
                            tooltip.add(Component.literal(fluidStack.getAmount() + " mB").withStyle(ChatFormatting.GRAY));
                        }
                    });
                });

        // Output slot
        ItemStack outputDisplay = recipe.output().copy();
        if (isModularPotion) {
            // Show what a filled glass-vial arrow looks like: same assembly
            // as the input + the example potion's effect contents.
            outputDisplay.set(ModDataComponents.ARROW_ASSEMBLY.get(),
                    new ArrowAssembly("glass_vial", "oak", "feather"));
            if (fluidPotionId != null) {
                BuiltInRegistries.POTION
                        .getHolder(ResourceLocation.parse(fluidPotionId))
                        .ifPresent(holder ->
                                outputDisplay.set(DataComponents.POTION_CONTENTS,
                                        new PotionContents(holder)));
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 101, 22)
                .addItemStack(outputDisplay);
    }
}