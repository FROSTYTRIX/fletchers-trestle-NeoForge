package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * @param requiredPotion optional: if present, the tank fluid must be exactly this potion
 */
public record DippingRecipe(Ingredient inputItem, int inputCount, Optional<String> requiredPotion, int fluidAmount,
                            ItemStack output) implements Recipe<DippingRecipeInput> {

    @Override
    public boolean matches(DippingRecipeInput input, Level level) {
        // Item and fluid amount must match.
        if (!this.inputItem.test(input.item()) || input.fluid().getAmount() < this.fluidAmount) {
            return false;
        }

        // 1b. Modular arrows can only be dipped if their head is the
        //     glass_vial type: the only head designed to hold a payload.
        //     This stops e.g. broadhead arrows from accidentally being
        //     converted into potion arrows.
        ArrowAssembly assembly = input.item().get(ModDataComponents.ARROW_ASSEMBLY.get());
        if (assembly != null && !"glass_vial".equals(assembly.head())) {
            return false;
        }

        // If the recipe requires a specific potion, the tank must hold exactly that one.
        if (this.requiredPotion.isPresent()) {
            CustomData customData = input.fluid().get(DataComponents.CUSTOM_DATA);
            if (customData == null || !customData.contains("potion")) {
                return false;
            }
            String potionInTank = customData.copyTag().getString("potion");
            return potionInTank.equals(this.requiredPotion.get());
        }

        return true;
    }

    @Override
    public ItemStack assemble(DippingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = this.output.copy();

        // Preserve the input's ArrowAssembly if present, so dipping a
        // glass_vial-headed arrow gives back the same modular arrow (with
        // its shaft + fletching choices) rather than a generic one.
        ArrowAssembly assembly = input.item().get(ModDataComponents.ARROW_ASSEMBLY.get());
        if (assembly != null) {
            result.set(ModDataComponents.ARROW_ASSEMBLY.get(), assembly);
        }

        // Only transfer the potion effect for generic recipes (no requiredPotion). A recipe that
        // demands a specific potion keeps its plain result instead of inheriting the effect.
        if (this.requiredPotion.isEmpty()) {
            CustomData customData = input.fluid().get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains("potion")) {
                String potionId = customData.copyTag().getString("potion");
                var potionHolder = BuiltInRegistries.POTION
                        .getHolder(ResourceLocation.parse(potionId)).orElse(null);

                if (potionHolder != null) {
                    result.set(DataComponents.POTION_CONTENTS, new PotionContents(potionHolder));
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

    public static class Serializer implements RecipeSerializer<DippingRecipe> {

        public static final MapCodec<DippingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.inputItem),
                Codec.INT.fieldOf("input_count").orElse(1).forGetter(r -> r.inputCount),
                Codec.STRING.optionalFieldOf("required_potion").forGetter(r -> r.requiredPotion),
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
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, recipe.requiredPotion);
            buf.writeInt(recipe.fluidAmount);
            ItemStack.STREAM_CODEC.encode(buf, recipe.output);
        }

        private static DippingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            return new DippingRecipe(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    buf.readInt(),
                    ItemStack.STREAM_CODEC.decode(buf)
            );
        }
    }
}