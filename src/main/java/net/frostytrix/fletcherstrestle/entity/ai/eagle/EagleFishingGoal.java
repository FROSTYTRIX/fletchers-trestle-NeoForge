package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A tame eagle with time on its talons, near water, now and then goes fishing:
 * it circles out over open water, stoops to the surface and comes up with a
 * fish (rolled from vanilla's fishing table), which it brings back to its owner
 * like fetched arrows.
 */
public class EagleFishingGoal extends Goal {

    private enum Phase { OUT, DIVE }

    /** Chance per check (every other tick) of going fishing: about once every few minutes. */
    private static final int ODDS = 2400;
    private static final int SEARCH_RADIUS = 16;
    private static final double MAX_OWNER_DISTANCE = 24.0;
    private static final int MAX_TICKS = 400;

    private final EagleEntity eagle;
    @Nullable
    private Vec3 surface;
    private Phase phase = Phase.OUT;
    private int ticks;

    public EagleFishingGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!eagle.isTame() || eagle.isOrderedToSit() || eagle.isBaby() || eagle.isInjured()) return false;
        if (eagle.getEagleState() != EagleEntity.STATE_IDLE || !eagle.isFetchInventoryEmpty()) return false;
        if (eagle.shouldShelter()) return false;
        LivingEntity owner = eagle.getOwner();
        if (owner == null || owner.level() != eagle.level() || eagle.distanceTo(owner) > MAX_OWNER_DISTANCE) return false;
        if (eagle.getRandom().nextInt(ODDS) != 0) return false;
        surface = findWater(owner.blockPosition());
        return surface != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = eagle.getOwner();
        return surface != null && ticks < MAX_TICKS && !eagle.isOrderedToSit()
                && eagle.getEagleState() == EagleEntity.STATE_IDLE
                && owner != null && eagle.distanceTo(owner) < MAX_OWNER_DISTANCE * 1.5;
    }

    @Override
    public void start() {
        phase = Phase.OUT;
        ticks = 0;
    }

    @Override
    public void stop() {
        surface = null;
        eagle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 water = surface;
        if (water == null) return;
        ticks++;
        if (phase == Phase.OUT) {
            Vec3 above = water.add(0, 7.0, 0);
            eagle.getMoveControl().setWantedPosition(above.x, above.y, above.z, 1.2);
            if (eagle.position().distanceToSqr(above) < 9.0) {
                phase = Phase.DIVE;
                eagle.playSound(ModSounds.EAGLE_DIVE.get(), 1.0f, 1.2f);
            }
            return;
        }
        eagle.getMoveControl().setWantedPosition(water.x, water.y, water.z, 2.2);
        if (eagle.position().distanceToSqr(water) < 1.4 * 1.4 || eagle.isInWater()) {
            catchFish(water);
            surface = null;
        }
    }

    private void catchFish(Vec3 at) {
        if (!(eagle.level() instanceof ServerLevel level)) return;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH, eagle.getSoundSource(), 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 12, 0.4, 0.1, 0.4, 0.1);
        var table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_FISH);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, at)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.FISHING);
        for (ItemStack fish : table.getRandomItems(params)) {
            // Into the fetch pouch: the fetch goal flies it home to the owner.
            ItemStack left = eagle.addToFetchInventory(fish);
            if (!left.isEmpty()) eagle.spawnAtLocation(left);
        }
        // Pull up hard out of the water.
        eagle.setDeltaMovement(eagle.getDeltaMovement().multiply(0.5, 0, 0.5).add(0, 0.5, 0));
    }

    /** Open water with sky above it, somewhere near the owner. */
    @Nullable
    private Vec3 findWater(BlockPos around) {
        var level = eagle.level();
        for (int i = 0; i < 16; i++) {
            int x = around.getX() + eagle.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = around.getZ() + eagle.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            if (!level.isLoaded(new BlockPos(x, around.getY(), z))) continue;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos water = new BlockPos(x, top - 1, z);
            if (level.getFluidState(water).is(FluidTags.WATER) && level.getFluidState(water.below()).is(FluidTags.WATER)) {
                return new Vec3(x + 0.5, top - 0.1, z + 0.5);
            }
        }
        return null;
    }
}
