package net.frostytrix.fletcherstrestle.sound;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Custom sound events for the mod. Most play recordings cut by tools/sounds/build_sounds.py
// (public-domain and CC0 sources, listed there); a few still borrow vanilla sounds. Which file,
// how loud and at what pitch all live in sounds.json, so they can be swapped without code.
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

    // The workshop and the bow.
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

    // The Bolt Warden's clockwork.
    public static final Supplier<SoundEvent> GARRISON_TICK = register("garrison_golem.tick");
    public static final Supplier<SoundEvent> GARRISON_WIND_UP = register("garrison_golem.wind_up");
    public static final Supplier<SoundEvent> GARRISON_CRANK = register("garrison_golem.crank");
    public static final Supplier<SoundEvent> GARRISON_EMPTY = register("garrison_golem.empty");
    public static final Supplier<SoundEvent> GARRISON_REPAIR = register("garrison_golem.repair");
    public static final Supplier<SoundEvent> GARRISON_CRACK = register("garrison_golem.crack");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
