package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.ArrowFletchingStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of an arrow-fletching material.
 * Lives at {@code data/<ns>/fletcherstrestle/arrow_fletching/<id>.json}.
 */
public record ArrowFletchingDef(
        Ingredient ingredient,
        ArrowFletchingStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects) {

    public static final Codec<ArrowFletchingDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ArrowFletchingDef::ingredient),
            ArrowFletchingStats.CODEC.fieldOf("stats").forGetter(ArrowFletchingDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(ArrowFletchingDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ArrowFletchingDef::effects)
    ).apply(inst, ArrowFletchingDef::new));
}
