package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowStringStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of a bow-string material.
 * Lives at {@code data/<ns>/fletcherstrestle/bow_string/<id>.json}.
 */
public record BowStringDef(
        Ingredient ingredient,
        BowStringStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects) {

    public static final Codec<BowStringDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BowStringDef::ingredient),
            BowStringStats.CODEC.fieldOf("stats").forGetter(BowStringDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(BowStringDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BowStringDef::effects)
    ).apply(inst, BowStringDef::new));
}
