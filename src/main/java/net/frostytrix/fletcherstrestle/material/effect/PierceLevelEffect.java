package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;

/**
 * Spawn-time effect: sets the arrow's pierce level (how many entities it
 * passes through before stopping). Replaces the hard-coded {@code dark_oak}
 * shaft branch.
 *
 * <p>JSON shape:</p>
 * <pre>{ "type": "fletcherstrestle:pierce_level", "level": 1 }</pre>
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
        // Vanilla AbstractArrow caps pierce at Byte.MAX_VALUE; the cast is
        // safe for any sane material declaration.
        arrow.setPierceLevel((byte) Math.max(0, Math.min(Byte.MAX_VALUE, level)));
    }
}
