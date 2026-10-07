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
 * The arrow lodges in the mob it hits and resonates: after {@code delay} ticks it
 * goes off for {@code damage_factor} times its impact damage, ignoring the
 * target's hurt cooldown.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:resonance", "delay": 20, "damage_factor": 0.3 }}</p>
 */
public record ResonanceEffect(int delay, float damageFactor) implements MaterialEffect {

    public static final MapCodec<ResonanceEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.optionalFieldOf("delay", 20).forGetter(ResonanceEffect::delay),
            Codec.FLOAT.optionalFieldOf("damage_factor", 0.3f).forGetter(ResonanceEffect::damageFactor)
    ).apply(inst, ResonanceEffect::new));

    @Override
    public Optional<Component> describe() {
        return Optional.of(Component.translatable("trait.fletcherstrestle.resonance"));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.RESONANCE.get();
    }
}
