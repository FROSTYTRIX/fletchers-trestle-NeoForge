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
 * On-hit: if the target has any armor, multiply base damage by {@code multiplier}. Backs
 * bodkin-point's "armor piercing" trait.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:damage_multiplier_if_target_armored", "multiplier": 1.25 }}
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
