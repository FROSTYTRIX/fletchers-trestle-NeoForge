package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class DippingRecipe implements Recipe<DippingRecipeInput> {
    public final Ingredient inputItem;
    public final int inputCount;
    public final Optional<String> requiredPotion; // NOUVEAU : Optionnel !
    public final int fluidAmount;
    public final ItemStack output;

    public DippingRecipe(Ingredient inputItem, int inputCount, Optional<String> requiredPotion, int fluidAmount, ItemStack output) {
        this.inputItem = inputItem;
        this.inputCount = inputCount;
        this.requiredPotion = requiredPotion;
        this.fluidAmount = fluidAmount;
        this.output = output;
    }

    @Override
    public boolean matches(DippingRecipeInput input, Level level) {
        // 1. Est-ce que l'item et la quantité de fluide matchent ?
        if (!this.inputItem.test(input.item()) || input.fluid().getAmount() < this.fluidAmount) {
            return false;
        }

        // 2. NOUVEAU : Si la recette exige une potion spécifique, on vérifie !
        if (this.requiredPotion.isPresent()) {
            net.minecraft.world.item.component.CustomData customData = input.fluid().get(DataComponents.CUSTOM_DATA);
            if (customData == null || !customData.contains("potion")) {
                return false; // Pas de potion du tout dans le fluide
            }
            String potionInTank = customData.copyTag().getString("potion");
            return potionInTank.equals(this.requiredPotion.get()); // Doit être EXACTEMENT la potion demandée
        }

        return true; // Si pas de requiredPotion, on accepte n'importe laquelle !
    }

    @Override
    public ItemStack assemble(DippingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = this.output.copy();

        // Si la recette demandait explicitement une potion, on ne transfère pas l'effet
        // (ex: une pomme en or reste une pomme en or, elle ne devient pas une "pomme en or de régénération")
        // Mais si c'est générique (requiredPotion est vide, ex: les flèches), on applique la magie :
        if (this.requiredPotion.isEmpty()) {
            net.minecraft.world.item.component.CustomData customData = input.fluid().get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains("potion")) {
                String potionId = customData.copyTag().getString("potion");
                var potionHolder = net.minecraft.core.registries.BuiltInRegistries.POTION
                        .getHolder(ResourceLocation.parse(potionId)).orElse(null);

                if (potionHolder != null) {
                    result.set(DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(potionHolder));
                }
            }
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DIPPING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.DIPPING_TYPE.get();
    }

    // --- LE SERIALIZER MIS À JOUR POUR LIRE L'OPTION ---
    public static class Serializer implements RecipeSerializer<DippingRecipe> {

        public static final MapCodec<DippingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.inputItem),
                Codec.INT.fieldOf("input_count").orElse(1).forGetter(r -> r.inputCount),
                Codec.STRING.optionalFieldOf("required_potion").forGetter(r -> r.requiredPotion), // NOUVEAU
                Codec.INT.fieldOf("fluid_amount").forGetter(r -> r.fluidAmount),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output)
        ).apply(inst, DippingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DippingRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork
        );

        @Override
        public MapCodec<DippingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DippingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, DippingRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.inputItem);
            buf.writeInt(recipe.inputCount);

            // Écriture de l'Optional sur le réseau
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, recipe.requiredPotion);

            buf.writeInt(recipe.fluidAmount);
            ItemStack.STREAM_CODEC.encode(buf, recipe.output);
        }

        private static DippingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            return new DippingRecipe(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf), // Lecture de l'Optional
                    buf.readInt(),
                    ItemStack.STREAM_CODEC.decode(buf)
            );
        }
    }
}