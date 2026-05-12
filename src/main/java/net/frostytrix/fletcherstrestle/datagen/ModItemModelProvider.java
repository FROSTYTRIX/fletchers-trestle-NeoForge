package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, FletcherTrestle.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.PLIABLE_OAK_LIMB.get());
        basicItem(ModItems.ROUGH_OAK_LIMB.get());

        basicItem(ModItems.PLIABLE_SPRUCE_LIMB.get());
        basicItem(ModItems.ROUGH_SPRUCE_LIMB.get());

        basicItem(ModItems.PLIABLE_BIRCH_LIMB.get());
        basicItem(ModItems.ROUGH_BIRCH_LIMB.get());

        basicItem(ModItems.PLIABLE_JUNGLE_LIMB.get());
        basicItem(ModItems.ROUGH_JUNGLE_LIMB.get());

        basicItem(ModItems.PLIABLE_ACACIA_LIMB.get());
        basicItem(ModItems.ROUGH_ACACIA_LIMB.get());

        basicItem(ModItems.PLIABLE_DARK_OAK_LIMB.get());
        basicItem(ModItems.ROUGH_DARK_OAK_LIMB.get());

        basicItem(ModItems.PLIABLE_MANGROVE_LIMB.get());
        basicItem(ModItems.ROUGH_MANGROVE_LIMB.get());

        basicItem(ModItems.PLIABLE_CHERRY_LIMB.get());
        basicItem(ModItems.ROUGH_CHERRY_LIMB.get());

        basicItem(ModItems.PLIABLE_PALE_OAK_LIMB.get());
        basicItem(ModItems.ROUGH_PALE_OAK_LIMB.get());

        basicItem(ModItems.PLIABLE_CRIMSON_LIMB.get());
        basicItem(ModItems.ROUGH_CRIMSON_LIMB.get());

        basicItem(ModItems.PLIABLE_WARPED_LIMB.get());
        basicItem(ModItems.ROUGH_WARPED_LIMB.get());

        basicItem(ModItems.WOOD_RISER.get());
        basicItem(ModItems.IRON_RISER.get());
        basicItem(ModItems.COPPER_RISER.get());

        basicItem(ModItems.HIGH_TENSION_STRING.get());
        basicItem(ModItems.FLAX.get());
        basicItem(ModItems.FLAX_SEEDS.get());
        basicItem(ModItems.FLAX_STRING.get());
        handheldItem(ModItems.DRAWKNIFE.get());

        basicItem(ModItems.QUIVER.get());
        basicItem(ModItems.MECHANICAL_TRIGGER.get());
        basicItem(ModItems.HEAVY_DUMMY_ITEM.get());
        basicItem(ModItems.WEIGHTED_HOOK.get());
    }
}
