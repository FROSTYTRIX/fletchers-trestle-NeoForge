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
 *
 * <p>{@code release_sound} is the sound event id the bow plays when this string
 * looses an arrow. It's a plain id rather than a registry reference, so a pack
 * can point it at a sound defined only in its resource pack's {@code sounds.json}:
 * no sound-event registration needed. Absent means the default release.</p>
 */
public record BowStringDef(
        Ingredient ingredient,
        BowStringStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects,
        Optional<ResourceLocation> releaseSound) {

    public BowStringDef(Ingredient ingredient, BowStringStats stats,
                        Optional<ResourceLocation> texture, List<MaterialEffect> effects) {
        this(ingredient, stats, texture, effects, Optional.empty());
    }

    public static final Codec<BowStringDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BowStringDef::ingredient),
            BowStringStats.CODEC.fieldOf("stats").forGetter(BowStringDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(BowStringDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BowStringDef::effects),
            ResourceLocation.CODEC.optionalFieldOf("release_sound").forGetter(BowStringDef::releaseSound)
    ).apply(inst, BowStringDef::new));
}
