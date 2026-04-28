package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ArrowAssembly(String head, String shaft, String fletching) {

    // For saving to the world/item NBT
    public static final Codec<ArrowAssembly> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("head").forGetter(ArrowAssembly::head),
            Codec.STRING.fieldOf("shaft").forGetter(ArrowAssembly::shaft),
            Codec.STRING.fieldOf("fletching").forGetter(ArrowAssembly::fletching)
    ).apply(inst, ArrowAssembly::new));

    // For syncing between Server and Client (required for rendering!)
    public static final StreamCodec<RegistryFriendlyByteBuf, ArrowAssembly> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ArrowAssembly::head,
            ByteBufCodecs.STRING_UTF8, ArrowAssembly::shaft,
            ByteBufCodecs.STRING_UTF8, ArrowAssembly::fletching,
            ArrowAssembly::new
    );
}