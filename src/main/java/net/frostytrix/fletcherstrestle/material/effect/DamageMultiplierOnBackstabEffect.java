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
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * On-hit: if the arrow comes from behind the target, multiply base damage by {@code multiplier}.
 * "Behind" means the dot of the target's view vector and the arrow's velocity exceeds
 * {@code dotThreshold} (closer to 1.0 = stricter angle). Used by the pale_oak shaft; optional
 * {@code sound} plays on a successful backstab.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:damage_multiplier_on_backstab", "dot_threshold": 0.5,
 * "multiplier": 1.4, "sound": "minecraft:entity.breeze.wind_charge_burst" }}
 */
public record DamageMultiplierOnBackstabEffect(
        float dotThreshold,
        float multiplier,
        Optional<net.minecraft.resources.ResourceLocation> sound)
        implements MaterialEffect {

    public static final MapCodec<DamageMultiplierOnBackstabEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("dot_threshold", 0.5f).forGetter(DamageMultiplierOnBackstabEffect::dotThreshold),
            Codec.FLOAT.fieldOf("multiplier").forGetter(DamageMultiplierOnBackstabEffect::multiplier),
            net.minecraft.resources.ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(DamageMultiplierOnBackstabEffect::sound)
    ).apply(inst, DamageMultiplierOnBackstabEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DAMAGE_MULTIPLIER_ON_BACKSTAB.get();
    }

    @Override
    public void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        Vec3 targetView = target.getViewVector(1.0F);
        Vec3 arrowDir = arrow.getDeltaMovement().normalize();
        if (targetView.dot(arrowDir) > dotThreshold) {
            arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
            sound.ifPresent(rl -> {
                var soundEvent = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(rl);
                if (soundEvent != null) {
                    arrow.level().playSound(null, arrow.getX(), arrow.getY(), arrow.getZ(),
                            net.minecraft.core.Holder.direct(soundEvent),
                            arrow.getSoundSource(), 1.0f, 1.5f);
                }
            });
        }
    }
}
