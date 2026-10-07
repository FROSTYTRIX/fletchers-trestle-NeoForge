package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The head carries a potion: it can be dipped in the Dipping Vat, and it shatters
 * on impact, splashing the potion over everything within {@code radius} blocks,
 * weaker toward the edge. The item model draws a {@code <head texture>_liquid}
 * layer, tinted to the potion, once it's been dipped.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:splash_potion", "radius": 4.0 }}</p>
 */
public record SplashPotionEffect(float radius) implements MaterialEffect {

    public static final MapCodec<SplashPotionEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("radius", 4.0f).forGetter(SplashPotionEffect::radius)
    ).apply(inst, SplashPotionEffect::new));

    @Override
    public Optional<Component> describe() {
        return Optional.of(Component.translatable("trait.fletcherstrestle.splash_potion"));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.SPLASH_POTION.get();
    }
}
