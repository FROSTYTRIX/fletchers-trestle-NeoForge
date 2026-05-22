package net.frostytrix.fletcherstrestle.worldgen;

import com.mojang.serialization.Codec;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.entity.EagleNestBlockEntity;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// EagleNestFeature — places an Eagle Nest block on a high, sky-exposed
// surface and seeds it with 0–3 random eggs plus 1–2 wild eagles whose
// `nestPos` is set so they patrol the nest.
//
// Wild eggs inside the nest hatch on their own via EagleNestBlockEntity's
// tick logic (it doesn't require an owner). Hatchlings are also untamed.
public class EagleNestFeature extends Feature<NoneFeatureConfiguration> {

    // Worldgen tuning. Kept here so they're easy to find / change.
    private static final int MIN_Y         = 70;   // includes high windswept hills
    private static final int MAX_EGGS_GEN  = 3;
    private static final int MIN_EAGLES    = 1;
    private static final int MAX_EAGLES    = 2;

    public EagleNestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // Snap to the top non-leaf surface at the origin's x/z. The placed
        // feature config gives us a column with an unstable Y; this gets us
        // onto the actual ridge.
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);

        if (surface.getY() < MIN_Y) return false;
        if (!level.canSeeSky(surface)) return false;

        // The block ABOVE the surface (where we place the nest) must be air.
        if (!level.isEmptyBlock(surface)) return false;

        // The block AT the surface (one below the nest) must be a real solid
        // block (not air, not single-layer snow, not grass tuft). Loosened
        // from isFaceSturdy because snow/ice surfaces sometimes fail that
        // check even when they're perfectly fine to perch on.
        BlockState support = level.getBlockState(surface.below());
        if (support.isAir()) return false;
        if (!support.blocksMotion()) return false;

        // Place the nest block.
        BlockState nestState = ModBlocks.EAGLE_NEST.get().defaultBlockState();
        if (!level.setBlock(surface, nestState, 3)) return false;
        FletcherTrestle.LOGGER.info("Eagle nest placed at {} (biome chunk feature)", surface);

        // Populate the BE with eggs + spawn wild eagles bound to it.
        if (level.getBlockEntity(surface) instanceof EagleNestBlockEntity nest) {
            long now = level.getLevelData().getGameTime();

            // 0 / 1 / 2 / 3 eggs, weighted to favor lower counts.
            int eggCount = pickWeightedEggCount(random);
            for (int i = 0; i < eggCount && i < MAX_EGGS_GEN; i++) {
                // Stagger each egg's lay time by a few seconds so they don't
                // all hatch on the same tick. Hatch time is laidAt + ~20 min.
                nest.addEgg(now - random.nextInt(200));
            }

            // 1 or 2 wild eagles patrolling this nest.
            int eagleCount = MIN_EAGLES + random.nextInt(MAX_EAGLES - MIN_EAGLES + 1);
            for (int i = 0; i < eagleCount; i++) {
                spawnWildEagle(level, surface, random);
            }
        }

        return true;
    }

    // 0 eggs: 30%, 1 egg: 35%, 2 eggs: 25%, 3 eggs: 10%. Skews toward
    // visually-empty nests so each populated one feels like a small reward.
    private static int pickWeightedEggCount(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 30)  return 0;
        if (roll < 65)  return 1;
        if (roll < 90)  return 2;
        return 3;
    }

    private static void spawnWildEagle(WorldGenLevel level, BlockPos nestPos, RandomSource random) {
        EagleEntity eagle = ModEntities.EAGLE.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (eagle == null) return;

        // Place each eagle in a small ring around (and above) the nest.
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist  = 2.0 + random.nextDouble() * 3.0;
        double x = nestPos.getX() + 0.5 + Math.cos(angle) * dist;
        double z = nestPos.getZ() + 0.5 + Math.sin(angle) * dist;
        double y = nestPos.getY() + 3.0 + random.nextDouble() * 4.0;

        // 26.1: moveTo(d,d,d,f,f) renamed to snapTo(d,d,d,f,f).
        eagle.snapTo(x, y, z, random.nextFloat() * 360.0f, 0f);
        eagle.setNestPos(nestPos);
        // Wild — no owner, no taming. Players need to tame these themselves.
        level.addFreshEntity(eagle);
    }
}
