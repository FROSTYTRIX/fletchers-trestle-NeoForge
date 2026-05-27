package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;

/**
 * On-hit: heals the shooter by {@code amount} half-hearts, optionally
 * spawning {@code particle_count} of {@code particle} at the target's
 * location. Used by the cherry shaft's "petal burst" trait.
 *
 * <p>JSON:</p>
 * <pre>
 * { "type": "fletcherstrestle:heal_shooter",
 *   "amount": 2.0,
 *   "particle": "minecraft:cherry_leaves",
 *   "particle_count": 5 }
 * </pre>
 */
public record HealShooterEffect(
        float amount,
        Optional<ParticleOptions> particle,
        int particleCount) implements MaterialEffect {

    public static final MapCodec<HealShooterEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("amount").forGetter(HealShooterEffect::amount),
            net.minecraft.core.particles.ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(HealShooterEffect::particle),
            Codec.INT.optionalFieldOf("particle_count", 5).forGetter(HealShooterEffect::particleCount)
    ).apply(inst, HealShooterEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.HEAL_SHOOTER.get();
    }

    @Override
    public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return;
        shooter.heal(amount);
        particle.ifPresent(p -> {
            var target = result.getEntity();
            for (int i = 0; i < particleCount; i++) {
                arrow.level().addParticle(p,
                        target.getRandomX(0.5D), target.getRandomY(), target.getRandomZ(0.5D),
                        0, 0, 0);
            }
        });
    }
}
