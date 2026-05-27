package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * On-hit: yanks the target toward the shooter on impact. Velocity is
 * {@code strength} blocks/tick toward the shooter, with a minimum upward
 * component of {@code minLift} so terrain doesn't snag the pull. Used by
 * the barbed_tip head.
 *
 * <p>JSON:</p>
 * <pre>
 * { "type": "fletcherstrestle:pull_target_to_shooter",
 *   "strength": 0.75,
 *   "min_lift": 0.25 }
 * </pre>
 */
public record PullTargetToShooterEffect(float strength, float minLift) implements MaterialEffect {

    public static final MapCodec<PullTargetToShooterEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("strength", 0.75f).forGetter(PullTargetToShooterEffect::strength),
            Codec.FLOAT.optionalFieldOf("min_lift", 0.25f).forGetter(PullTargetToShooterEffect::minLift)
    ).apply(inst, PullTargetToShooterEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.PULL_TARGET_TO_SHOOTER.get();
    }

    @Override
    public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        if (arrow.getOwner() == null) return;
        Vec3 toShooter = arrow.getOwner().position()
                .subtract(target.position())
                .normalize()
                .scale(strength);
        target.setDeltaMovement(toShooter.x, Math.max(minLift, toShooter.y), toShooter.z);
        target.hurtMarked = true;
        arrow.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0f, 1.6f);
    }
}
