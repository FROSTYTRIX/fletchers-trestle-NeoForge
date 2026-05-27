package net.frostytrix.fletcherstrestle.material.builtin;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.material.BowStringDef;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
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
 * Built-in bow-string material defs. Mirrors the legacy
 * {@code ModularBowItem.StringStats} enum 1:1.
 *
 * <p>Note: SPIDER maps to vanilla {@link Items#STRING}, not to a custom item.
 * The legacy enum's "spider" id was just a flavor label.</p>
 */
public final class BuiltinBowStrings {
    private BuiltinBowStrings() {}

    public static final ResourceKey<BowStringDef> SPIDER       = key("spider");
    public static final ResourceKey<BowStringDef> FLAX         = key("flax");
    public static final ResourceKey<BowStringDef> HIGH_TENSION = key("high_tension");

    public static void bootstrap(BootstrapContext<BowStringDef> ctx) {
        // Spider — vanilla string. 1× velocity, 1 durability per shot.
        ctx.register(SPIDER, new BowStringDef(
                Ingredient.of(Items.STRING),
                new BowStringStats(1.0f, 1),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
        // Flax — mod's own string item, 1.3× velocity.
        ctx.register(FLAX, new BowStringDef(
                Ingredient.of(ModItems.FLAX_STRING.get()),
                new BowStringStats(1.3f, 1),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
        // High-tension — 1.8× velocity, costs 2 durability per shot.
        ctx.register(HIGH_TENSION, new BowStringDef(
                Ingredient.of(ModItems.HIGH_TENSION_STRING.get()),
                new BowStringStats(1.8f, 2),
                Optional.empty(),
                List.<MaterialEffect>of()
        ));
    }

    private static ResourceKey<BowStringDef> key(String name) {
        return ResourceKey.create(ModMaterialRegistries.BOW_STRING,
                ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name));
    }
}
