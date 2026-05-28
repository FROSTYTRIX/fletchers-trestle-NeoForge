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
 * Public registry of named callback handlers invoked by
 * {@link net.frostytrix.fletcherstrestle.material.effect.ScriptedCallbackEffect}.
 * The escape valve for behaviors that don't fit the closed vocabulary
 * shipped under {@code fletcherstrestle:*} effect types.
 *
 * <h3>Why this exists</h3>
 * The closed vocabulary covers everything the built-in materials need
 * and ~95% of what modpack makers want — straight stat scaling, status
 * effects, target-pull, bouncing, ignite, etc. A modpack maker who
 * needs something the vocab can't express (e.g. "spawn a lightning
 * bolt on hit but only at night") can either:
 * <ul>
 *   <li>Ship a tiny companion mod that registers a new
 *       {@link MaterialEffectType} (the heavyweight path), OR</li>
 *   <li>Use this lightweight escape valve: register a Java callback
 *       under a string id, then reference that id from JSON via the
 *       {@code fletcherstrestle:scripted_callback} effect type.</li>
 * </ul>
 *
 * <h3>KubeJS bridge</h3>
 * KubeJS scripts can call {@link #register(ResourceLocation, Handler)}
 * directly from a {@code startup_scripts} file:
 *
 * <pre>{@code
 * Java.loadClass('net.frostytrix.fletcherstrestle.material.ScriptedEffectCallbacks')
 *     .register(
 *         Java.loadClass('net.minecraft.resources.ResourceLocation').parse('mypack:my_hit'),
 *         {
 *             onArrowHit: (arrow, hit) => {
 *                 const target = hit.getEntity();
 *                 if (target) target.setSecondsOnFire(5);
 *             }
 *         }
 *     );
 * }</pre>
 * <p>
 * The corresponding material JSON:
 *
 * <pre>
 * { "type": "fletcherstrestle:scripted_callback", "id": "mypack:my_hit" }
 * </pre>
 *
 * <h3>Lifecycle alignment</h3>
 * The {@link Handler} interface has the same hook surface as
 * {@link MaterialEffect}. Each method defaults to no-op so a handler
 * only implements what it cares about — both from Java
 * ({@code new Handler() { @Override public void onArrowHit(...) }}) and
 * from KubeJS ({@code { onArrowHit: ... }} object-literal).
 *
 * <h3>Lookup miss policy</h3>
 * If a JSON references a callback id that has no registered handler,
 * the effect silently no-ops at runtime. A warning is logged once per
 * unknown id to flag typos. This is intentional — we don't want a
 * missing KubeJS callback to crash the game.
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
     * Empty handler returned for unknown ids — every method no-ops.
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
