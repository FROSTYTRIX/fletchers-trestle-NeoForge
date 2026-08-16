package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of named callback handlers invoked by {@code ScriptedCallbackEffect}: the lightweight
 * escape valve for behaviors the closed {@code fletcherstrestle:*} effect vocabulary can't express.
 * Register a Java/KubeJS handler under a string id, then reference that id from material JSON. The
 * {@link Handler} interface mirrors {@link MaterialEffect}'s hooks, each defaulting to no-op.
 * Unknown ids no-op and log one warning so a typo never crashes the game.
 *
 * <p>KubeJS ({@code startup_scripts}):
 * <pre>{@code
 * Java.loadClass('net.frostytrix.fletcherstrestle.material.ScriptedEffectCallbacks')
 *     .register(
 *         Java.loadClass('net.minecraft.resources.ResourceLocation').parse('mypack:my_hit'),
 *         { onArrowHit: (arrow, hit) => { const t = hit.getEntity(); if (t) t.setSecondsOnFire(5); } }
 *     );
 * }</pre>
 * with material JSON {@code { "type": "fletcherstrestle:scripted_callback", "id": "mypack:my_hit" }}.
 */
public final class ScriptedEffectCallbacks {
    private ScriptedEffectCallbacks() {
    }

    /**
     * Lifecycle-handler interface for scripted callbacks. Mirror of
     * {@link MaterialEffect}'s hook surface; every method defaults
     * no-op so a handler only overrides what it needs.
     */
    public interface Handler {
        default void onArrowSpawn(ModularArrowEntity arrow) {
        }

        default void onArrowTick(ModularArrowEntity arrow) {
        }

        default void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        }

        default void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        }

        default void onArrowHitBlock(ModularArrowEntity arrow, BlockHitResult result) {
        }

        default void onBowRelease(LivingEntity shooter, ItemStack weapon) {
        }

        default void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
        }
    }

    /**
     * Empty handler returned for unknown ids: every method no-ops.
     */
    public static final Handler NOOP = new Handler() {
    };

    /**
     * Concurrent because registration may race with script-engine init.
     */
    private static final Map<ResourceLocation, Handler> HANDLERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Boolean> WARNED = new ConcurrentHashMap<>();

    /**
     * Register a callback handler for a string id. Subsequent lookups
     * for that id return this handler. Overwriting a previous
     * registration is allowed (a script hot-reload commonly does this).
     */
    public static void register(ResourceLocation id, Handler handler) {
        HANDLERS.put(id, handler);
    }

    /**
     * Looks up the handler for an id, or returns {@link #NOOP}.
     */
    public static Handler get(ResourceLocation id) {
        Handler h = HANDLERS.get(id);
        if (h != null) return h;
        if (WARNED.putIfAbsent(id, true) == null) {
            net.frostytrix.fletcherstrestle.FletcherTrestle.LOGGER.warn(
                    "Unknown scripted_callback id '{}' referenced by a material def. " +
                            "Register the handler via ScriptedEffectCallbacks.register() or fix the JSON.",
                    id);
        }
        return NOOP;
    }
}
