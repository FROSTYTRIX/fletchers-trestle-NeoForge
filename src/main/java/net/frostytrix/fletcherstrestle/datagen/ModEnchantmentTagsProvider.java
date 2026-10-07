package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.enchantment.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModEnchantmentTagsProvider extends EnchantmentTagsProvider {

    public ModEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, FletcherTrestle.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(EnchantmentTags.IN_ENCHANTING_TABLE)
                .add(ModEnchantments.PHOTOSYNTHESIS)
                .add(ModEnchantments.BIOLUMINESCENCE)
                .add(ModEnchantments.GALE_FORCE)
                .add(ModEnchantments.TINKERS_MARK)
                .add(ModEnchantments.QUICK_NOCK)
                .add(ModEnchantments.FOLLOW_THROUGH);

        // Librarians sell them as books, like any vanilla table enchantment.
        this.tag(EnchantmentTags.TRADEABLE)
                .add(ModEnchantments.PHOTOSYNTHESIS)
                .add(ModEnchantments.BIOLUMINESCENCE)
                .add(ModEnchantments.GALE_FORCE)
                .add(ModEnchantments.TINKERS_MARK)
                .add(ModEnchantments.QUICK_NOCK)
                .add(ModEnchantments.FOLLOW_THROUGH);

        // The Fletcher's enchanted bows and crossbows roll from this tag, so the
        // mod's own trader can sell the mod's own enchantments.
        this.tag(EnchantmentTags.ON_TRADED_EQUIPMENT)
                .add(ModEnchantments.PHOTOSYNTHESIS)
                .add(ModEnchantments.BIOLUMINESCENCE)
                .add(ModEnchantments.GALE_FORCE)
                .add(ModEnchantments.TINKERS_MARK)
                .add(ModEnchantments.QUICK_NOCK)
                .add(ModEnchantments.FOLLOW_THROUGH);
    }
}