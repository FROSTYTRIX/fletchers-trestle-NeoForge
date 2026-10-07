package net.frostytrix.fletcherstrestle.tags;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static class Blocks {
        /** Blocks a Bolt Warden can be built from: stripped logs and woods by default. */
        public static final TagKey<net.minecraft.world.level.block.Block> GARRISON_GOLEM_BODY = TagKey.create(
                Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "garrison_golem_body"));
    }

    public static class EntityTypes {
        /** Shot by garrisons even though they aren't hostile mobs. */
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> GARRISON_TARGETS = TagKey.create(
                Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "garrison_targets"));
        /** Never shot by garrisons, hostile or not. */
        public static final TagKey<net.minecraft.world.entity.EntityType<?>> GARRISON_IGNORES = TagKey.create(
                Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "garrison_ignores"));
    }

    public static class Items {
        // Define our custom tags
        public static final TagKey<Item> BOW_LIMBS = create("bow_limbs");
        public static final TagKey<Item> BOW_RISERS = create("bow_risers");
        public static final TagKey<Item> BOW_STRINGS = create("bow_strings");
        public static final TagKey<Item> ROUGH_LIMBS = create("rough_limbs");
        public static final TagKey<Item> ARROW_HEADS = create("arrow_heads");
        public static final TagKey<Item> ARROW_FLETCHING = create("arrow_fletching");
        /** All linen blocks (undyed + every dyed colour): lets dye recipes re-colour any of them. */
        public static final TagKey<Item> LINEN = create("linen");
        /** Strings a garland can be woven on. */
        public static final TagKey<Item> GARLAND_STRINGS = create("garland_strings");
        /** The clockwork part: fits the Crossbow Bench, and winds up a Bolt Warden. */
        public static final TagKey<Item> MECHANISMS = create("mechanisms");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
        }
    }
}
