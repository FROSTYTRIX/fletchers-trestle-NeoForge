package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public Ingredient getHead() { return head; }
    public Ingredient getShaft() { return shaft; }
    public Ingredient getFletching() { return fletching; }

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

        String headName = getArrowHead(input.head());
        String shaftName = getArrowShaft(input.shaft());
        String fletchName = getArrowFletching(input.fletching());

        // Fallbacks just in case
        if (headName == null) headName = "flint";
        if (shaftName == null) shaftName = "oak";
        if (fletchName == null) fletchName = "feather";

        ArrowAssembly assembly = new ArrowAssembly(headName, shaftName, fletchName);
        output.set(ModDataComponents.ARROW_ASSEMBLY.get(), assembly);

        return output;
    }

    // Your exact mappings moved here!
    private String getArrowHead(ItemStack stack) {
        if (stack.is(Items.FLINT)) return "flint";
        if (stack.is(Items.IRON_INGOT)) return "broadhead";
        if (stack.is(Items.COPPER_INGOT)) return "bodkin_point";
        if (stack.is(Items.ECHO_SHARD)) return "resonance_tip";
        if (stack.is(Items.IRON_NUGGET)) return "barbed_tip";
        if (stack.is(Items.GOLD_INGOT)) return "weighted_blunt";
        if (stack.is(ModItems.WEIGHTED_HOOK)) return "weighted_hook";
        if (stack.is(ModBlocks.ROPE.asItem())) return "trailing_rope";
        return null;
    }

    private String getArrowShaft(ItemStack stack) {
        if (stack.is(ModItems.ROUGH_OAK_LIMB.get())) return "oak";
        if (stack.is(Items.STICK)) return "oak";
        if (stack.is(ModItems.ROUGH_SPRUCE_LIMB.get())) return "spruce";
        if (stack.is(ModItems.ROUGH_BIRCH_LIMB.get())) return "birch";
        if (stack.is(ModItems.ROUGH_DARK_OAK_LIMB.get())) return "dark_oak";
        if (stack.is(ModItems.ROUGH_JUNGLE_LIMB.get())) return "jungle";
        if (stack.is(ModItems.ROUGH_ACACIA_LIMB.get())) return "acacia";
        if (stack.is(ModItems.ROUGH_MANGROVE_LIMB.get())) return "mangrove";
        if (stack.is(ModItems.ROUGH_CHERRY_LIMB.get())) return "cherry";
        if (stack.is(ModItems.ROUGH_PALE_OAK_LIMB.get())) return "pale_oak";
        if (stack.is(ModItems.ROUGH_CRIMSON_LIMB.get())) return "crimson";
        if (stack.is(ModItems.ROUGH_WARPED_LIMB.get())) return "warped";
        return null;
    }

    private String getArrowFletching(ItemStack stack) {
        if (stack.is(Items.FEATHER)) return "feather";
        if (stack.is(Items.FLINT)) return "rigid";
        if (stack.is(Items.STRING)) return "trailing";
        if (stack.is(Items.PHANTOM_MEMBRANE)) return "serrated";
        if (stack.is(Items.LEATHER)) return "bound";
        if (stack.is(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE)) return "vex";
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return true; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) { return this.result; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.MODULAR_ARROW_SERIALIZER.get(); }

    @Override
    public RecipeType<?> getType() { return ModRecipes.MODULAR_ARROW_TYPE.get(); }

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

        @Override public MapCodec<ModularArrowRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ModularArrowRecipe> streamCodec() { return STREAM_CODEC; }

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