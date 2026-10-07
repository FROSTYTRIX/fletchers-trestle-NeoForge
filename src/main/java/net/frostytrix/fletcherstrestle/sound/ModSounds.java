package net.frostytrix.fletcherstrestle.sound;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Custom sound events for the mod. The files referenced from sounds.json are vanilla
// placeholders for now: swapping in real .ogg files only needs a sounds.json change, no code.
public final class ModSounds {

    private ModSounds() {
    }

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, FletcherTrestle.MOD_ID);

    public static final Supplier<SoundEvent> EAGLE_AMBIENT = register("eagle.ambient");
    public static final Supplier<SoundEvent> EAGLE_HURT = register("eagle.hurt");
    public static final Supplier<SoundEvent> EAGLE_DEATH = register("eagle.death");
    public static final Supplier<SoundEvent> EAGLE_TAME = register("eagle.tame");
    public static final Supplier<SoundEvent> EAGLE_FLAP = register("eagle.flap");
    public static final Supplier<SoundEvent> EAGLE_DIVE = register("eagle.dive");

    // The workshop and the bow. Like the eagle, these point at vanilla files for now
    // (see sounds.json): pitch and volume live there, so they can be tuned or swapped
    // for real recordings without touching code.
    public static final Supplier<SoundEvent> BOW_DRAW = register("bow.draw");
    public static final Supplier<SoundEvent> BOW_READY = register("bow.ready");
    public static final Supplier<SoundEvent> BOW_RELEASE = register("bow.release");
    public static final Supplier<SoundEvent> STRING_FLAX = register("bow.release.flax");
    public static final Supplier<SoundEvent> STRING_SPIDER = register("bow.release.spider");
    public static final Supplier<SoundEvent> STRING_HIGH_TENSION = register("bow.release.high_tension");
    public static final Supplier<SoundEvent> STEAM_BOX_HISS = register("steam_box.hiss");
    public static final Supplier<SoundEvent> STEAM_BOX_DONE = register("steam_box.done");
    public static final Supplier<SoundEvent> SHAVING_HORSE_SHAVE = register("shaving_horse.shave");
    public static final Supplier<SoundEvent> TARGET_HIT = register("archery_target.hit");
    public static final Supplier<SoundEvent> TARGET_BULLSEYE = register("archery_target.bullseye");
    public static final Supplier<SoundEvent> HEADSHOT = register("headshot");
    public static final Supplier<SoundEvent> FLETCHING_PLUCK = register("fletching.pluck");
    public static final Supplier<SoundEvent> CAPSTONE = register("capstone");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
