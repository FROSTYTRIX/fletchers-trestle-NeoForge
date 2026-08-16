package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.world.phys.EntityHitResult;

/**
 * On-hit: scales base damage by {@code 1 + (distanceTraveled / perBlock)}. Used by the
 * weighted_blunt head: every {@code perBlock} blocks of travel adds 1× the base damage.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:damage_multiplier_by_distance", "per_block": 100 }}
 */
public record DamageMultiplierByDistanceEffect(float perBlock) implements MaterialEffect {

    public static final MapCodec<DamageMultiplierByDistanceEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("per_block").forGetter(DamageMultiplierByDistanceEffect::perBlock)
    ).apply(inst, DamageMultiplierByDistanceEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DAMAGE_MULTIPLIER_BY_DISTANCE.get();
    }

    @Override
    public void onPreArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        var start = arrow.getStartPos();
        if (start == null) return;
        double distance = arrow.position().distanceTo(start);
        double mult = 1.0 + (distance / perBlock);
        arrow.setBaseDamage(arrow.getBaseDamage() * mult);
    }
}
