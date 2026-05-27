package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;

/**
 * Spawn-time effect: scales the arrow's base damage by a constant.
 *
 * <p>Note: head / shaft / fletching stats records *already* carry a
 * {@code damage_multiplier} field for the common case. This effect type is
 * the escape valve for cases where a material wants to stack an additional
 * multiplier with the base stat (e.g. a future "Enchanted" string that
 * boosts whatever head it's paired with).</p>
 *
 * <p>JSON shape:</p>
 * <pre>{ "type": "fletcherstrestle:damage_multiplier", "multiplier": 1.25 }</pre>
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
