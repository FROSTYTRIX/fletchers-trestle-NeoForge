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

public class SteamingRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    private final ItemStack result;
    private final int waterAmount; // Millibuckets required
    private final int processingTime; // Ticks required

    public SteamingRecipe(Ingredient input, ItemStack result, int waterAmount, int processingTime) {
        this.input = input;
        this.result = result;
        this.waterAmount = waterAmount;
        this.processingTime = processingTime;
    }

    public Ingredient getInput() {
        return input;
    }

    public int getWaterAmount() {
        return waterAmount;
    }

    public int getProcessingTime() {
        return processingTime;
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
        return ModRecipes.STEAMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.STEAMING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SteamingRecipe> {
        public static final MapCodec<SteamingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("water_amount", 250).forGetter(r -> r.waterAmount), // Default 250mb if omitted
                Codec.INT.optionalFieldOf("processing_time", 200).forGetter(r -> r.processingTime) // Default 10 seconds if omitted
        ).apply(inst, SteamingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SteamingRecipe> STREAM_CODEC = StreamCodec.of(
                SteamingRecipe.Serializer::toNetwork, SteamingRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<SteamingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SteamingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static SteamingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            return new SteamingRecipe(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            );
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, SteamingRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            ByteBufCodecs.INT.encode(buf, recipe.waterAmount);
            ByteBufCodecs.INT.encode(buf, recipe.processingTime);
        }
    }
}