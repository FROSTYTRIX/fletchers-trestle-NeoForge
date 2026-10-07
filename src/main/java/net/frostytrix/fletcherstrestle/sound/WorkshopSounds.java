package net.frostytrix.fletcherstrestle.sound;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Bow draw and release sounds, with their pitch rules in one place. Played on the
 * server and broadcast, so other players hear them too.
 */
public final class WorkshopSounds {
    private WorkshopSounds() {
    }

    /** Lightest draw in the base game (birch) and heaviest (dark oak), in ticks. */
    private static final float LIGHT_DRAW = 10f;
    private static final float HEAVY_DRAW = 35f;

    /**
     * The creak of a bow coming to draw. Heavier limbs creak deeper: the pitch is
     * read from the draw time, so a pack's limbs need no sound of their own.
     */
    public static void playDraw(Level level, LivingEntity archer, float drawTicks) {
        float weight = Mth.clamp((drawTicks - LIGHT_DRAW) / (HEAVY_DRAW - LIGHT_DRAW), 0f, 1f);
        float pitch = Mth.lerp(weight, 1.3f, 0.7f) * jitter(level);
        level.playSound(null, archer.getX(), archer.getY(), archer.getZ(),
                ModSounds.BOW_DRAW.get(), SoundSource.PLAYERS, 1.0f, pitch);
    }

    /**
     * The release, voiced by the string. Uses the string's {@code release_sound}
     * when it has one, else the default. {@code power} is the draw fraction, which
     * lifts the pitch slightly on a full draw the way vanilla's release does.
     */
    public static void playRelease(Level level, LivingEntity archer, BowAssembly assembly, float power) {
        Holder<SoundEvent> sound = assembly == null
                ? defaultRelease()
                : Materials.bowString(assembly.stringMaterial()).releaseSound()
                        .map(WorkshopSounds::holder)
                        .orElseGet(WorkshopSounds::defaultRelease);
        float pitch = (0.9f + power * 0.2f) * jitter(level);
        level.playSeededSound(null, archer.getX(), archer.getY(), archer.getZ(),
                sound, SoundSource.PLAYERS, 1.0f, pitch, level.getRandom().nextLong());
    }

    /**
     * A registered sound when the id is one, otherwise a direct holder. The direct
     * case is what lets a pack name a sound that only exists in its resource
     * pack's {@code sounds.json}.
     */
    public static Holder<SoundEvent> holder(ResourceLocation id) {
        return BuiltInRegistries.SOUND_EVENT.getHolder(ResourceKey.create(Registries.SOUND_EVENT, id))
                .<Holder<SoundEvent>>map(h -> h)
                .orElseGet(() -> Holder.direct(SoundEvent.createVariableRangeEvent(id)));
    }

    private static Holder<SoundEvent> defaultRelease() {
        return holder(ModSounds.BOW_RELEASE.get().getLocation());
    }

    /** A small random pitch spread. */
    public static float jitter(Level level) {
        return 0.95f + level.getRandom().nextFloat() * 0.1f;
    }
}
