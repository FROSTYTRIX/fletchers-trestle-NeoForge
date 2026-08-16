package net.frostytrix.fletcherstrestle.datagen;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.loot.AddItemModifier;
import net.frostytrix.fletcherstrestle.loot.ParrotFeatherModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class ModGlobalLootModifierProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, FletcherTrestle.MOD_ID);
    }

    @Override
    protected void start() {
        this.add("mechanical_trigger_from_pillager_outpost",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/pillager_outpost")).build(),
                        LootItemRandomChanceCondition.randomChance(.7f).build()
                }, ModItems.MECHANICAL_TRIGGER.get()));

        this.add("mechanical_trigger_from_bastion_other",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/bastion_bridge")).build(),
                        LootItemRandomChanceCondition.randomChance(.9f).build()
                }, ModItems.MECHANICAL_TRIGGER.get()));


        this.add("flax_seed_from_short_grass",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.SHORT_GRASS).build(),
                        LootItemRandomChanceCondition.randomChance(.125f).build()
                }, ModItems.FLAX_SEEDS.get()));

        this.add("flax_seed_from_tall_grass",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.TALL_GRASS).build(),
                        LootItemRandomChanceCondition.randomChance(.125f).build()
                }, ModItems.FLAX_SEEDS.get()));

        this.add("flax_seed_from_fern",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.FERN).build(),
                        LootItemRandomChanceCondition.randomChance(.125f).build()
                }, ModItems.FLAX_SEEDS.get()));

        this.add("flax_seed_from_large_fern",
                new AddItemModifier(new LootItemCondition[]{
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(Blocks.LARGE_FERN).build(),
                        LootItemRandomChanceCondition.randomChance(.125f).build()
                }, ModItems.FLAX_SEEDS.get()));

        // Flax in villages. The fletcher and the shepherd are the fibre/string
        // trades, so they stock finished Flax String; the farming houses keep
        // seeds. Gives players a way to find flax (and a first bowstring)
        // without waiting on a grass-drop seed to sprout.
        this.add("flax_string_from_village_fletcher",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_fletcher")).build(),
                        LootItemRandomChanceCondition.randomChance(.85f).build()
                }, ModItems.FLAX_STRING.get(), 2, 5));

        this.add("flax_string_from_village_shepherd",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_shepherd")).build(),
                        LootItemRandomChanceCondition.randomChance(.65f).build()
                }, ModItems.FLAX_STRING.get(), 2, 4));

        this.add("flax_seed_from_village_plains_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_plains_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.65f).build()
                }, ModItems.FLAX_SEEDS.get(), 2, 5));

        this.add("flax_seed_from_village_savanna_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_savanna_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.65f).build()
                }, ModItems.FLAX_SEEDS.get(), 2, 5));

        this.add("flax_seed_from_village_taiga_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_taiga_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.65f).build()
                }, ModItems.FLAX_SEEDS.get(), 2, 5));

        // Raw flax fibre. The shepherd works fibre for a living, the fletcher
        // keeps stock for spinning into string, and farming houses store what
        // they harvested, so all three can turn up unspun flax.
        this.add("flax_from_village_shepherd",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_shepherd")).build(),
                        LootItemRandomChanceCondition.randomChance(.8f).build()
                }, ModItems.FLAX.get(), 4, 8));

        this.add("flax_from_village_fletcher",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_fletcher")).build(),
                        LootItemRandomChanceCondition.randomChance(.75f).build()
                }, ModItems.FLAX.get(), 3, 6));

        this.add("flax_from_village_plains_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_plains_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build()
                }, ModItems.FLAX.get(), 2, 5));

        this.add("flax_from_village_savanna_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_savanna_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build()
                }, ModItems.FLAX.get(), 2, 5));

        this.add("flax_from_village_taiga_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_taiga_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build()
                }, ModItems.FLAX.get(), 2, 5));

        // Farming stashes: the village crop stores. Flax is a farm crop, so a
        // farmer's chest is the most natural place in the world to find it.
        this.add("flax_from_village_desert_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_desert_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build()
                }, ModItems.FLAX.get(), 2, 5));

        this.add("flax_seed_from_village_desert_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_desert_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.65f).build()
                }, ModItems.FLAX_SEEDS.get(), 2, 5));

        this.add("flax_seed_from_village_snowy_house",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_snowy_house")).build(),
                        LootItemRandomChanceCondition.randomChance(.65f).build()
                }, ModItems.FLAX_SEEDS.get(), 2, 5));

        // General world chests. Flax is a common rural good, so it turns up in
        // the ordinary places people stash supplies: kept to modest odds so it
        // stays a nice find rather than clutter.
        this.add("flax_seed_from_village_temple",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/village/village_temple")).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build()
                }, ModItems.FLAX_SEEDS.get(), 1, 4));

        this.add("flax_from_shipwreck_supply",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/shipwreck_supply")).build(),
                        LootItemRandomChanceCondition.randomChance(.6f).build()
                }, ModItems.FLAX.get(), 2, 6));

        this.add("flax_string_from_shipwreck_supply",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/shipwreck_supply")).build(),
                        LootItemRandomChanceCondition.randomChance(.5f).build()
                }, ModItems.FLAX_STRING.get(), 1, 4));

        this.add("flax_seed_from_pillager_outpost",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/pillager_outpost")).build(),
                        LootItemRandomChanceCondition.randomChance(.55f).build()
                }, ModItems.FLAX_SEEDS.get(), 1, 4));

        this.add("flax_string_from_pillager_outpost",
                new AddItemModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("chests/pillager_outpost")).build(),
                        LootItemRandomChanceCondition.randomChance(.55f).build()
                }, ModItems.FLAX_STRING.get(), 1, 3));

        // Parrots drop a feather matching their colour instead of a plain one.
        this.add("coloured_feathers_from_parrot",
                new ParrotFeatherModifier(new LootItemCondition[]{
                        new LootTableIdCondition.Builder(ResourceLocation.withDefaultNamespace("entities/parrot")).build()
                }));
    }
}
