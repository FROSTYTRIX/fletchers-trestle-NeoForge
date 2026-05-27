package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowRiserStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of a bow-riser material.
 * Lives at {@code data/<ns>/fletcherstrestle/bow_riser/<id>.json}.
 *
 * @see BowLimbDef for the shared shape rationale.
 */
public record BowRiserDef(
        Ingredient ingredient,
        BowRiserStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects) {

    public static final Codec<BowRiserDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BowRiserDef::ingredient),
            BowRiserStats.CODEC.fieldOf("stats").forGetter(BowRiserDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(BowRiserDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BowRiserDef::effects)
    ).apply(inst, BowRiserDef::new));
}
