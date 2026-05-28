package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.material.MaterialResolver;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class ModularArrowRecipe implements Recipe<ArrowRecipeInput> {

    private final Ingredient head;
    private final Ingredient shaft;
    private final Ingredient fletching;
    private final ItemStack result;

    public ModularArrowRecipe(Ingredient head, Ingredient shaft, Ingredient fletching, ItemStack result) {
        this.head = head;
        this.shaft = shaft;
        this.fletching = fletching;
        this.result = result;
    }

    // Getters for JEI
    public Ingredient getHead() {
        return head;
    }

    public Ingredient getShaft() {
        return shaft;
    }

    public Ingredient getFletching() {
        return fletching;
    }

    @Override
    public boolean matches(ArrowRecipeInput input, Level level) {
        if (input.isEmpty()) return false;
        return this.head.test(input.head()) &&
                this.shaft.test(input.shaft()) &&
                this.fletching.test(input.fletching());
    }

    @Override
    public ItemStack assemble(ArrowRecipeInput input, HolderLookup.Provider provider) {
        ItemStack output = this.result.copy();

        // Resolve each part by its Ingredient match against the supplied
        // stack. Returns the canonical registry-id path (e.g. "broadhead")
        // — same string the legacy hardcoded chains used to return, so
        // ArrowAssembly's storage format and downstream string-matching
        // branches in ModularArrowEntity are unchanged.
        String headName = resolveHead(provider, input.head());
        String shaftName = resolveShaft(provider, input.shaft());
        String fletchName = resolveFletching(provider, input.fletching());

        ArrowAssembly assembly = new ArrowAssembly(headName, shaftName, fletchName);
        output.set(ModDataComponents.ARROW_ASSEMBLY.get(), assembly);

        return output;
    }

    /**
     * Returns the id-path of the matching head def, or {@code "flint"} as fallback.
     */
    private static String resolveHead(HolderLookup.Provider provider, ItemStack stack) {
        return MaterialResolver.resolveArrowHead(provider, stack)
                .map(h -> h.key().location().toString())
                .orElse("flint");
    }

    private static String resolveShaft(HolderLookup.Provider provider, ItemStack stack) {
        return MaterialResolver.resolveArrowShaft(provider, stack)
                .map(h -> h.key().location().toString())
                .orElse("oak");
    }

    private static String resolveFletching(HolderLookup.Provider provider, ItemStack stack) {
        return MaterialResolver.resolveArrowFletching(provider, stack)
                .map(h -> h.key().location().toString())
                .orElse("feather");
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return this.result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MODULAR_ARROW_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MODULAR_ARROW_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ModularArrowRecipe> {
        public static final MapCodec<ModularArrowRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("head").forGetter(r -> r.head),
                Ingredient.CODEC_NONEMPTY.fieldOf("shaft").forGetter(r -> r.shaft),
                Ingredient.CODEC_NONEMPTY.fieldOf("fletching").forGetter(r -> r.fletching),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(inst, ModularArrowRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ModularArrowRecipe> STREAM_CODEC = StreamCodec.of(
                ModularArrowRecipe.Serializer::toNetwork, ModularArrowRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<ModularArrowRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ModularArrowRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ModularArrowRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            return new ModularArrowRecipe(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf)
            );
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, ModularArrowRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.head);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.shaft);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.fletching);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        }
    }
}