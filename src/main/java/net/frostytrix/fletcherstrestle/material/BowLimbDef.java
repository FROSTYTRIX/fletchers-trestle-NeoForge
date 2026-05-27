package net.frostytrix.fletcherstrestle.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Datapack definition of a bow-limb material.
 *
 * <p>Lives at {@code data/<ns>/fletcherstrestle/bow_limb/<id>.json}.
 * The file's id (e.g. {@code fletcherstrestle:oak}) is the canonical material
 * id used by {@link net.frostytrix.fletcherstrestle.component.BowAssembly},
 * texture paths, and tooltip translation keys.</p>
 *
 * @param ingredient        items accepted by the fletching menu's limb slot;
 *                          how modpack makers map a modded item onto this
 *                          material.
 * @param stats             stats consumed at draw / fire time
 * @param texture           optional resource-location of the bow texture for
 *                          this limb; falls back to the conventional path
 *                          {@code <ns>:textures/.../limb/<id>.png}.
 * @param effects           declarative behaviors attached to this material
 *                          (e.g. amphibious aim, slow-falling-while-aiming).
 * @param crossbowOverrides optional per-stat overrides applied when this
 *                          limb is used on a crossbow.
 *
 * @see CrossbowOverrides
 * @see MaterialEffect
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
