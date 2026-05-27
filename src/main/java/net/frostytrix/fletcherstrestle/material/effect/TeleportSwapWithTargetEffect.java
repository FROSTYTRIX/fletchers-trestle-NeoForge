package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * On-hit: with {@code chance} probability, swaps the shooter and target's
 * positions. Used by the warped shaft.
 *
 * <p>JSON:</p>
 * <pre>{ "type": "fletcherstrestle:teleport_swap_with_target", "chance": 1.0 }</pre>
 */
public record TeleportSwapWithTargetEffect(float chance) implements MaterialEffect {

    public static final MapCodec<TeleportSwapWithTargetEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(TeleportSwapWithTargetEffect::chance)
    ).apply(inst, TeleportSwapWithTargetEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.TELEPORT_SWAP_WITH_TARGET.get();
    }

    @Override
    public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (arrow.level().random.nextFloat() >= chance) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        Entity shooter = arrow.getOwner();
        if (shooter == null || !shooter.isAlive()) return;
        Vec3 sPos = shooter.position();
        Vec3 tPos = target.position();
        shooter.teleportTo(tPos.x, tPos.y, tPos.z);
        target.teleportTo(sPos.x, sPos.y, sPos.z);
        arrow.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
    }
}
