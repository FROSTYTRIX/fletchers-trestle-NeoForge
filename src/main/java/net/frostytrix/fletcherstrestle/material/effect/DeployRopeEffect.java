package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Shot into the underside of a block, the arrow anchors there and lets down a
 * climbable rope, up to {@code max_length} blocks or until it reaches the floor.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:deploy_rope", "max_length": 20 }}</p>
 */
public record DeployRopeEffect(int maxLength) implements MaterialEffect {

    public static final MapCodec<DeployRopeEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.optionalFieldOf("max_length", 20).forGetter(DeployRopeEffect::maxLength)
    ).apply(inst, DeployRopeEffect::new));

    @Override
    public Optional<Component> describe() {
        return Optional.of(Component.translatable("trait.fletcherstrestle.rope"));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.DEPLOY_ROPE.get();
    }
}
