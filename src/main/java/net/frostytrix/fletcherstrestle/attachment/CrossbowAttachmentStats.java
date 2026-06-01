package net.frostytrix.fletcherstrestle.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Numeric stats for a crossbow attachment. Every field is optional and
 * defaults to a no-op (1.0 multipliers, magazine of 1), so a def only needs
 * to declare the stats it actually changes.
 *
 * @param zoom                 FOV zoom factor while aiming (scopes). 1.0 = none.
 * @param swayMultiplier       aim-sway multiplier (scopes). &lt;1.0 = steadier.
 * @param inaccuracyMultiplier projectile spread multiplier. &lt;1.0 = tighter.
 * @param reloadMultiplier     reload-time multiplier. &gt;1.0 = slower (magazines).
 * @param magazineSize         shots before a reload is required (magazines).
 * @param meleeDamage          bonus melee attack damage while wielded (bayonets). 0 = none.
 */
public record CrossbowAttachmentStats(
        float zoom,
        float swayMultiplier,
        float inaccuracyMultiplier,
        float reloadMultiplier,
        int magazineSize,
        float meleeDamage) {

    /** No-op stats — used as the default when a def omits the `stats` block. */
    public static final CrossbowAttachmentStats DEFAULT =
            new CrossbowAttachmentStats(1.0f, 1.0f, 1.0f, 1.0f, 1, 0.0f);

    public static final Codec<CrossbowAttachmentStats> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("zoom", 1.0f).forGetter(CrossbowAttachmentStats::zoom),
            Codec.FLOAT.optionalFieldOf("sway_multiplier", 1.0f).forGetter(CrossbowAttachmentStats::swayMultiplier),
            Codec.FLOAT.optionalFieldOf("inaccuracy_multiplier", 1.0f).forGetter(CrossbowAttachmentStats::inaccuracyMultiplier),
            Codec.FLOAT.optionalFieldOf("reload_multiplier", 1.0f).forGetter(CrossbowAttachmentStats::reloadMultiplier),
            Codec.INT.optionalFieldOf("magazine_size", 1).forGetter(CrossbowAttachmentStats::magazineSize),
            Codec.FLOAT.optionalFieldOf("melee_damage", 0.0f).forGetter(CrossbowAttachmentStats::meleeDamage)
    ).apply(inst, CrossbowAttachmentStats::new));
}
