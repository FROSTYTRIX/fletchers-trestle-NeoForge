package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class ModularWeaponRecipe implements Recipe<FletchingRecipeInput> {

    private final Ingredient riser;
    private final Ingredient limbs;
    private final Ingredient string;
    private final ItemStack result;

    public ModularWeaponRecipe(Ingredient riser, Ingredient limbs, Ingredient string, ItemStack result) {
        this.riser = riser;
        this.limbs = limbs;
        this.string = string;
        this.result = result;
    }

    @Override
    public boolean matches(FletchingRecipeInput input, Level level) {
        if (input.isEmpty()) return false;

        // Checks if the items in the slots match the ingredients defined in the JSON
        return this.riser.test(input.riser()) &&
                this.limbs.test(input.topLimb()) &&
                this.limbs.test(input.bottomLimb()) &&
                this.string.test(input.string());
    }

    @Override
    public ItemStack assemble(FletchingRecipeInput input, HolderLookup.Provider provider) {
        ItemStack output = this.result.copy();

        String riserMat = getMaterialName(input.riser());
        String limbMat = getMaterialName(input.topLimb());
        String stringMat = getMaterialName(input.string());

        float defaultTuning = 0.0f;

        // FIXED ORDER: limbMat first, then riserMat!
        BowAssembly assembly = new BowAssembly(limbMat, riserMat, stringMat, defaultTuning);
        output.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);

        return output;
    }

    // Helper method to extract your material names from the input items.
    // E.g., if the item is "copper_riser", return "copper"
    private String getMaterialName(ItemStack stack) {
        if (stack.isEmpty()) return "Unknown";

        // Handle vanilla items that don't match your naming convention
        if (stack.is(net.minecraft.world.item.Items.STRING)) return "Spider";
        if (stack.is(net.minecraft.world.item.Items.STICK)) return "Oak";

        // Get the registry path (e.g., "dark_oak_limb" or "copper_riser")
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        // Strip away the part identifiers
        String material = path
                .replace("_limb", "")
                .replace("pliable_", "")
                .replace("rough_", "")
                .replace("_riser", "")
                .replace("_string", "");

        // Automatically capitalize each word (e.g., "dark_oak" -> "Dark Oak")
        return java.util.Arrays.stream(material.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
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
        return ModRecipes.MODULAR_WEAPON_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MODULAR_WEAPON_TYPE.get();
    }

    // Modern 1.21 Codecs
    public static class Serializer implements RecipeSerializer<ModularWeaponRecipe> {
        public static final MapCodec<ModularWeaponRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("riser").forGetter(r -> r.riser),
                Ingredient.CODEC_NONEMPTY.fieldOf("limbs").forGetter(r -> r.limbs),
                Ingredient.CODEC_NONEMPTY.fieldOf("string").forGetter(r -> r.string),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(inst, ModularWeaponRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ModularWeaponRecipe> STREAM_CODEC = StreamCodec.of(
                ModularWeaponRecipe.Serializer::toNetwork, ModularWeaponRecipe.Serializer::fromNetwork
        );

        @Override
        public MapCodec<ModularWeaponRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ModularWeaponRecipe> streamCodec() { return STREAM_CODEC; }

        private static ModularWeaponRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            Ingredient riser = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient limbs = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient string = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            return new ModularWeaponRecipe(riser, limbs, string, result);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, ModularWeaponRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.riser);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.limbs);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.string);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
        }
    }

    public net.minecraft.world.item.crafting.Ingredient getRiser() { return this.riser; }
    public net.minecraft.world.item.crafting.Ingredient getLimbs() { return this.limbs; }
    public net.minecraft.world.item.crafting.Ingredient getString() { return this.string; }
}