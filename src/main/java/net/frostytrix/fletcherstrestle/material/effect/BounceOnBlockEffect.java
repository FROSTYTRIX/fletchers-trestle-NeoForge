package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Block-hit: deflects off the impacted face instead of embedding, with {@code chance}
 * probability and up to {@code maxBounces} times. Used by the jungle shaft. The bounce
 * inverts the velocity axis facing the surface and scales all axes by {@code retention}.
 * Signals "consumed" by incrementing the arrow's bounce count, which
 * {@link ModularArrowEntity#getBounceCount()} checks to skip the vanilla embed path.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:bounce_on_block", "chance": 0.85,
 * "max_bounces": 3, "retention": 0.3 }}
 */
public record BounceOnBlockEffect(float chance, int maxBounces, float retention) implements MaterialEffect {

    public static final MapCodec<BounceOnBlockEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(BounceOnBlockEffect::chance),
            Codec.INT.optionalFieldOf("max_bounces", 3).forGetter(BounceOnBlockEffect::maxBounces),
            Codec.FLOAT.optionalFieldOf("retention", 0.3f).forGetter(BounceOnBlockEffect::retention)
    ).apply(inst, BounceOnBlockEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.BOUNCE_ON_BLOCK.get();
    }

    @Override
    public void onArrowHitBlock(ModularArrowEntity arrow, BlockHitResult result) {
        if (arrow.getBounceCount() >= maxBounces) return;
        if (arrow.level().random.nextFloat() >= chance) return;

        // Snap to the surface so the arrow doesn't embed and slingshot.
        Vec3 hitPos = result.getLocation();
        arrow.setPos(hitPos.x, hitPos.y, hitPos.z);

        Direction face = result.getDirection();
        Vec3 motion = arrow.getDeltaMovement();
        double bounceX = motion.x * retention;
        double bounceY = motion.y * retention;
        double bounceZ = motion.z * retention;
        if (face.getAxis() == Direction.Axis.X) bounceX = -bounceX;
        if (face.getAxis() == Direction.Axis.Y) bounceY = -bounceY;
        if (face.getAxis() == Direction.Axis.Z) bounceZ = -bounceZ;

        Vec3 newMovement = new Vec3(bounceX, bounceY, bounceZ);
        arrow.setDeltaMovement(newMovement);

        // Recompute rotation off the new velocity vector.
        double d0 = newMovement.horizontalDistance();
        arrow.setYRot((float) (Math.atan2(newMovement.x, newMovement.z) * (180F / (float) Math.PI)));
        arrow.setXRot((float) (Math.atan2(newMovement.y, d0) * (180F / (float) Math.PI)));
        arrow.yRotO = arrow.getYRot();
        arrow.xRotO = arrow.getXRot();
        arrow.setCritArrow(false);

        arrow.hasImpulse = true;
        arrow.incrementBounceCount();
        arrow.playSound(SoundEvents.SLIME_BLOCK_FALL, 1.0F,
                1.2F / (arrow.level().random.nextFloat() * 0.2F + 0.9F));
    }
}
