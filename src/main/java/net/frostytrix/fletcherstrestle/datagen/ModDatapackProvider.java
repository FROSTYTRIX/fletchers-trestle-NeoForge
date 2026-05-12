package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.enchantment.ModEnchantments;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModDatapackProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);

    public ModDatapackProvider(PackOutput output, CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
        // The 'Set.of(FletcherTrestle.MOD_ID)' ensures it generates in your mod's namespace
        super(output, registries, BUILDER, Set.of(FletcherTrestle.MOD_ID));
    }
}