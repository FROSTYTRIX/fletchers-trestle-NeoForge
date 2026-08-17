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

    public static final Supplier<RecipeSerializer<GarlandRecipe>> GARLAND_SERIALIZER =
            SERIALIZERS.register("garland", () ->
                    new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(GarlandRecipe::new));

    public static final Supplier<RecipeSerializer<ModularWeaponRecipe>> MODULAR_WEAPON_SERIALIZER =
            SERIALIZERS.register("modular_weapon_assembly", ModularWeaponRecipe.Serializer::new);

    public static final Supplier<RecipeType<ModularWeaponRecipe>> MODULAR_WEAPON_TYPE =
            TYPES.register("modular_weapon_assembly", () -> new RecipeType<ModularWeaponRecipe>() {
                @Override
                public String toString() {
                    return "modular_weapon_assembly";
                }
            });

    public static final Supplier<RecipeSerializer<ModularArrowRecipe>> MODULAR_ARROW_SERIALIZER =
            SERIALIZERS.register("modular_arrow_assembly", ModularArrowRecipe.Serializer::new);

    public static final Supplier<RecipeType<ModularArrowRecipe>> MODULAR_ARROW_TYPE =
            TYPES.register("modular_arrow_assembly", () -> new RecipeType<ModularArrowRecipe>() {
                @Override
                public String toString() {
                    return "modular_arrow_assembly";
                }
            });

    public static final Supplier<RecipeSerializer<SteamingRecipe>> STEAMING_SERIALIZER =
            SERIALIZERS.register("steaming", SteamingRecipe.Serializer::new);

    public static final Supplier<RecipeType<SteamingRecipe>> STEAMING_TYPE =
            TYPES.register("steaming", () -> new RecipeType<SteamingRecipe>() {
                @Override
                public String toString() {
                    return "steaming";
                }
            });

    public static final Supplier<RecipeSerializer<ShavingHorseRecipe>> SHAVING_SERIALIZER =
            SERIALIZERS.register("shaving", ShavingHorseRecipe.Serializer::new);

    public static final Supplier<RecipeType<ShavingHorseRecipe>> SHAVING_TYPE =
            TYPES.register("shaving", () -> new RecipeType<ShavingHorseRecipe>() {
                @Override
                public String toString() {
                    return "shaving";
                }
            });

    public static final Supplier<RecipeSerializer<DippingRecipe>> DIPPING_SERIALIZER =
            SERIALIZERS.register("dipping", DippingRecipe.Serializer::new);

    public static final Supplier<RecipeType<DippingRecipe>> DIPPING_TYPE =
            TYPES.register("dipping", () -> new RecipeType<DippingRecipe>() {
                @Override
                public String toString() {
                    return "dipping";
                }
            });
}