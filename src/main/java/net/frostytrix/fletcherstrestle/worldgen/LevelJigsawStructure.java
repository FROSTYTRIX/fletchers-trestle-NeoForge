package net.frostytrix.fletcherstrestle.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.Optional;

/**
 * A jigsaw structure that only stands on level, dry ground.
 *
 * <p>A jigsaw structure takes its height from the terrain at its middle, so on a
 * hillside one side ends up buried and the other on a mound of dirt. That's fine
 * for a village plot, not for a stone tower. This takes the usual jigsaw fields
 * as they are, then checks the ground on a grid {@code footprint_radius} blocks
 * either side of the middle: the spot is skipped if the ground rises or falls by
 * more than {@code max_height_difference} blocks, or if there's water on it.</p>
 */
public class LevelJigsawStructure extends Structure {

    public static final MapCodec<LevelJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            JigsawStructure.CODEC.forGetter(s -> s.jigsaw),
            Codec.intRange(1, 32).optionalFieldOf("footprint_radius", 5).forGetter(s -> s.footprintRadius),
            Codec.intRange(0, 64).optionalFieldOf("max_height_difference", 3).forGetter(s -> s.maxHeightDifference)
    ).apply(i, LevelJigsawStructure::new));

    private final JigsawStructure jigsaw;
    private final int footprintRadius;
    private final int maxHeightDifference;

    public LevelJigsawStructure(JigsawStructure jigsaw, int footprintRadius, int maxHeightDifference) {
        super(new StructureSettings(jigsaw.biomes(), jigsaw.spawnOverrides(), jigsaw.step(), jigsaw.terrainAdaptation()));
        this.jigsaw = jigsaw;
        this.footprintRadius = footprintRadius;
        this.maxHeightDifference = maxHeightDifference;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return jigsaw.findGenerationPoint(context).filter(stub -> levelAndDry(context, stub.position()));
    }

    private boolean levelAndDry(GenerationContext context, BlockPos middle) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int dx = -footprintRadius; dx <= footprintRadius; dx += footprintRadius) {
            for (int dz = -footprintRadius; dz <= footprintRadius; dz += footprintRadius) {
                int x = middle.getX() + dx;
                int z = middle.getZ() + dz;
                int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState());
                // Anything standing on the floor up to the surface is water.
                if (surface != floor) return false;
                lowest = Math.min(lowest, surface);
                highest = Math.max(highest, surface);
            }
        }
        return highest - lowest <= maxHeightDifference;
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.LEVEL_JIGSAW.get();
    }
}
