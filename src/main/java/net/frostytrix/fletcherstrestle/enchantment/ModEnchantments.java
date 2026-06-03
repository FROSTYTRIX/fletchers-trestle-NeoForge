package net.frostytrix.fletcherstrestle.enchantment;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> PHOTOSYNTHESIS = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "photosynthesis")
    );

    public static final ResourceKey<Enchantment> BIOLUMINESCENCE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "bioluminescence")
    );

    public static final ResourceKey<Enchantment> GALE_FORCE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "gale_force")
    );

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);

        context.register(PHOTOSYNTHESIS, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ItemTags.BOW_ENCHANTABLE), // supported_items
                        2, // weight
                        3, // max_level
                        Enchantment.dynamicCost(10, 5), // min_cost
                        Enchantment.dynamicCost(60, 5), // max_cost
                        4, // anvil_cost
                        EquipmentSlotGroup.MAINHAND,
                        EquipmentSlotGroup.OFFHAND
                )
        ).build(PHOTOSYNTHESIS.location()));

        context.register(BIOLUMINESCENCE, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ItemTags.BOW_ENCHANTABLE), // supported_items
                        2, // weight
                        1, // max_level
                        Enchantment.dynamicCost(10, 5), // min_cost
                        Enchantment.dynamicCost(20, 5), // max_cost
                        1, // anvil_cost
                        EquipmentSlotGroup.MAINHAND,
                        EquipmentSlotGroup.OFFHAND
                )
        ).build(BIOLUMINESCENCE.location()));

        context.register(GALE_FORCE, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ItemTags.BOW_ENCHANTABLE),
                        2, // weight
                        3, // max_level
                        Enchantment.dynamicCost(15, 10), // min_cost
                        Enchantment.dynamicCost(65, 10), // max_cost
                        4, // anvil_cost
                        EquipmentSlotGroup.MAINHAND,
                        EquipmentSlotGroup.OFFHAND
                )
        ).build(GALE_FORCE.location()));
    }
}