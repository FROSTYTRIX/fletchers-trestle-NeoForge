package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * On-fire: sets the fired projectile alight for {@code seconds} seconds. Used by the crimson limb.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:ignite_arrow", "seconds": 100 }}
 */
public record IgniteArrowEffect(int seconds) implements MaterialEffect {

    public static final MapCodec<IgniteArrowEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.fieldOf("seconds").forGetter(IgniteArrowEffect::seconds)
    ).apply(inst, IgniteArrowEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.IGNITE_ARROW.get();
    }

    @Override
    public void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
        projectile.igniteForSeconds(seconds);
    }
}
