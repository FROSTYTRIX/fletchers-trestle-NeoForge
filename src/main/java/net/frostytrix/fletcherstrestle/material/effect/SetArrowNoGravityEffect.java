package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * On-fire: removes gravity from the fired projectile so it flies in a
 * straight line. Used by the warped bow / crossbow limb.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:set_arrow_no_gravity" }}</p>
 */
public record SetArrowNoGravityEffect() implements MaterialEffect {

    public static final MapCodec<SetArrowNoGravityEffect> CODEC = MapCodec.unit(SetArrowNoGravityEffect::new);

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.SET_ARROW_NO_GRAVITY.get();
    }

    @Override
    public void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
        projectile.setNoGravity(true);
    }
}
