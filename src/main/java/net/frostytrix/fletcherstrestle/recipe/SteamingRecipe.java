package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class SteamingRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    private final ItemStack result;
    private final int waterAmount;
    private final int processingTime;

    public SteamingRecipe(Ingredient input, ItemStack result, int waterAmount, int processingTime) {
        this.input = input;
        this.result = result;
        this.waterAmount = waterAmount;
        this.processingTime = processingTime;
    }

    public Ingredient getInput() { return input; }
    public int getWaterAmount() { return waterAmount; }
    public int getProcessingTime() { return processingTime; }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.copy();
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return ModRecipes.STEAMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return ModRecipes.STEAMING_TYPE.get();
    }

    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public PlacementInfo placementInfo()           { return PlacementInfo.NOT_PLACEABLE; }
    @Override public boolean showNotification()              { return false; }
    @Override public String group()                          { return ""; }

    public static final MapCodec<SteamingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.input),
            ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
            Codec.INT.optionalFieldOf("water_amount", 250).forGetter(r -> r.waterAmount),
            Codec.INT.optionalFieldOf("processing_time", 200).forGetter(r -> r.processingTime)
    ).apply(inst, SteamingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SteamingRecipe> STREAM_CODEC = StreamCodec.of(
            SteamingRecipe::toNetwork, SteamingRecipe::fromNetwork);

    public static RecipeSerializer<SteamingRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, SteamingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input);
        ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        ByteBufCodecs.INT.encode(buf, recipe.waterAmount);
        ByteBufCodecs.INT.encode(buf, recipe.processingTime);
    }

    private static SteamingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        return new SteamingRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                ItemStack.STREAM_CODEC.decode(buf),
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf)
        );
    }
}
