package net.frostytrix.fletcherstrestle.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, "fletcherstrestle");

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, "fletcherstrestle");

    public static final Supplier<RecipeSerializer<ModularWeaponRecipe>> MODULAR_WEAPON_SERIALIZER =
            SERIALIZERS.register("modular_weapon_assembly", ModularWeaponRecipe.Serializer::new);

    public static final Supplier<RecipeType<ModularWeaponRecipe>> MODULAR_WEAPON_TYPE =
            TYPES.register("modular_weapon_assembly", () -> new RecipeType<ModularWeaponRecipe>() {
                @Override
                public String toString() {
                    return "modular_weapon_assembly";
                }
            });
}