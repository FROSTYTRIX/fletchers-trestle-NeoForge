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
 * On-hit, with {@code chance} probability, drops the arrow item at the
 * impact site and discards the entity. Used by the bound fletching's
 * "drop-on-hit" trait: lets players recover the arrow for ~1/4 hits.
 *
 * <p>JSON:</p>
 * <pre>{ "type": "fletcherstrestle:drop_self_on_hit", "chance": 0.25 }</pre>
 */
public record DropSelfOnHitEffect(float chance) implements MaterialEffect {

    public static final MapCodec<DropSelfOnHitEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.FLOAT.fieldOf("chance").forGetter(DropSelfOnHitEffect::chance)
    ).apply(inst, DropSelfOnHitEffect::new));

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DROP_SELF_ON_HIT.get();
    }

    @Override
    public void onArrowHit(ModularArrowEntity arrow, EntityHitResult result) {
        if (arrow.level().random.nextFloat() < chance) {
            arrow.spawnAtLocation(arrow.getPickupItemPublic());
            arrow.discard();
        }
    }
}
