package net.frostytrix.fletcherstrestle.item;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.item.custom.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// 26.1: Item.Properties needs the id set before the Item constructor runs.
// DeferredRegister.Items.registerItem(name, factory, propsModifier) is the
// 26.1 idiom — the registry sets the id on a fresh Properties, passes it
// through the modifier you supply, then hands it to the factory.
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FletcherTrestle.MOD_ID);

    // 0. The Knife
    public static final DeferredItem<Item> DRAWKNIFE = ITEMS.registerItem("drawknife",
            Item::new, props -> props.durability(250));

    // 1. Limbs
    public static final DeferredItem<Item> ROUGH_OAK_LIMB        = ITEMS.registerSimpleItem("rough_oak_limb");
    public static final DeferredItem<Item> PLIABLE_OAK_LIMB      = ITEMS.registerSimpleItem("pliable_oak_limb");
    public static final DeferredItem<Item> ROUGH_SPRUCE_LIMB     = ITEMS.registerSimpleItem("rough_spruce_limb");
    public static final DeferredItem<Item> PLIABLE_SPRUCE_LIMB   = ITEMS.registerSimpleItem("pliable_spruce_limb");
    public static final DeferredItem<Item> ROUGH_BIRCH_LIMB      = ITEMS.registerSimpleItem("rough_birch_limb");
    public static final DeferredItem<Item> PLIABLE_BIRCH_LIMB    = ITEMS.registerSimpleItem("pliable_birch_limb");
    public static final DeferredItem<Item> ROUGH_JUNGLE_LIMB     = ITEMS.registerSimpleItem("rough_jungle_limb");
    public static final DeferredItem<Item> PLIABLE_JUNGLE_LIMB   = ITEMS.registerSimpleItem("pliable_jungle_limb");
    public static final DeferredItem<Item> ROUGH_ACACIA_LIMB     = ITEMS.registerSimpleItem("rough_acacia_limb");
    public static final DeferredItem<Item> PLIABLE_ACACIA_LIMB   = ITEMS.registerSimpleItem("pliable_acacia_limb");
    public static final DeferredItem<Item> ROUGH_DARK_OAK_LIMB   = ITEMS.registerSimpleItem("rough_dark_oak_limb");
    public static final DeferredItem<Item> PLIABLE_DARK_OAK_LIMB = ITEMS.registerSimpleItem("pliable_dark_oak_limb");
    public static final DeferredItem<Item> ROUGH_MANGROVE_LIMB   = ITEMS.registerSimpleItem("rough_mangrove_limb");
    public static final DeferredItem<Item> PLIABLE_MANGROVE_LIMB = ITEMS.registerSimpleItem("pliable_mangrove_limb");
    public static final DeferredItem<Item> ROUGH_CHERRY_LIMB     = ITEMS.registerSimpleItem("rough_cherry_limb");
    public static final DeferredItem<Item> PLIABLE_CHERRY_LIMB   = ITEMS.registerSimpleItem("pliable_cherry_limb");
    public static final DeferredItem<Item> ROUGH_PALE_OAK_LIMB   = ITEMS.registerSimpleItem("rough_pale_oak_limb");
    public static final DeferredItem<Item> PLIABLE_PALE_OAK_LIMB = ITEMS.registerSimpleItem("pliable_pale_oak_limb");
    public static final DeferredItem<Item> ROUGH_CRIMSON_LIMB    = ITEMS.registerSimpleItem("rough_crimson_limb");
    public static final DeferredItem<Item> PLIABLE_CRIMSON_LIMB  = ITEMS.registerSimpleItem("pliable_crimson_limb");
    public static final DeferredItem<Item> ROUGH_WARPED_LIMB     = ITEMS.registerSimpleItem("rough_warped_limb");
    public static final DeferredItem<Item> PLIABLE_WARPED_LIMB   = ITEMS.registerSimpleItem("pliable_warped_limb");

    // 2. Risers
    public static final DeferredItem<Item> WOOD_RISER   = ITEMS.registerSimpleItem("wood_riser");
    public static final DeferredItem<Item> IRON_RISER   = ITEMS.registerSimpleItem("iron_riser");
    public static final DeferredItem<Item> COPPER_RISER = ITEMS.registerSimpleItem("copper_riser");

    // Flax
    // TODO(port-26.1): ItemNameBlockItem removed; this is now just a BlockItem
    // pointing at FLAX_CROP. Custom seed name lives on the component instead.
    public static final DeferredItem<BlockItem> FLAX_SEEDS = ITEMS.registerItem("flax_seeds",
            props -> new BlockItem(ModBlocks.FLAX_CROP.get(), props));

    public static final DeferredItem<Item> FLAX = ITEMS.registerSimpleItem("flax");

    public static final DeferredItem<ModularBowItem> MODULAR_BOW = ITEMS.registerItem("modular_bow",
            ModularBowItem::new, props -> props.durability(384));

    public static final DeferredItem<ModularCrossbowItem> MODULAR_CROSSBOW = ITEMS.registerItem("modular_crossbow",
            ModularCrossbowItem::new, props -> props.durability(384));

    public static final DeferredItem<Item> MECHANICAL_TRIGGER = ITEMS.registerSimpleItem("mechanical_trigger");

    // 3. Strings
    public static final DeferredItem<Item> HIGH_TENSION_STRING = ITEMS.registerSimpleItem("high_tension_string");
    public static final DeferredItem<Item> FLAX_STRING         = ITEMS.registerSimpleItem("flax_string");

    // Arrows
    public static final DeferredItem<ModularArrowItem> MODULAR_ARROW = ITEMS.registerItem("modular_arrow",
            ModularArrowItem::new);

    public static final DeferredItem<Item> WEIGHTED_HOOK = ITEMS.registerSimpleItem("weighted_hook");

    public static final DeferredItem<ModularQuiverItem> LEATHER_QUIVER = ITEMS.registerItem("leather_quiver",
            ModularQuiverItem::new,
            props -> props.stacksTo(1).component(ModDataComponents.MAX_QUIVER_SLOTS.get(), 3));

    public static final DeferredItem<ModularQuiverItem> IRON_QUIVER = ITEMS.registerItem("iron_quiver",
            ModularQuiverItem::new,
            props -> props.stacksTo(1).component(ModDataComponents.MAX_QUIVER_SLOTS.get(), 5));

    public static final DeferredItem<FletcherGuideItem> FLETCHER_GUIDE = ITEMS.registerItem("fletcher_guide",
            FletcherGuideItem::new, props -> props.stacksTo(1));

    public static final DeferredItem<HeavyDummyItem> HEAVY_DUMMY_ITEM = ITEMS.registerItem("heavy_dummy",
            HeavyDummyItem::new);

    // 26.1: SpawnEggItem(Properties) only. Entity type comes from
    // Properties.spawnEgg(EntityType). Colors are no longer set in code —
    // they live on the entity's TypedEntityData component (default tint
    // until configured via data component).
    public static final DeferredItem<SpawnEggItem> EAGLE_SPAWN_EGG = ITEMS.registerItem("eagle_spawn_egg",
            SpawnEggItem::new, props -> props.spawnEgg(ModEntities.EAGLE.get()));

    public static final DeferredItem<EagleWhistleItem> EAGLE_WHISTLE = ITEMS.registerItem("eagle_whistle",
            EagleWhistleItem::new, props -> props.stacksTo(1));

    public static final DeferredItem<EagleEggItem> EAGLE_EGG = ITEMS.registerItem("eagle_egg",
            EagleEggItem::new, props -> props.stacksTo(16));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
