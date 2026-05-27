package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;

/**
 * Tick-time effect: chance to bounce on block impact instead of sticking.
 * Replaces the hardcoded {@code jungle} shaft bounce branch.
 *
 * <p>The actual block-collision wiring is done in Phase E (the {@code jungle}
 * branch lives inside a block-hit handler we'll generalise). For Phase A
 * this just provides the codec + a parameter shape that obviously covers the
 * existing behavior.</p>
 *
 * <p>JSON shape:</p>
 * <pre>
 * { "type": "fletcherstrestle:bounce_on_block",
 *   "chance": 0.85,
 *   "max_bounces": 3 }
 * </pre>
 */
public record BounceOnBlockEffect(float chance, int maxBounces) implements MaterialEffect {

    public static final MapCodec<BounceOnBlockEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(BounceOnBlockEffect::chance),
            Codec.INT.optionalFieldOf("max_bounces", 3).forGetter(BounceOnBlockEffect::maxBounces)
    ).apply(inst, BounceOnBlockEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.BOUNCE_ON_BLOCK.get();
    }

    // Phase E will read these parameters from a block-hit handler in
    // ModularArrowEntity. No-op for Phase A.
    @Override
    public void onArrowTick(ModularArrowEntity arrow) {
        // intentionally empty
    }
}
