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
 * On-hit: if the target is below {@code threshold} fraction of max health, multiply base damage
 * by {@code multiplier}. Used by the crimson shaft's "executioner" trait.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:damage_multiplier_if_target_below_health",
 * "threshold": 0.5, "multiplier": 1.5 }}
 */
public record DamageMultiplierIfTargetBelowHealthEffect(float threshold, float multiplier)
        implements MaterialEffect {

    public static final MapCodec<DamageMultiplierIfTargetBelowHealthEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("threshold").forGetter(DamageMultiplierIfTargetBelowHealthEffect::threshold),
            Codec.FLOAT.fieldOf("multiplier").forGetter(DamageMultiplierIfTargetBelowHealthEffect::multiplier)
    ).apply(inst, DamageMultiplierIfTargetBelowHealthEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DAMAGE_MULTIPLIER_IF_TARGET_BELOW_HEALTH.get();
    }

    @Override
    public void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity target
                && target.getHealth() < target.getMaxHealth() * threshold) {
            arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
        }
    }
}
