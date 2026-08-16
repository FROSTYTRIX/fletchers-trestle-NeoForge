package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.custom.FlaxCropBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, FletcherTrestle.MOD_ID, exFileHelper);
    }


    @Override
    protected void registerStatesAndModels() {
        makeFlaxCrop((CropBlock) (ModBlocks.FLAX_CROP.get()), "flax_stage_", "flax_stage_");

        // Weapon rack: a wall fixture that faces out of the wall it's on. The
        // model is hand-written (backboard + two pegs), so just wire the states.
        horizontalBlock(ModBlocks.WEAPON_RACK.get(),
                models().getExistingFile(modLoc("block/weapon_rack")));
        itemModels().withExistingParent("weapon_rack", modLoc("block/weapon_rack"));

        // Linen: a wool-like decorative block woven from flax string, in 17
        // colours, each with a block, stairs, slab and carpet. Every variant
        // has its own texture (terracotta-style rather than tinted).
        for (ModBlocks.LinenSet set : ModBlocks.allLinenSets()) {
            linenSet(set);
        }
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    /**
     * Blockstates + models for one linen colour. All four blocks share the
     * colour's single texture, so the artist only paints one file per colour.
     */
    private void linenSet(ModBlocks.LinenSet set) {
        blockWithItem(set.block());

        var texture = blockTexture(set.block().get());
        String stairsName = name(set.stairs());
        String slabName = name(set.slab());
        String carpetName = name(set.carpet());

        stairsBlock(set.stairs().get(), texture);
        slabBlock(set.slab().get(), texture, texture);

        // No carpet helper exists, so build the model off the vanilla parent.
        var carpetModel = models().withExistingParent(carpetName, mcLoc("block/carpet"))
                .texture("wool", texture);
        simpleBlock(set.carpet().get(), carpetModel);

        // Item models for the non-cube variants just inherit their block model.
        itemModels().withExistingParent(stairsName, modLoc("block/" + stairsName));
        itemModels().withExistingParent(slabName, modLoc("block/" + slabName));
        itemModels().withExistingParent(carpetName, modLoc("block/" + carpetName));
    }

    private static String name(DeferredBlock<?> block) {
        return block.getId().getPath();
    }

    public void makeFlaxCrop(CropBlock block, String modelName, String textureName) {
        Function<BlockState, ConfiguredModel[]> function = state -> flaxStates(state, block, modelName, textureName);

        getVariantBuilder(block).forAllStates(function);
    }

    private ConfiguredModel[] flaxStates(BlockState state, CropBlock block, String modelName, String textureName) {
        ConfiguredModel[] models = new ConfiguredModel[1];
        models[0] = new ConfiguredModel(models().crop(modelName + state.getValue(((FlaxCropBlock) block).getAgeProperty()),
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "block/" + textureName + state.getValue(((FlaxCropBlock) block).getAgeProperty()))).renderType("cutout"));

        return models;
    }
}
