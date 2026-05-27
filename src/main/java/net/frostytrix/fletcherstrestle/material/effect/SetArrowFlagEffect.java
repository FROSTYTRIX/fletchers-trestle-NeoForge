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
 * On-fire: sets a boolean flag on the fired projectile's persistent data
 * map. Used by built-ins for several limb/riser traits the arrow entity
 * inspects later:
 * <ul>
 *   <li>{@code fletcherstrestle:amphibious} (mangrove limb stats already
 *       handle this directly, but the flag pattern is the escape valve
 *       for modpack-added behaviors)</li>
 *   <li>{@code fletcherstrestle:punch} (spruce limb — read in
 *       {@code doKnockback})</li>
 *   <li>{@code fletcherstrestle:conductive} (copper riser — read in
 *       the onHit lightning path)</li>
 * </ul>
 *
 * <p>JSON:</p>
 * <pre>
 * { "type": "fletcherstrestle:set_arrow_flag",
 *   "key": "fletcherstrestle:conductive",
 *   "value": true }
 * </pre>
 */
public record SetArrowFlagEffect(String key, boolean value) implements MaterialEffect {

    public static final MapCodec<SetArrowFlagEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("key").forGetter(SetArrowFlagEffect::key),
            Codec.BOOL.optionalFieldOf("value", true).forGetter(SetArrowFlagEffect::value)
    ).apply(inst, SetArrowFlagEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.SET_ARROW_FLAG.get();
    }

    @Override
    public void onProjectileFired(LivingEntity shooter, ItemStack weapon, Entity projectile) {
        projectile.getPersistentData().putBoolean(key, value);
    }
}
