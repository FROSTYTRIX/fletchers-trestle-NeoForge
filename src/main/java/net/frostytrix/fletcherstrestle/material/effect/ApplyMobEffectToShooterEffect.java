package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * On-release: applies a MobEffect to the shooter (NOT the target). Used
 * by the acacia limb's brief movement-speed buff on release.
 *
 * <p>JSON:</p>
 * <pre>
 * { "type": "fletcherstrestle:apply_effect_to_shooter",
 *   "effect": "minecraft:speed",
 *   "duration": 30,
 *   "amplifier": 1 }
 * </pre>
 */
public record ApplyMobEffectToShooterEffect(
        Holder<MobEffect> effect, int duration, int amplifier) implements MaterialEffect {

    public static final MapCodec<ApplyMobEffectToShooterEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(ApplyMobEffectToShooterEffect::effect),
            Codec.INT.fieldOf("duration").forGetter(ApplyMobEffectToShooterEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(ApplyMobEffectToShooterEffect::amplifier)
    ).apply(inst, ApplyMobEffectToShooterEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.APPLY_EFFECT_TO_SHOOTER.get();
    }

    @Override
    public void onBowRelease(LivingEntity shooter, ItemStack weapon) {
        shooter.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
    }
}
