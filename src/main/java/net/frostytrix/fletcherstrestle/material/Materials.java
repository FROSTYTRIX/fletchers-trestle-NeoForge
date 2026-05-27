package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.material.stats.ArrowFletchingStats;
import net.frostytrix.fletcherstrestle.material.stats.ArrowHeadStats;
import net.frostytrix.fletcherstrestle.material.stats.ArrowShaftStats;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;
import net.frostytrix.fletcherstrestle.material.stats.BowRiserStats;
import net.frostytrix.fletcherstrestle.material.stats.BowStringStats;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Thin convenience facade over {@link MaterialResolver}. Each lookup
 * returns a def with a sensible default applied on miss — mirrors the
 * legacy {@code *Stats.fromString(...)} contract of "never null, always
 * returns the OAK/WOOD/SPIDER/etc fallback".
 *
 * <p>Use this from any call site that just wants stat values without
 * caring about resolution failure. Use {@link MaterialResolver} directly
 * if you need {@code Optional} semantics or a {@link Holder.Reference}
 * (e.g. to read the material's id for tooltip translation).</p>
 *
 * <p>The default defs ({@link #DEFAULT_BOW_LIMB} etc.) are also returned
 * during early init / datagen / before the client joins a world — anywhere
 * {@link MaterialResolver} can't find a {@code RegistryAccess}.</p>
 */
public final class Materials {
    private Materials() {}

    public static final BowLimbDef DEFAULT_BOW_LIMB = new BowLimbDef(
            Ingredient.EMPTY,
            new BowLimbStats(20f, 1f, false, false),
            Optional.empty(), List.of(), Optional.empty());

    public static final BowRiserDef DEFAULT_BOW_RISER = new BowRiserDef(
            Ingredient.EMPTY,
            new BowRiserStats(250, 1f),
            Optional.empty(), List.of());

    public static final BowStringDef DEFAULT_BOW_STRING = new BowStringDef(
            Ingredient.EMPTY,
            new BowStringStats(1f, 1),
            Optional.empty(), List.of());

    public static final ArrowHeadDef DEFAULT_ARROW_HEAD = new ArrowHeadDef(
            Ingredient.EMPTY,
            new ArrowHeadStats(1f),
            Optional.empty(), List.of());

    public static final ArrowShaftDef DEFAULT_ARROW_SHAFT = new ArrowShaftDef(
            Ingredient.EMPTY,
            new ArrowShaftStats(1f, 1f),
            Optional.empty(), List.of());

    public static final ArrowFletchingDef DEFAULT_ARROW_FLETCHING = new ArrowFletchingDef(
            Ingredient.EMPTY,
            new ArrowFletchingStats(1f),
            Optional.empty(), List.of());

    public static BowLimbDef bowLimb(String idOrLegacy) {
        return MaterialResolver.resolveBowLimbById(idOrLegacy).map(Holder.Reference::value).orElse(DEFAULT_BOW_LIMB);
    }

    public static BowRiserDef bowRiser(String idOrLegacy) {
        return MaterialResolver.resolveBowRiserById(idOrLegacy).map(Holder.Reference::value).orElse(DEFAULT_BOW_RISER);
    }

    public static BowStringDef bowString(String idOrLegacy) {
        return MaterialResolver.resolveBowStringById(idOrLegacy).map(Holder.Reference::value).orElse(DEFAULT_BOW_STRING);
    }

    public static ArrowHeadDef arrowHead(String idOrLegacy) {
        return MaterialResolver.resolveArrowHeadById(idOrLegacy).map(Holder.Reference::value).orElse(DEFAULT_ARROW_HEAD);
    }

    public static ArrowShaftDef arrowShaft(String idOrLegacy) {
        return MaterialResolver.resolveArrowShaftById(idOrLegacy).map(Holder.Reference::value).orElse(DEFAULT_ARROW_SHAFT);
    }

    public static ArrowFletchingDef arrowFletching(String idOrLegacy) {
        return MaterialResolver.resolveArrowFletchingById(idOrLegacy).map(Holder.Reference::value).orElse(DEFAULT_ARROW_FLETCHING);
    }

    // ---- Display names ----

    /**
     * Returns the translatable display name for a material stored on a
     * {@link net.frostytrix.fletcherstrestle.component.BowAssembly} /
     * {@link net.frostytrix.fletcherstrestle.component.ArrowAssembly}.
     *
     * <p>If the id resolves to a registry entry, returns
     * {@code Component.translatable("material.<namespace>.<path>")} so the
     * built-in lang file (or a modpack's lang file for added materials)
     * controls the player-facing label. Falls back to a Component built
     * off the raw id string when resolution fails — this keeps existing
     * worlds with legacy "Dark Oak" stored strings displaying SOMETHING
     * even if the registry doesn't recognise the string.</p>
     */
    public static Component bowLimbName(String idOrLegacy) {
        return MaterialResolver.resolveBowLimbById(idOrLegacy)
                .map(h -> MaterialResolver.displayName(h.key().location()))
                .orElseGet(() -> Component.literal(idOrLegacy));
    }

    public static Component bowRiserName(String idOrLegacy) {
        return MaterialResolver.resolveBowRiserById(idOrLegacy)
                .map(h -> MaterialResolver.displayName(h.key().location()))
                .orElseGet(() -> Component.literal(idOrLegacy));
    }

    public static Component bowStringName(String idOrLegacy) {
        return MaterialResolver.resolveBowStringById(idOrLegacy)
                .map(h -> MaterialResolver.displayName(h.key().location()))
                .orElseGet(() -> Component.literal(idOrLegacy));
    }

    public static Component arrowHeadName(String idOrLegacy) {
        return MaterialResolver.resolveArrowHeadById(idOrLegacy)
                .map(h -> MaterialResolver.displayName(h.key().location()))
                .orElseGet(() -> Component.literal(idOrLegacy));
    }

    public static Component arrowShaftName(String idOrLegacy) {
        return MaterialResolver.resolveArrowShaftById(idOrLegacy)
                .map(h -> MaterialResolver.displayName(h.key().location()))
                .orElseGet(() -> Component.literal(idOrLegacy));
    }

    public static Component arrowFletchingName(String idOrLegacy) {
        return MaterialResolver.resolveArrowFletchingById(idOrLegacy)
                .map(h -> MaterialResolver.displayName(h.key().location()))
                .orElseGet(() -> Component.literal(idOrLegacy));
    }

    /**
     * Normalises a stored id-or-legacy string to its underscore-path form,
     * stripping any namespace. Used by Phase D code that still compares
     * material identity by string ({@code "crimson".equals(normaliseId(...))}).
     * Phase E will replace most of those compares with effect-driven
     * dispatch and this helper will lose most of its callers.
     */
    public static String normaliseId(String idOrLegacy) {
        if (idOrLegacy == null || idOrLegacy.isEmpty()) return "";
        int colon = idOrLegacy.indexOf(':');
        String path = colon >= 0 ? idOrLegacy.substring(colon + 1) : idOrLegacy;
        return path.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
