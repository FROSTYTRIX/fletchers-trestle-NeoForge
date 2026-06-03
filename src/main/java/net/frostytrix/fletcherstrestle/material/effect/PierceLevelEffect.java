package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;

/**
 * Spawn-time: sets the arrow's pierce level (entities passed through before stopping).
 * Used by the dark_oak shaft.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:pierce_level", "level": 1 }}
 */
public record PierceLevelEffect(int level) implements MaterialEffect {

    public static final MapCodec<PierceLevelEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.fieldOf("level").forGetter(PierceLevelEffect::level)
    ).apply(inst, PierceLevelEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.PIERCE_LEVEL.get();
    }

    @Override
    public void onArrowSpawn(ModularArrowEntity arrow) {
        // AbstractArrow stores pierce as a byte; clamp before casting.
        arrow.setPierceLevel((byte) Math.max(0, Math.min(Byte.MAX_VALUE, level)));
    }
}
