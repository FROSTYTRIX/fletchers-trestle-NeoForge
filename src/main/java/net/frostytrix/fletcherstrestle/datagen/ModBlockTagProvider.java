package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, FletcherTrestle.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.SHAVING_HORSE.get())
                .add(ModBlocks.STEAM_BOX.get())
                .add(ModBlocks.DIPPING_VAT.get())
                .add(ModBlocks.ARROW_SLIT.get())
        ;

        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                ;

        this.tag(BlockTags.WOOL)
                .add(ModBlocks.ARCHERY_TARGET.get())
                .add(ModBlocks.ROPE.get())
        ;

        // Linen behaves like wool: shears mine it fast, and the stairs/slabs/
        // carpets join the vanilla families so anything looking for "all
        // stairs" or "all carpets" picks them up.
        var wool = this.tag(BlockTags.WOOL);
        var stairs = this.tag(BlockTags.STAIRS);
        var slabs = this.tag(BlockTags.SLABS);
        var carpets = this.tag(BlockTags.WOOL_CARPETS);
        for (ModBlocks.LinenSet set : ModBlocks.allLinenSets()) {
            wool.add(set.block().get());
            stairs.add(set.stairs().get());
            slabs.add(set.slab().get());
            carpets.add(set.carpet().get());
        }

        this.tag(BlockTags.CLIMBABLE)
                .add(ModBlocks.ROPE.get());
    }
}
