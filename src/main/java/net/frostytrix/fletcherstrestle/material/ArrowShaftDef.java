package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.ArrowShaftStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of an arrow-shaft material.
 * Lives at {@code data/<ns>/fletcherstrestle/arrow_shaft/<id>.json}.
 */
public record ArrowShaftDef(
        Ingredient ingredient,
        ArrowShaftStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects) {

    public static final Codec<ArrowShaftDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ArrowShaftDef::ingredient),
            ArrowShaftStats.CODEC.fieldOf("stats").forGetter(ArrowShaftDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(ArrowShaftDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ArrowShaftDef::effects)
    ).apply(inst, ArrowShaftDef::new));
}
