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
 * The arrow slips through the first {@code blocks} blocks it hits instead of
 * sticking in them.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:phase_through_blocks", "blocks": 1 }}</p>
 */
public record PhaseThroughBlocksEffect(int blocks) implements MaterialEffect {

    public static final MapCodec<PhaseThroughBlocksEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.optionalFieldOf("blocks", 1).forGetter(PhaseThroughBlocksEffect::blocks)
    ).apply(inst, PhaseThroughBlocksEffect::new));

    @Override
    public Optional<Component> describe() {
        return Optional.of(Component.translatable("trait.fletcherstrestle.phasing"));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.PHASE_THROUGH_BLOCKS.get();
    }
}
