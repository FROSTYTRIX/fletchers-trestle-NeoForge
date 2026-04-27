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

    }

    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
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
