package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class DippingRecipe implements Recipe<DippingRecipeInput> {
    public final Ingredient inputItem;
    public final int inputCount;
    public final Optional<String> requiredPotion;
    public final int fluidAmount;
    // 26.1: ItemStackTemplate replaces ItemStack for recipe results so the
    // JSON parse doesn't require item components to be bound yet (see
    // ShavingHorseRecipe for the longer note).
    public final ItemStackTemplate output;

    public DippingRecipe(Ingredient inputItem, int inputCount, Optional<String> requiredPotion, int fluidAmount, ItemStackTemplate output) {
        this.inputItem = inputItem;
        this.inputCount = inputCount;
        this.requiredPotion = requiredPotion;
        this.fluidAmount = fluidAmount;
        this.output = output;
    }

    @Override
    public boolean matches(DippingRecipeInput input, Level level) {
        if (!this.inputItem.test(input.item()) || input.fluid().getAmount() < this.fluidAmount) {
            return false;
        }
        ArrowAssembly assembly = input.item().get(ModDataComponents.ARROW_ASSEMBLY.get());
        if (assembly != null && !"glass_vial".equals(assembly.head())) {
            return false;
        }
        if (this.requiredPotion.isPresent()) {
            net.minecraft.world.item.component.CustomData customData = input.fluid().get(DataComponents.CUSTOM_DATA);
            if (customData == null || !customData.contains("potion")) {
                return false;
            }
            String potionInTank = customData.copyTag().getString("potion").orElse("");
            return potionInTank.equals(this.requiredPotion.get());
        }
        return true;
    }

    @Override
    public ItemStack assemble(DippingRecipeInput input) {
        ItemStack result = this.output.create();
        ArrowAssembly assembly = input.item().get(ModDataComponents.ARROW_ASSEMBLY.get());
        if (assembly != null) {
            result.set(ModDataComponents.ARROW_ASSEMBLY.get(), assembly);
            // The output is a fresh ItemStack from the recipe template, so
            // it inherits none of the input's data components — including
            // the 26.1 mirror components the renderer dispatches on.
            // Without these, a dipped arrow falls back to oak/feather/flint
            // even though the ArrowAssembly is correct. Mirror them back.
            result.set(ModDataComponents.HEAD_MATERIAL.get(), assembly.head());
            result.set(ModDataComponents.SHAFT_MATERIAL.get(), assembly.shaft());
            result.set(ModDataComponents.FLETCHING_MATERIAL.get(), assembly.fletching());
        }
        if (this.requiredPotion.isEmpty()) {
            net.minecraft.world.item.component.CustomData customData = input.fluid().get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains("potion")) {
                String potionId = customData.copyTag().getString("potion").orElse("");
                if (!potionId.isEmpty()) {
                    // Registry.get(Identifier) replaces getHolder in 26.1
                    net.minecraft.core.registries.BuiltInRegistries.POTION
                            .get(Identifier.parse(potionId))
                            .ifPresent(potionHolder -> result.set(
                                    DataComponents.POTION_CONTENTS,
                                    new net.minecraft.world.item.alchemy.PotionContents(potionHolder)));
                }
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<? extends Recipe<DippingRecipeInput>> getSerializer() {
        return ModRecipes.DIPPING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<DippingRecipeInput>> getType() {
        return ModRecipes.DIPPING_TYPE.get();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    // In 26.1, RecipeSerializer is a `record(MapCodec<T> codec, StreamCodec<...> streamCodec)`,
    // not an interface. So we just expose static codec instances and a factory
    // method to build the RecipeSerializer record at registration time.
    public static final MapCodec<DippingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.inputItem),
            Codec.INT.fieldOf("input_count").orElse(1).forGetter(r -> r.inputCount),
            Codec.STRING.optionalFieldOf("required_potion").forGetter(r -> r.requiredPotion),
            Codec.INT.fieldOf("fluid_amount").forGetter(r -> r.fluidAmount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.output)
    ).apply(inst, DippingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DippingRecipe> STREAM_CODEC = StreamCodec.of(
            DippingRecipe::toNetwork, DippingRecipe::fromNetwork);

    public static RecipeSerializer<DippingRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, DippingRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.inputItem);
        buf.writeInt(recipe.inputCount);
        ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, recipe.requiredPotion);
        buf.writeInt(recipe.fluidAmount);
        ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.output);
    }

    private static DippingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        return new DippingRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                buf.readInt(),
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                buf.readInt(),
                ItemStackTemplate.STREAM_CODEC.decode(buf)
        );
    }
}
