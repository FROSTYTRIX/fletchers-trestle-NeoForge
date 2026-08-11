package net.frostytrix.fletcherstrestle.compat;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * Isolated Patchouli calls. Every reference to Patchouli lives in this class, so
 * it is only classloaded when Patchouli is actually present. Callers MUST guard
 * with {@code ModList.get().isLoaded("patchouli")} before touching this class —
 * Patchouli is an optional (compileOnly) dependency and will be absent at runtime
 * in packs that don't ship it.
 */
public final class PatchouliCompat {
    private PatchouliCompat() {
    }

    /** The book id — matches assets/fletcherstrestle/patchouli_books/guide/. */
    public static final ResourceLocation GUIDE_BOOK =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "guide");

    /** Opens the guidebook for the player (server-side; sends the open packet). */
    public static void openGuide(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, GUIDE_BOOK);
    }
}
