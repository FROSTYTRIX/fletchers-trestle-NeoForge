package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ShavingHorseRecipe implements Recipe<SingleRecipeInput> {
    private final Ingredient input;
    // 26.1: recipes store ItemStackTemplate (a Holder<Item> + count + patch)
    // for the result instead of a fully-materialised ItemStack. ItemStack.CODEC
    // now routes through Item.CODEC_WITH_BOUND_COMPONENTS, which throws
    // "does not have components yet" at recipe-load time because component
    // binding hasn't run on the server when datapacks reload. The template
    // codec sidesteps that and materializes into a real ItemStack via
    // .create() inside assemble().
    private final ItemStackTemplate result;
    private final int shavesRequired;

    public ShavingHorseRecipe(Ingredient input, ItemStackTemplate result, int shavesRequired) {
        this.input = input;
        this.result = result;
        this.shavesRequired = shavesRequired;
    }

    public Ingredient getInput() { return input; }
    public int getShavesRequired() { return shavesRequired; }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.create();
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return ModRecipes.SHAVING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return ModRecipes.SHAVING_TYPE.get();
    }

    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public PlacementInfo placementInfo()           { return PlacementInfo.NOT_PLACEABLE; }
    @Override public boolean showNotification()              { return false; }
    @Override public String group()                          { return ""; }

    public static final MapCodec<ShavingHorseRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
            Codec.INT.optionalFieldOf("shaves_required", 3).forGetter(r -> r.shavesRequired)
    ).apply(inst, ShavingHorseRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShavingHorseRecipe> STREAM_CODEC = StreamCodec.of(
            ShavingHorseRecipe::toNetwork, ShavingHorseRecipe::fromNetwork);

    public static RecipeSerializer<ShavingHorseRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, ShavingHorseRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input);
        ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
        ByteBufCodecs.INT.encode(buf, recipe.shavesRequired);
    }

    private static ShavingHorseRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        return new ShavingHorseRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                ItemStackTemplate.STREAM_CODEC.decode(buf),
                ByteBufCodecs.INT.decode(buf)
        );
    }
}
