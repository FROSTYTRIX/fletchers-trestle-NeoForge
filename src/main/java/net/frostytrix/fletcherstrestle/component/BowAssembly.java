package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BowAssembly(String limbMaterial, String riserMaterial, String stringMaterial, float tuning) {

    public static final Codec<BowAssembly> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("limb").forGetter(BowAssembly::limbMaterial),
                    Codec.STRING.fieldOf("riser").forGetter(BowAssembly::riserMaterial),
                    Codec.STRING.fieldOf("string").forGetter(BowAssembly::stringMaterial),
                    Codec.FLOAT.fieldOf("tuning").forGetter(BowAssembly::tuning)
            ).apply(instance, BowAssembly::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BowAssembly> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BowAssembly::limbMaterial,
            ByteBufCodecs.STRING_UTF8, BowAssembly::riserMaterial,
            ByteBufCodecs.STRING_UTF8, BowAssembly::stringMaterial,
            ByteBufCodecs.FLOAT, BowAssembly::tuning,
            BowAssembly::new
    );
}