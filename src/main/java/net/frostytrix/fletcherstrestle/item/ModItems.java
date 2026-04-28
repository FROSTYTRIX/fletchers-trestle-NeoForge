package net.frostytrix.fletcherstrestle.item;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.item.custom.ModularArrowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FletcherTrestle.MOD_ID);

    // 0. The Knife
    public static final DeferredItem<Item> DRAWKNIFE = ITEMS.register("drawknife",
            () -> new Item(new Item.Properties().durability(250)));

    // 1. Limbs
    public static final DeferredItem<Item> ROUGH_OAK_LIMB = ITEMS.register("rough_oak_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_OAK_LIMB = ITEMS.register("pliable_oak_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_SPRUCE_LIMB = ITEMS.register("rough_spruce_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_SPRUCE_LIMB = ITEMS.register("pliable_spruce_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_BIRCH_LIMB = ITEMS.register("rough_birch_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_BIRCH_LIMB = ITEMS.register("pliable_birch_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_JUNGLE_LIMB = ITEMS.register("rough_jungle_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_JUNGLE_LIMB = ITEMS.register("pliable_jungle_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_ACACIA_LIMB = ITEMS.register("rough_acacia_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_ACACIA_LIMB = ITEMS.register("pliable_acacia_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_DARK_OAK_LIMB = ITEMS.register("rough_dark_oak_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_DARK_OAK_LIMB = ITEMS.register("pliable_dark_oak_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_MANGROVE_LIMB = ITEMS.register("rough_mangrove_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_MANGROVE_LIMB = ITEMS.register("pliable_mangrove_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_CHERRY_LIMB = ITEMS.register("rough_cherry_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_CHERRY_LIMB = ITEMS.register("pliable_cherry_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_PALE_OAK_LIMB = ITEMS.register("rough_pale_oak_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_PALE_OAK_LIMB = ITEMS.register("pliable_pale_oak_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_CRIMSON_LIMB = ITEMS.register("rough_crimson_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_CRIMSON_LIMB = ITEMS.register("pliable_crimson_limb",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ROUGH_WARPED_LIMB = ITEMS.register("rough_warped_limb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PLIABLE_WARPED_LIMB = ITEMS.register("pliable_warped_limb",
            () -> new Item(new Item.Properties()));


    // 2. Risers
    public static final DeferredItem<Item> WOOD_RISER = ITEMS.register("wood_riser",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> IRON_RISER = ITEMS.register("iron_riser",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COPPER_RISER = ITEMS.register("copper_riser",
            () -> new Item(new Item.Properties()));


    // Flax

    public static final DeferredItem<Item> FLAX_SEEDS = ITEMS.register("flax_seeds",
            () -> new ItemNameBlockItem(ModBlocks.FLAX_CROP.get(),new Item.Properties()));

    public static final DeferredItem<Item> FLAX = ITEMS.register("flax",
            () -> new Item(new Item.Properties()));








    public static final DeferredItem<Item> MODULAR_BOW = ITEMS.register("modular_bow",
            () -> new ModularBowItem(new Item.Properties().durability(384)));

    // 3. Strings
    public static final DeferredItem<Item> HIGH_TENSION_STRING = ITEMS.register("high_tension_string",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> FLAX_STRING = ITEMS.register("flax_string",
            () -> new Item(new Item.Properties()));

    // Arrows

    public static final DeferredItem<Item> MODULAR_ARROW = ITEMS.register("modular_arrow",
            () -> new ModularArrowItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
