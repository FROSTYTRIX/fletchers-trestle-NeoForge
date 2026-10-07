package net.frostytrix.fletcherstrestle.material.effect;

import com.mojang.serialization.MapCodec;
import net.frostytrix.fletcherstrestle.material.MaterialEffect;
import net.frostytrix.fletcherstrestle.material.MaterialEffectType;
import net.frostytrix.fletcherstrestle.material.ModMaterialEffectTypes;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The arrow collapses into a black hole wherever it lands, on a mob or a block,
 * and is used up.
 *
 * <p>JSON: {@code { "type": "fletcherstrestle:black_hole" }}</p>
 */
public record SpawnBlackHoleEffect() implements MaterialEffect {

    public static final MapCodec<SpawnBlackHoleEffect> CODEC = MapCodec.unit(SpawnBlackHoleEffect::new);

    @Override
    public Optional<Component> describe() {
        return Optional.of(Component.translatable("trait.fletcherstrestle.black_hole"));
    }

    @Override
    public MaterialEffectType<? extends MaterialEffect> type() {
        return ModMaterialEffectTypes.BLACK_HOLE.get();
    }
}
