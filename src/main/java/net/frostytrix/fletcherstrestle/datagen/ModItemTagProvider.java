package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, FletcherTrestle.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.BOW_LIMBS)
                .add(ModItems.PLIABLE_OAK_LIMB.get())
                .add(ModItems.PLIABLE_SPRUCE_LIMB.get())
                .add(ModItems.PLIABLE_BIRCH_LIMB.get())
                .add(ModItems.PLIABLE_JUNGLE_LIMB.get())
                .add(ModItems.PLIABLE_ACACIA_LIMB.get())
                .add(ModItems.PLIABLE_DARK_OAK_LIMB.get())
                .add(ModItems.PLIABLE_MANGROVE_LIMB.get())
                .add(ModItems.PLIABLE_CHERRY_LIMB.get())
                .add(ModItems.PLIABLE_PALE_OAK_LIMB.get())
                .add(ModItems.PLIABLE_CRIMSON_LIMB.get())
                .add(ModItems.PLIABLE_WARPED_LIMB.get())
        ;

        tag(ModTags.Items.ROUGH_LIMBS)
                .add(ModItems.ROUGH_OAK_LIMB.get())
                .add(ModItems.ROUGH_SPRUCE_LIMB.get())
                .add(ModItems.ROUGH_BIRCH_LIMB.get())
                .add(ModItems.ROUGH_JUNGLE_LIMB.get())
                .add(ModItems.ROUGH_ACACIA_LIMB.get())
                .add(ModItems.ROUGH_DARK_OAK_LIMB.get())
                .add(ModItems.ROUGH_MANGROVE_LIMB.get())
                .add(ModItems.ROUGH_CHERRY_LIMB.get())
                .add(ModItems.ROUGH_PALE_OAK_LIMB.get())
                .add(ModItems.ROUGH_CRIMSON_LIMB.get())
                .add(ModItems.ROUGH_WARPED_LIMB.get())
        ;

        tag(ModTags.Items.BOW_RISERS)
                .add(ModItems.WOOD_RISER.get())
                .add(ModItems.COPPER_RISER.get())
                .add(ModItems.IRON_RISER.get())
                ;

        tag(ModTags.Items.BOW_STRINGS)
                .add(Items.STRING)
                .add(ModItems.HIGH_TENSION_STRING.get())
                .add(ModItems.FLAX_STRING.get())
                ;
    }
}
