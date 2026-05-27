package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tick-time: pulls the arrow toward the nearest non-owner living entity
 * within {@code range} blocks, with a velocity-vector add of magnitude
 * {@code strength}. Skips the first {@code grace_ticks} ticks so the
 * arrow can establish its initial trajectory. Used by the serrated
 * fletching's "magnetism" trait.
 *
 * <p>JSON:</p>
 * <pre>
 * { "type": "fletcherstrestle:subtle_homing",
 *   "range": 5.0,
 *   "strength": 1.0,
 *   "grace_ticks": 2 }
 * </pre>
 */
public record SubtleHomingEffect(float range, float strength, int graceTicks) implements MaterialEffect {

    public static final MapCodec<SubtleHomingEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("range", 5.0f).forGetter(SubtleHomingEffect::range),
            Codec.FLOAT.optionalFieldOf("strength", 1.0f).forGetter(SubtleHomingEffect::strength),
            Codec.INT.optionalFieldOf("grace_ticks", 2).forGetter(SubtleHomingEffect::graceTicks)
    ).apply(inst, SubtleHomingEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.SUBTLE_HOMING.get();
    }

    @Override
    public void onArrowTick(ModularArrowEntity arrow) {
        if (arrow.isInGroundPublic() || arrow.tickCount <= graceTicks) return;
        AABB searchBox = arrow.getBoundingBox().inflate(range);
        List<LivingEntity> entities = arrow.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != arrow.getOwner() && e.isAlive());
        if (entities.isEmpty()) return;
        LivingEntity target = entities.get(0); // closest by AABB order
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
        Vec3 pull = targetCenter.subtract(arrow.position()).normalize().scale(strength);
        arrow.setDeltaMovement(arrow.getDeltaMovement().add(pull));
    }
}
