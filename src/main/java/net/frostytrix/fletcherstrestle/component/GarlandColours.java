package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * The feathers a garland was woven from, in the order they were crafted.
 *
 * <p>Stored as plain packed RGB values rather than item ids, so the renderer can
 * colour the bunting without a registry lookup, and so a modpack's own feather
 * can join in simply by mapping to a colour at craft time.</p>
 *
 * <p>The list is a <em>mix</em>, not a segment-per-feather layout: the renderer
 * spreads these colours proportionally across however many pennants the span
 * needs, so a short garland and a long one both read correctly.</p>
 */
public record GarlandColours(List<Integer> colours) {

    public static final GarlandColours EMPTY = new GarlandColours(List.of(0xFFFFFF));

    public static final Codec<GarlandColours> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.listOf().fieldOf("colours").forGetter(GarlandColours::colours)
    ).apply(inst, GarlandColours::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GarlandColours> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.<RegistryFriendlyByteBuf>cast().apply(ByteBufCodecs.list()),
            GarlandColours::colours,
            GarlandColours::new
    );

    /** The colour for pennant {@code index} of {@code total}, spread proportionally. */
    public int colourAt(int index, int total) {
        if (colours.isEmpty()) {
            return 0xFFFFFF;
        }
        if (total <= 1) {
            return colours.get(0);
        }
        int slot = (int) ((long) index * colours.size() / total);
        return colours.get(Math.min(slot, colours.size() - 1));
    }
}
