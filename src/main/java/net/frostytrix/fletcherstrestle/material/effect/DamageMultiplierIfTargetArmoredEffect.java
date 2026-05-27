package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * On-hit: if the target has any armor at all, multiply base damage by
 * {@code multiplier}. Bodkin-point's "armor piercing" trait flattens out
 * to this in practice — the legacy code did a more elaborate armor-scaled
 * bonus but the result was effectively a flat 1.25× when armor > 0.
 *
 * <p>JSON:</p>
 * <pre>{ "type": "fletcherstrestle:damage_multiplier_if_target_armored", "multiplier": 1.25 }</pre>
 */
public record DamageMultiplierIfTargetArmoredEffect(float multiplier) implements MaterialEffect {

    public static final MapCodec<DamageMultiplierIfTargetArmoredEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("multiplier").forGetter(DamageMultiplierIfTargetArmoredEffect::multiplier)
    ).apply(inst, DamageMultiplierIfTargetArmoredEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DAMAGE_MULTIPLIER_IF_TARGET_ARMORED.get();
    }

    @Override
    public void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity target && target.getArmorValue() > 0) {
            arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
        }
    }
}
