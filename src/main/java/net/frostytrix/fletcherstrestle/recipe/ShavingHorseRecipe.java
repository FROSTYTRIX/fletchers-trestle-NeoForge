package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ShavingHorseRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    private final ItemStack result;
    private final int shavesRequired; // Number of drawknife clicks

    public ShavingHorseRecipe(Ingredient input, ItemStack result, int shavesRequired) {
        this.input = input;
        this.result = result;
        this.shavesRequired = shavesRequired;
    }

    public Ingredient getInput() {
        return input;
    }

    public int getShavesRequired() {
        return shavesRequired;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
        return this.result.copy();
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
        return ModRecipes.SHAVING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.SHAVING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ShavingHorseRecipe> {
        public static final MapCodec<ShavingHorseRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("shaves_required", 3).forGetter(r -> r.shavesRequired) // Defaults to 3 clicks!
        ).apply(inst, ShavingHorseRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShavingHorseRecipe> STREAM_CODEC = StreamCodec.of(
                ShavingHorseRecipe.Serializer::toNetwork, ShavingHorseRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<ShavingHorseRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShavingHorseRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ShavingHorseRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            return new ShavingHorseRecipe(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            );
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, ShavingHorseRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            ByteBufCodecs.INT.encode(buf, recipe.shavesRequired);
        }
    }
}