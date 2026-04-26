package net.frostytrix.fletcherstrestle.tags;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static class Items {
        // Define our custom tags
        public static final TagKey<Item> BOW_LIMBS = create("bow_limbs");
        public static final TagKey<Item> BOW_RISERS = create("bow_risers");
        public static final TagKey<Item> BOW_STRINGS = create("bow_strings");
        public static final TagKey<Item> ROUGH_LIMBS = create("rough_limbs");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
        }
    }
}
