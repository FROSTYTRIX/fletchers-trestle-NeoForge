package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of a bow-limb material, at {@code data/<ns>/fletcherstrestle/bow_limb/<id>.json}.
 * The file id (e.g. {@code fletcherstrestle:oak}) is the canonical material id used by assemblies,
 * texture paths and tooltip keys.
 *
 * @param ingredient        items accepted by the fletching menu's limb slot
 * @param stats             stats consumed at draw/fire time
 * @param texture           optional bow-texture override; else the conventional path
 * @param effects           declarative behaviors attached to this material
 * @param crossbowOverrides optional per-stat overrides applied on a crossbow
 */
public record BowLimbDef(
        Ingredient ingredient,
        BowLimbStats stats,
        Optional<ResourceLocation> texture,
        List<MaterialEffect> effects,
        Optional<CrossbowOverrides> crossbowOverrides) {

    public static final Codec<BowLimbDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BowLimbDef::ingredient),
            BowLimbStats.CODEC.fieldOf("stats").forGetter(BowLimbDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(BowLimbDef::texture),
            MaterialEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BowLimbDef::effects),
            CrossbowOverrides.CODEC.optionalFieldOf("crossbow_overrides").forGetter(BowLimbDef::crossbowOverrides)
    ).apply(inst, BowLimbDef::new));
}
