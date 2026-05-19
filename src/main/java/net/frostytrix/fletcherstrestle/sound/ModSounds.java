package net.frostytrix.fletcherstrestle.sound;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Custom sound events for the eagle. The audio files referenced from
// sounds.json are vanilla placeholders for now — when you record your own
// .ogg files you only have to update sounds.json (no code changes).
public final class ModSounds {

    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, FletcherTrestle.MOD_ID);

    public static final Supplier<SoundEvent> EAGLE_AMBIENT = register("eagle.ambient");
    public static final Supplier<SoundEvent> EAGLE_HURT    = register("eagle.hurt");
    public static final Supplier<SoundEvent> EAGLE_DEATH   = register("eagle.death");
    public static final Supplier<SoundEvent> EAGLE_TAME    = register("eagle.tame");
    public static final Supplier<SoundEvent> EAGLE_FLAP    = register("eagle.flap");
    public static final Supplier<SoundEvent> EAGLE_DIVE    = register("eagle.dive");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
