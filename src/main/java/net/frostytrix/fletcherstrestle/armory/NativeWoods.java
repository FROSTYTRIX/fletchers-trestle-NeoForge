package net.frostytrix.fletcherstrestle.armory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

import java.util.List;

/**
 * The woods that grow in a biome, so an archer who spawns there carries a bow made
 * from the trees around it: a skeleton in a birch forest draws a birch bow.
 *
 * <p>A NeoForge data map on biomes, at
 * {@code data/<ns>/data_maps/worldgen/biome/native_woods.json}. Keys can be biome
 * ids or biome tags, so a pack can teach it its own biomes. Oak isn't listed: it
 * grows nearly everywhere, and mobs that want it say so in their own
 * {@link MobArmory} entry.</p>
 */
public record NativeWoods(List<String> woods) {

    public static final Codec<NativeWoods> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("woods").forGetter(NativeWoods::woods)
    ).apply(inst, NativeWoods::new));

    public static final DataMapType<Biome, NativeWoods> DATA_MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "native_woods"),
            Registries.BIOME, CODEC).build();
}
