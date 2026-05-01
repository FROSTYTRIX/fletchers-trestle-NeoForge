package net.frostytrix.fletcherstrestle.effect;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, FletcherTrestle.MOD_ID);

    public static final Holder<MobEffect> BLEED_EFFECT = MOB_EFFECTS.register("bleed",
            () -> new BleedEffect(MobEffectCategory.HARMFUL, 0x980002));

    public static void register(IEventBus bus)
    {MOB_EFFECTS.register(bus);}
}
