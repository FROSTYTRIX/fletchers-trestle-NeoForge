package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.enchantment.ModEnchantments;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.builtin.BuiltinArrowFletchings;
import net.frostytrix.fletcherstrestle.material.builtin.BuiltinArrowHeads;
import net.frostytrix.fletcherstrestle.material.builtin.BuiltinArrowShafts;
import net.frostytrix.fletcherstrestle.material.builtin.BuiltinBowLimbs;
import net.frostytrix.fletcherstrestle.material.builtin.BuiltinBowRisers;
import net.frostytrix.fletcherstrestle.material.builtin.BuiltinBowStrings;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.ENCHANTMENT, ModEnchantments::bootstrap)
            // Built-in bow/arrow materials. Modpacks layer their JSONs on top.
            .add(ModMaterialRegistries.BOW_LIMB,        BuiltinBowLimbs::bootstrap)
            .add(ModMaterialRegistries.BOW_RISER,       BuiltinBowRisers::bootstrap)
            .add(ModMaterialRegistries.BOW_STRING,      BuiltinBowStrings::bootstrap)
            .add(ModMaterialRegistries.ARROW_HEAD,      BuiltinArrowHeads::bootstrap)
            .add(ModMaterialRegistries.ARROW_SHAFT,     BuiltinArrowShafts::bootstrap)
            .add(ModMaterialRegistries.ARROW_FLETCHING, BuiltinArrowFletchings::bootstrap);

    public ModDatapackProvider(PackOutput output, CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
        // The 'Set.of(FletcherTrestle.MOD_ID)' ensures it generates in your mod's namespace
        super(output, registries, BUILDER, Set.of(FletcherTrestle.MOD_ID));
    }
}