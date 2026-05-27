package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;

/**
 * Tick-time: on exactly the tick number {@code tick}, multiply the arrow's
 * delta movement by {@code multiplier}. Used by the acacia shaft to give
 * the arrow a brief mid-flight boost (tick=10, x1.4).
 *
 * <p>JSON:</p>
 * <pre>
 * { "type": "fletcherstrestle:set_velocity_multiplier_at_tick",
 *   "tick": 10,
 *   "multiplier": 1.4 }
 * </pre>
 */
public record SetVelocityMultiplierAtTickEffect(int tick, float multiplier) implements MaterialEffect {

    public static final MapCodec<SetVelocityMultiplierAtTickEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.fieldOf("tick").forGetter(SetVelocityMultiplierAtTickEffect::tick),
            Codec.FLOAT.fieldOf("multiplier").forGetter(SetVelocityMultiplierAtTickEffect::multiplier)
    ).apply(inst, SetVelocityMultiplierAtTickEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.SET_VELOCITY_MULTIPLIER_AT_TICK.get();
    }

    @Override
    public void onArrowTick(ModularArrowEntity arrow) {
        if (arrow.tickCount == tick && !arrow.isInGroundPublic()) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(multiplier));
            arrow.hasImpulse = true; // sync the sudden movement to clients
        }
    }
}
