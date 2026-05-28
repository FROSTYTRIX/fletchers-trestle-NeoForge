package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.material.stats.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private Materials() {
    }

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

    // ---- Texture resolution ----

    /**
     * Resolves a texture {@link ResourceLocation} for a material slot,
     * honoring the def's optional {@code texture} override and falling
     * back to a conventional path otherwise.
     *
     * <p>Convention path: {@code <materialNamespace>:<basePathPrefix>/<materialPath><suffix>}
     * where the material namespace is the namespace of the registered def
     * (so {@code mypack:steel} pulls from {@code mypack:...} by default),
     * and {@code <materialPath>} is the registry-key path of the def
     * (e.g. {@code dark_oak}, {@code high_tension}).</p>
     *
     * <p>If the def carries an explicit {@code texture} field, that
     * location is used as the override base — {@code suffix} is appended
     * to whatever path the override points to so the same suffix scheme
     * (e.g. {@code _limb_pulling_0}) works.</p>
     *
     * <p>Falls back to the mod's own namespace + the supplied id string if
     * the def can't be resolved (e.g. an empty assembly or a legacy
     * display-form string that doesn't normalise to anything known).</p>
     */
    public static ResourceLocation bowLimbTexture(String idString, String basePathPrefix, String suffix) {
        return textureOf(MaterialResolver.resolveBowLimbById(idString),
                h -> h.value().texture(), basePathPrefix, suffix, idString);
    }

    public static ResourceLocation bowRiserTexture(String idString, String basePathPrefix, String suffix) {
        return textureOf(MaterialResolver.resolveBowRiserById(idString),
                h -> h.value().texture(), basePathPrefix, suffix, idString);
    }

    public static ResourceLocation bowStringTexture(String idString, String basePathPrefix, String suffix) {
        return textureOf(MaterialResolver.resolveBowStringById(idString),
                h -> h.value().texture(), basePathPrefix, suffix, idString);
    }

    public static ResourceLocation arrowHeadTexture(String idString, String basePathPrefix, String suffix) {
        return textureOf(MaterialResolver.resolveArrowHeadById(idString),
                h -> h.value().texture(), basePathPrefix, suffix, idString);
    }

    public static ResourceLocation arrowShaftTexture(String idString, String basePathPrefix, String suffix) {
        return textureOf(MaterialResolver.resolveArrowShaftById(idString),
                h -> h.value().texture(), basePathPrefix, suffix, idString);
    }

    public static ResourceLocation arrowFletchingTexture(String idString, String basePathPrefix, String suffix) {
        return textureOf(MaterialResolver.resolveArrowFletchingById(idString),
                h -> h.value().texture(), basePathPrefix, suffix, idString);
    }

    /**
     * Implementation shared by every per-part texture helper.
     */
    private static <T> ResourceLocation textureOf(
            Optional<Holder.Reference<T>> resolved,
            java.util.function.Function<Holder.Reference<T>, Optional<ResourceLocation>> overrideExtractor,
            String basePathPrefix,
            String suffix,
            String fallbackPath) {
        if (resolved.isPresent()) {
            Holder.Reference<T> h = resolved.get();
            Optional<ResourceLocation> override = overrideExtractor.apply(h);
            if (override.isPresent()) {
                return ResourceLocation.fromNamespaceAndPath(
                        override.get().getNamespace(),
                        override.get().getPath() + suffix);
            }
            ResourceLocation key = h.key().location();
            return ResourceLocation.fromNamespaceAndPath(
                    key.getNamespace(),
                    basePathPrefix + "/" + key.getPath() + suffix);
        }
        // Last-ditch: mod namespace + the raw string (normalised). Mirrors
        // the legacy behavior for stored display-form strings the resolver
        // couldn't bridge.
        return ResourceLocation.fromNamespaceAndPath(
                FletcherTrestle.MOD_ID,
                basePathPrefix + "/" + normaliseId(fallbackPath) + suffix);
    }

    /**
     * Normalises a stored id-or-legacy string to its underscore-path form,
     * stripping any namespace. Used where code still compares material
     * identity by string ({@code "crimson".equals(normaliseId(...))}).
     */
    public static String normaliseId(String idOrLegacy) {
        if (idOrLegacy == null || idOrLegacy.isEmpty()) return "";
        int colon = idOrLegacy.indexOf(':');
        String path = colon >= 0 ? idOrLegacy.substring(colon + 1) : idOrLegacy;
        return path.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
