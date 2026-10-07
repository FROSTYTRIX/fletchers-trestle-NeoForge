package net.frostytrix.fletcherstrestle.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * Which items count as garland feathers, and the colour each dyes the bunting.
 *
 * <p>A NeoForge data map on items, at
 * {@code data/<ns>/data_maps/item/garland_feather.json}:</p>
 * <pre>{ "values": { "mypack:peacock_feather": { "colour": "#1F7A8C" } } }</pre>
 * <p>The colour is a {@code "#RRGGBB"} string or a plain integer.</p>
 */
public record GarlandFeather(int colour) {

    private static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(text -> {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        try {
            return DataResult.success(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a #RRGGBB colour: " + text);
        }
    }, value -> String.format("#%06X", value & 0xFFFFFF));

    public static final Codec<GarlandFeather> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.withAlternative(HEX, Codec.INT).fieldOf("colour").forGetter(GarlandFeather::colour)
    ).apply(inst, GarlandFeather::new));

    public static final DataMapType<Item, GarlandFeather> DATA_MAP = DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "garland_feather"),
                    Registries.ITEM, CODEC)
            .synced(CODEC, false)
            .build();
}
