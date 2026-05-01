package net.frostytrix.fletcherstrestle.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;

public class BleedEffect extends MobEffect {
    public BleedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Deal 1.0f damage (half a heart) every time the tick hits.
        // The frequency is controlled by shouldApplyEffectTick below.
        entity.hurt(entity.damageSources().generic(), 1.0f + (float)amplifier);

        // Spawn "Blood" particles (Redstone dust particles are the best vanilla substitute)
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    3, 0.2, 0.2, 0.2, 0.05);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 40 >> amplifier;
        if (interval > 0) {
            return duration % interval == 0;
        } else {
            return true;
        }
    }
}