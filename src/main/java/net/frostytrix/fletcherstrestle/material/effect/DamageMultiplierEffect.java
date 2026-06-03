package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;

/**
 * Spawn-time: scales the arrow's base damage by a constant. The part stats records already
 * carry a {@code damage_multiplier} field for the common case; this effect lets a material
 * stack an extra multiplier on top.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:damage_multiplier", "multiplier": 1.25 }}
 */
public record DamageMultiplierEffect(float multiplier) implements MaterialEffect {

    public static final MapCodec<DamageMultiplierEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("multiplier").forGetter(DamageMultiplierEffect::multiplier)
    ).apply(inst, DamageMultiplierEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DAMAGE_MULTIPLIER.get();
    }

    @Override
    public void onArrowSpawn(ModularArrowEntity arrow) {
        arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
    }
}
