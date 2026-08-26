package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * The parts a modular weapon was built from.
 *
 * <p>{@code secondLimb} is only present on a <em>composite</em> bow, one
 * laminated from two different woods. It is an optional field so bows saved
 * before composites existed still load, and so an ordinary single-wood bow
 * stays exactly as it was.</p>
 */
public record BowAssembly(String limbMaterial, Optional<String> secondLimb,
                          String riserMaterial, String stringMaterial, float tuning) {

    /** Convenience for the common single-wood case. */
    public BowAssembly(String limbMaterial, String riserMaterial, String stringMaterial, float tuning) {
        this(limbMaterial, Optional.empty(), riserMaterial, stringMaterial, tuning);
    }

    /**
     * The same assembly at a different tuning. Use this instead of rebuilding
     * the record by hand: a manual 4-argument rebuild silently drops the second
     * limb and turns a composite back into a plain bow.
     */
    public BowAssembly withTuning(float newTuning) {
        return new BowAssembly(limbMaterial, secondLimb, riserMaterial, stringMaterial, newTuning);
    }

    /** True when this bow was laminated from two different woods. */
    public boolean isComposite() {
        return secondLimb.isPresent() && !secondLimb.get().equals(limbMaterial);
    }

    public static final Codec<BowAssembly> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("limb").forGetter(BowAssembly::limbMaterial),
                    Codec.STRING.optionalFieldOf("second_limb").forGetter(BowAssembly::secondLimb),
                    Codec.STRING.fieldOf("riser").forGetter(BowAssembly::riserMaterial),
                    Codec.STRING.fieldOf("string").forGetter(BowAssembly::stringMaterial),
                    Codec.FLOAT.fieldOf("tuning").forGetter(BowAssembly::tuning)
            ).apply(instance, BowAssembly::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BowAssembly> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BowAssembly::limbMaterial,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), BowAssembly::secondLimb,
            ByteBufCodecs.STRING_UTF8, BowAssembly::riserMaterial,
            ByteBufCodecs.STRING_UTF8, BowAssembly::stringMaterial,
            ByteBufCodecs.FLOAT, BowAssembly::tuning,
            BowAssembly::new
    );
}
