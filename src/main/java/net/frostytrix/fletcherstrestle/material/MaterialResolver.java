package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Lookup service for material defs. Two flavors per part:
 * <ul>
 *   <li>{@code resolveX(stack)} — given a player-supplied ItemStack, find
 *       the def whose {@link Ingredient} accepts it. Used by the recipe
 *       matchers in the fletching menu.</li>
 *   <li>{@code resolveXById(idOrLegacy)} — given the String stored on an
 *       {@code ArrowAssembly} / {@code BowAssembly} component, find the
 *       def. Tolerates both modern id form ({@code "dark_oak"},
 *       {@code "mypack:steel"}) and legacy display form
 *       ({@code "Dark Oak"}) so worlds saved before Phase D keep working.</li>
 * </ul>
 *
 * <p>Most callers don't have a {@link RegistryAccess} handy, so each public
 * method has an overload that calls {@link #pickAccess()} to find one via
 * the running server (logical server) or {@link Minecraft#getConnection()}
 * (logical client). Callers that DO have an access (recipes during
 * matching, the arrow entity post-spawn, anything in a {@code Level}-aware
 * code path) should pass it explicitly — it's a strict superset.</p>
 *
 * <p>Walks the registry linearly on each ingredient-based lookup. The
 * registries are tiny (3–11 entries) so this is negligible; a cache can
 * land in a later phase if profile shows it's worth the lifecycle wiring.</p>
 */
public final class MaterialResolver {
    private MaterialResolver() {}

    // ---- ItemStack → def ----

    public static Optional<Holder.Reference<BowLimbDef>> resolveBowLimb(HolderLookup.Provider lookups, ItemStack stack) {
        return byIngredient(lookups, ModMaterialRegistries.BOW_LIMB, stack, BowLimbDef::ingredient);
    }
    public static Optional<Holder.Reference<BowLimbDef>> resolveBowLimb(ItemStack stack) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveBowLimb(a, stack);
    }

    public static Optional<Holder.Reference<BowRiserDef>> resolveBowRiser(HolderLookup.Provider lookups, ItemStack stack) {
        return byIngredient(lookups, ModMaterialRegistries.BOW_RISER, stack, BowRiserDef::ingredient);
    }
    public static Optional<Holder.Reference<BowRiserDef>> resolveBowRiser(ItemStack stack) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveBowRiser(a, stack);
    }

    public static Optional<Holder.Reference<BowStringDef>> resolveBowString(HolderLookup.Provider lookups, ItemStack stack) {
        return byIngredient(lookups, ModMaterialRegistries.BOW_STRING, stack, BowStringDef::ingredient);
    }
    public static Optional<Holder.Reference<BowStringDef>> resolveBowString(ItemStack stack) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveBowString(a, stack);
    }

    public static Optional<Holder.Reference<ArrowHeadDef>> resolveArrowHead(HolderLookup.Provider lookups, ItemStack stack) {
        return byIngredient(lookups, ModMaterialRegistries.ARROW_HEAD, stack, ArrowHeadDef::ingredient);
    }
    public static Optional<Holder.Reference<ArrowHeadDef>> resolveArrowHead(ItemStack stack) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveArrowHead(a, stack);
    }

    public static Optional<Holder.Reference<ArrowShaftDef>> resolveArrowShaft(HolderLookup.Provider lookups, ItemStack stack) {
        return byIngredient(lookups, ModMaterialRegistries.ARROW_SHAFT, stack, ArrowShaftDef::ingredient);
    }
    public static Optional<Holder.Reference<ArrowShaftDef>> resolveArrowShaft(ItemStack stack) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveArrowShaft(a, stack);
    }

    public static Optional<Holder.Reference<ArrowFletchingDef>> resolveArrowFletching(HolderLookup.Provider lookups, ItemStack stack) {
        return byIngredient(lookups, ModMaterialRegistries.ARROW_FLETCHING, stack, ArrowFletchingDef::ingredient);
    }
    public static Optional<Holder.Reference<ArrowFletchingDef>> resolveArrowFletching(ItemStack stack) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveArrowFletching(a, stack);
    }

    // ---- Component-string → def ----

    public static Optional<Holder.Reference<BowLimbDef>> resolveBowLimbById(HolderLookup.Provider lookups, String idOrLegacy) {
        return byId(lookups, ModMaterialRegistries.BOW_LIMB, idOrLegacy);
    }
    public static Optional<Holder.Reference<BowLimbDef>> resolveBowLimbById(String idOrLegacy) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveBowLimbById(a, idOrLegacy);
    }

    public static Optional<Holder.Reference<BowRiserDef>> resolveBowRiserById(HolderLookup.Provider lookups, String idOrLegacy) {
        return byId(lookups, ModMaterialRegistries.BOW_RISER, idOrLegacy);
    }
    public static Optional<Holder.Reference<BowRiserDef>> resolveBowRiserById(String idOrLegacy) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveBowRiserById(a, idOrLegacy);
    }

    public static Optional<Holder.Reference<BowStringDef>> resolveBowStringById(HolderLookup.Provider lookups, String idOrLegacy) {
        return byId(lookups, ModMaterialRegistries.BOW_STRING, idOrLegacy);
    }
    public static Optional<Holder.Reference<BowStringDef>> resolveBowStringById(String idOrLegacy) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveBowStringById(a, idOrLegacy);
    }

    public static Optional<Holder.Reference<ArrowHeadDef>> resolveArrowHeadById(HolderLookup.Provider lookups, String idOrLegacy) {
        return byId(lookups, ModMaterialRegistries.ARROW_HEAD, idOrLegacy);
    }
    public static Optional<Holder.Reference<ArrowHeadDef>> resolveArrowHeadById(String idOrLegacy) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveArrowHeadById(a, idOrLegacy);
    }

    public static Optional<Holder.Reference<ArrowShaftDef>> resolveArrowShaftById(HolderLookup.Provider lookups, String idOrLegacy) {
        return byId(lookups, ModMaterialRegistries.ARROW_SHAFT, idOrLegacy);
    }
    public static Optional<Holder.Reference<ArrowShaftDef>> resolveArrowShaftById(String idOrLegacy) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveArrowShaftById(a, idOrLegacy);
    }

    public static Optional<Holder.Reference<ArrowFletchingDef>> resolveArrowFletchingById(HolderLookup.Provider lookups, String idOrLegacy) {
        return byId(lookups, ModMaterialRegistries.ARROW_FLETCHING, idOrLegacy);
    }
    public static Optional<Holder.Reference<ArrowFletchingDef>> resolveArrowFletchingById(String idOrLegacy) {
        HolderLookup.Provider a = pickAccess();
        return a == null ? Optional.empty() : resolveArrowFletchingById(a, idOrLegacy);
    }

    // ---- Display ----

    /**
     * Translation key for a material's display name. Built-ins ship the
     * {@code material.fletcherstrestle.<path>} keys in en_us; modpack
     * makers ship matching keys for their own namespace.
     */
    public static String displayKey(ResourceLocation entryId) {
        return "material." + entryId.getNamespace() + "." + entryId.getPath();
    }

    /** {@link #displayKey(ResourceLocation)} wrapped in a {@link Component}. */
    public static Component displayName(ResourceLocation entryId) {
        return Component.translatable(displayKey(entryId));
    }

    // ---- Internals ----

    private static <T> Optional<Holder.Reference<T>> byIngredient(
            HolderLookup.Provider lookups,
            ResourceKey<Registry<T>> registryKey,
            ItemStack stack,
            Function<T, Ingredient> ingFn) {
        if (stack.isEmpty()) return Optional.empty();
        return lookups.lookup(registryKey)
                .flatMap(lookup -> lookup.listElements()
                        .filter(h -> ingFn.apply(h.value()).test(stack))
                        .findFirst());
    }

    /**
     * Looks up by stored id. Three-step fallback:
     * <ol>
     *   <li>{@code "mypack:steel"} → parse as {@link ResourceLocation}</li>
     *   <li>{@code "dark_oak"} → assume default namespace
     *       {@code fletcherstrestle}</li>
     *   <li>{@code "Dark Oak"} → legacy display form, lowercase + space-to-underscore,
     *       retry as default-namespace lookup. Lets worlds saved before
     *       the storage format change keep working.</li>
     * </ol>
     */
    private static <T> Optional<Holder.Reference<T>> byId(
            HolderLookup.Provider lookups,
            ResourceKey<Registry<T>> registryKey,
            String idOrLegacy) {
        if (idOrLegacy == null || idOrLegacy.isEmpty()) return Optional.empty();
        Optional<HolderLookup.RegistryLookup<T>> maybeLookup = lookups.lookup(registryKey);
        if (maybeLookup.isEmpty()) return Optional.empty();
        HolderLookup.RegistryLookup<T> lookup = maybeLookup.get();

        // Every ResourceLocation build below goes through tryBuild / tryParse,
        // which return null on invalid characters instead of throwing. This
        // matters: BowAssemblies saved before Phase D store legacy display
        // form ("Wood", "Dark Oak", "High Tension") which contain capitals
        // and spaces — both rejected by ResourceLocation's strict path check.
        // The legacy-form fallback at step (3) is what handles those, but it
        // can only run if steps (1) and (2) don't throw first.

        // (1) explicit namespace e.g. "mypack:steel"
        if (idOrLegacy.indexOf(':') >= 0) {
            ResourceLocation rl = ResourceLocation.tryParse(idOrLegacy);
            if (rl != null) {
                Optional<Holder.Reference<T>> hit = lookup.get(ResourceKey.create(registryKey, rl));
                if (hit.isPresent()) return hit;
            }
        }

        // (2) bare path, default namespace — only attempted if the path
        //     passes ResourceLocation's character validation.
        ResourceLocation withNs = ResourceLocation.tryBuild(FletcherTrestle.MOD_ID, idOrLegacy);
        if (withNs != null) {
            Optional<Holder.Reference<T>> direct = lookup.get(ResourceKey.create(registryKey, withNs));
            if (direct.isPresent()) return direct;
        }

        // (3) legacy display form: lowercase + space-to-underscore.
        String normalised = idOrLegacy.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!normalised.equals(idOrLegacy) && !normalised.isEmpty()) {
            ResourceLocation rl2 = ResourceLocation.tryBuild(FletcherTrestle.MOD_ID, normalised);
            if (rl2 != null) {
                return lookup.get(ResourceKey.create(registryKey, rl2));
            }
        }
        return Optional.empty();
    }

    /**
     * Best-effort {@link RegistryAccess} lookup for callers that don't have
     * a {@link net.minecraft.world.level.Level} handy. Tries the running
     * dedicated/integrated server first; falls back to the client connection.
     * Returns null during datagen, in early init, or before the client has
     * joined a world.
     */
    private static @Nullable HolderLookup.Provider pickAccess() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return clientAccess();
        }
        return null;
    }

    /**
     * Pulled out so the client-only class load happens only when the
     * dist is client. Calling {@link Minecraft#getInstance()} on a
     * dedicated server would NoClassDefFoundError.
     */
    private static @Nullable HolderLookup.Provider clientAccess() {
        var conn = Minecraft.getInstance().getConnection();
        return conn == null ? null : conn.registryAccess();
    }
}
