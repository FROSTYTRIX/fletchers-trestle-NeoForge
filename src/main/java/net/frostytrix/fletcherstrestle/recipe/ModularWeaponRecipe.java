package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
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

        // Resolve each part by Ingredient match and store its full
        // namespaced id (e.g. "fletcherstrestle:dark_oak", "mypack:steel").
        // Pre-2.0.0 worlds with bare-path or display-form strings still
        // resolve via MaterialResolver's fallback tiers.
        String limbMat = MaterialResolver.resolveBowLimb(provider, input.topLimb())
                .map(h -> h.key().location().toString())
                .orElse("oak");
        String riserMat = MaterialResolver.resolveBowRiser(provider, input.riser())
                .map(h -> h.key().location().toString())
                .orElse("wood");
        String stringMat = MaterialResolver.resolveBowString(provider, input.string())
                .map(h -> h.key().location().toString())
                .orElse("spider");

        float defaultTuning = 0.0f;

        // FIXED ORDER: limbMat first, then riserMat!
        BowAssembly assembly = new BowAssembly(limbMat, riserMat, stringMat, defaultTuning);
        output.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);

        return output;
    }

    /**
     * Legacy material-name extractor. Kept because external code (e.g. the
     * JEI fletching category) called it on hand-built sample stacks where
     * no registry lookup is available. Returns the canonical id form
     * ({@code "dark_oak"}, {@code "high_tension"}) rather than the old
     * display form ("Dark Oak") to match the new storage format.
     *
     * @deprecated prefer {@link MaterialResolver#resolveBowLimb(HolderLookup.Provider, ItemStack)}
     *             / {@code resolveBowRiser} / {@code resolveBowString} when
     *             a {@link HolderLookup.Provider} is available — they
     *             account for modpack-supplied materials too.
     */
    @Deprecated
    public static String getMaterialName(ItemStack stack) {
        if (stack.isEmpty()) return "unknown";

        if (stack.is(net.minecraft.world.item.Items.STRING)) return "spider";
        if (stack.is(net.minecraft.world.item.Items.STICK))  return "oak";

        // Get the registry path (e.g. "dark_oak_limb" or "copper_riser")
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        // Strip the part-identifier prefixes / suffixes.
        return path
                .replace("_limb", "")
                .replace("pliable_", "")
                .replace("rough_", "")
                .replace("_riser", "")
                .replace("_string", "");
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