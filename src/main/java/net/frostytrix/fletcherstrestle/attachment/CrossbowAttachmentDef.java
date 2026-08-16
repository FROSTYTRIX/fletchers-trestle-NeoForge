package net.frostytrix.fletcherstrestle.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;

/**
 * Datapack definition of a crossbow attachment.
 *
 * <p>Lives at {@code data/<ns>/fletcherstrestle/crossbow_attachment/<id>.json}.
 * Mirrors the bow/arrow material defs (see {@code material/BowLimbDef}): the
 * file's id is the canonical attachment id used by the crossbow's attachment
 * component, and {@code ingredient} is how a modpack maps any item onto this
 * attachment so the Crossbow Bench accepts it.</p>
 *
 * <p>Registered as a synced datapack registry in
 * {@link ModCrossbowAttachments}: JEI and client-side aim/zoom logic read
 * these, so they ship to clients like the material defs do.</p>
 *
 * @param ingredient items the Crossbow Bench accepts to install this attachment
 * @param type       slot category (see {@link AttachmentType})
 * @param stats      aim / reload stats applied while this attachment is installed
 * @param texture    optional icon/overlay override; falls back to the
 *                   conventional path if absent
 */
public record CrossbowAttachmentDef(
        Ingredient ingredient,
        AttachmentType type,
        CrossbowAttachmentStats stats,
        Optional<ResourceLocation> texture) {

    public static final Codec<CrossbowAttachmentDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrossbowAttachmentDef::ingredient),
            AttachmentType.CODEC.fieldOf("type").forGetter(CrossbowAttachmentDef::type),
            CrossbowAttachmentStats.CODEC.optionalFieldOf("stats", CrossbowAttachmentStats.DEFAULT).forGetter(CrossbowAttachmentDef::stats),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(CrossbowAttachmentDef::texture)
    ).apply(inst, CrossbowAttachmentDef::new));
}
