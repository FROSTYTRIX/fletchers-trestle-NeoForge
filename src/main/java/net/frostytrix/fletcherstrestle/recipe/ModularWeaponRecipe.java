package net.frostytrix.fletcherstrestle.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class ModularWeaponRecipe implements Recipe<FletchingRecipeInput> {

    private final Ingredient riser;
    private final Ingredient limbs;
    private final Ingredient string;
    // 26.1: ItemStackTemplate result — see ShavingHorseRecipe note.
    private final ItemStackTemplate result;

    public ModularWeaponRecipe(Ingredient riser, Ingredient limbs, Ingredient string, ItemStackTemplate result) {
        this.riser = riser;
        this.limbs = limbs;
        this.string = string;
        this.result = result;
    }

    @Override
    public boolean matches(FletchingRecipeInput input, Level level) {
        if (input.isEmpty()) return false;
        return this.riser.test(input.riser())
                && this.limbs.test(input.topLimb())
                && this.limbs.test(input.bottomLimb())
                && this.string.test(input.string());
    }

    @Override
    public ItemStack assemble(FletchingRecipeInput input) {
        ItemStack output = this.result.create();
        String riserMat = getMaterialName(input.riser());
        String limbMat = getMaterialName(input.topLimb());
        String stringMat = getMaterialName(input.string());
        output.set(ModDataComponents.BOW_ASSEMBLY.get(),
                new BowAssembly(limbMat, riserMat, stringMat, 0.0f));
        return output;
    }

    public static String getMaterialName(ItemStack stack) {
        if (stack.isEmpty()) return "Unknown";
        if (stack.is(net.minecraft.world.item.Items.STRING)) return "Spider";
        if (stack.is(net.minecraft.world.item.Items.STICK)) return "Oak";

        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String material = path
                .replace("_limb", "")
                .replace("pliable_", "")
                .replace("rough_", "")
                .replace("_riser", "")
                .replace("_string", "");
        return java.util.Arrays.stream(material.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    @Override
    public RecipeSerializer<? extends Recipe<FletchingRecipeInput>> getSerializer() {
        return ModRecipes.MODULAR_WEAPON_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<FletchingRecipeInput>> getType() {
        return ModRecipes.MODULAR_WEAPON_TYPE.get();
    }

    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public PlacementInfo placementInfo()           { return PlacementInfo.NOT_PLACEABLE; }
    @Override public boolean showNotification()              { return false; }
    @Override public String group()                          { return ""; }

    public Ingredient getRiser() { return this.riser; }
    public Ingredient getLimbs() { return this.limbs; }
    public Ingredient getString() { return this.string; }

    public static final MapCodec<ModularWeaponRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Ingredient.CODEC.fieldOf("riser").forGetter(r -> r.riser),
            Ingredient.CODEC.fieldOf("limbs").forGetter(r -> r.limbs),
            Ingredient.CODEC.fieldOf("string").forGetter(r -> r.string),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(inst, ModularWeaponRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularWeaponRecipe> STREAM_CODEC = StreamCodec.of(
            ModularWeaponRecipe::toNetwork, ModularWeaponRecipe::fromNetwork);

    public static RecipeSerializer<ModularWeaponRecipe> serializer() {
        return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, ModularWeaponRecipe recipe) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.riser);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.limbs);
        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.string);
        ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
    }

    private static ModularWeaponRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        return new ModularWeaponRecipe(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                ItemStackTemplate.STREAM_CODEC.decode(buf)
        );
    }
}
