package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.custom.FlaxCropBlock;
import net.frostytrix.fletcherstrestle.block.custom.RopeBlock;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.STEAM_BOX.get());
        dropSelf(ModBlocks.SHAVING_HORSE.get());
        dropSelf(ModBlocks.ARCHERY_TARGET.get());
        dropSelf(ModBlocks.DIPPING_VAT.get());
        // Pre-existing gap: every registered block needs a loot table entry
        // here or BlockLootSubProvider.generate throws on datagen.
        dropSelf(ModBlocks.EAGLE_PERCH.get());
        dropSelf(ModBlocks.EAGLE_NEST.get());

        LootItemCondition.Builder lootitemcondition$builder = LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(ModBlocks.FLAX_CROP.get())
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(FlaxCropBlock.AGE, 7))
                .or(LootItemBlockStatePropertyCondition
                        .hasBlockStateProperties(ModBlocks.FLAX_CROP.get())
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(FlaxCropBlock.AGE, 8)));

        this.add(ModBlocks.FLAX_CROP.get(), createCropDrops(ModBlocks.FLAX_CROP.get(), ModItems.FLAX.get(),
                ModItems.FLAX_SEEDS.get(), lootitemcondition$builder));

        this.add(ModBlocks.ROPE.get(), block -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModBlocks.ROPE.get())) // The item to drop
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                // THE CONDITION: Only drop if PERSISTENT is true
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(RopeBlock.PERSISTENT, true))
                        )
                )
        );

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }

}
