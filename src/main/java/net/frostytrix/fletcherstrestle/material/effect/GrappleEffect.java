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
 * The arrow hooks into the block it hits and reels its shooter in, until they
 * arrive, drift beyond {@code max_distance}, or {@code max_ticks} pass.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:grapple", "pull": 0.15, "max_ticks": 100, "max_distance": 32.0 }}</p>
 */
public record GrappleEffect(float pull, int maxTicks, float maxDistance) implements MaterialEffect {

    public static final MapCodec<GrappleEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("pull", 0.15f).forGetter(GrappleEffect::pull),
            Codec.INT.optionalFieldOf("max_ticks", 100).forGetter(GrappleEffect::maxTicks),
            Codec.FLOAT.optionalFieldOf("max_distance", 32.0f).forGetter(GrappleEffect::maxDistance)
    ).apply(inst, GrappleEffect::new));

    @Override
    public Optional<Component> describe() {
        return Optional.of(Component.translatable("trait.fletcherstrestle.grapple"));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.GRAPPLE.get();
    }
}
