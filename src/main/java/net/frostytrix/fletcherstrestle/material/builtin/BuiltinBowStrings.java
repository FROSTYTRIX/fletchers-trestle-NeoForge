package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.BowStringDef;
import net.frostytrix.fletcherstrestle.material.ModMaterialRegistries;
import net.frostytrix.fletcherstrestle.material.stats.BowStringStats;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Built-in bow-string material defs. Note SPIDER maps to vanilla {@link Items#STRING} (the id is
 * just a flavor label), not a custom item.
 */
public final class BuiltinBowStrings {
    private BuiltinBowStrings() {
    }

    public static final ResourceKey<BowStringDef> SPIDER = key("spider");
    public static final ResourceKey<BowStringDef> FLAX = key("flax");
    public static final ResourceKey<BowStringDef> HIGH_TENSION = key("high_tension");

    public static void bootstrap(BootstrapContext<BowStringDef> ctx) {
        // Spider: vanilla string. 1× velocity, 1 durability per shot.
        ctx.register(SPIDER, new BowStringDef(
                Ingredient.of(Items.STRING),
                new BowStringStats(1.0f, 1, false),
                Optional.empty(),
                List.of()
        ));
        // Flax: mod's own string item. 0.85× velocity: farmable and renewable,
        // so it trades real power (and the shaky overdraw) for convenience.
        ctx.register(FLAX, new BowStringDef(
                Ingredient.of(ModItems.FLAX_STRING.get()),
                new BowStringStats(0.85f, 1, false),
                Optional.empty(),
                List.of()
        ));
        // High-tension: 1.4× velocity, costs 2 durability per shot.
        ctx.register(HIGH_TENSION, new BowStringDef(
                Ingredient.of(ModItems.HIGH_TENSION_STRING.get()),
                new BowStringStats(1.4f, 2, true),
                Optional.empty(),
                List.of()
        ));
    }

    private static ResourceKey<BowStringDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.BOW_STRING,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
