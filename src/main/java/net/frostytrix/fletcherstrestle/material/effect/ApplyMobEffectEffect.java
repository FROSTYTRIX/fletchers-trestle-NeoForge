package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * On-hit: applies a MobEffect to the entity hit. Used by broadhead (bleed) and mangrove shaft (slowness).
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:apply_effect", "effect": "minecraft:slowness",
 * "duration": 80, "amplifier": 2 }}
 */
public record ApplyMobEffectEffect(Holder<MobEffect> effect, int duration, int amplifier)
        implements MaterialEffect {

    public static final MapCodec<ApplyMobEffectEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(ApplyMobEffectEffect::effect),
            Codec.INT.fieldOf("duration").forGetter(ApplyMobEffectEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(ApplyMobEffectEffect::amplifier)
    ).apply(inst, ApplyMobEffectEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.APPLY_EFFECT.get();
    }

    @Override
    public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity target) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }
}
