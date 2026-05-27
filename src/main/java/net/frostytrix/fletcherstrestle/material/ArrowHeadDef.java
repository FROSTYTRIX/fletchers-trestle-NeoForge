package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.ArrowHeadStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of an arrow-head material.
 * Lives at {@code data/<ns>/fletcherstrestle/arrow_head/<id>.json}.
 */
public record ArrowHeadDef(
        Ingredient ingredient,
        ArrowHeadStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects) {

    public static final Codec<ArrowHeadDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ArrowHeadDef::ingredient),
            ArrowHeadStats.CODEC.fieldOf("stats").forGetter(ArrowHeadDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(ArrowHeadDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(ArrowHeadDef::effects)
    ).apply(inst, ArrowHeadDef::new));
}
