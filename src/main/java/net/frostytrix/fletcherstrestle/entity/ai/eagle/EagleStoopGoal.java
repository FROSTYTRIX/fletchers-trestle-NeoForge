package net.frostytrix.fletcherstrestle.entity.ai.eagle;

import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * The stoop: a raptor's hunting dive. The eagle climbs high above its quarry,
 * folds and drops on it at speed, strikes, pulls up and climbs for another pass.
 *
 * <p>A tame eagle stoops on whatever its owner marked through the spyglass: each
 * strike does its attack damage and leaves the target glowing, so the eagle
 * softens and spots it while the kill stays the archer's. If the quarry is under
 * cover there's no dive to be had, so it circles overhead instead, marking it.</p>
 *
 * <p>A wild eagle hunts for itself every few minutes: a rabbit in the open, or a
 * fish just under the surface, taken in one strike and carried off in its
 * talons, home to the nest to feed any eaglets there, or to a lookout to eat.</p>
 */
public class EagleStoopGoal extends Goal {

    private enum Phase { CLIMB, STOOP, PULL_UP, CIRCLE, CARRY, FEED }

    /** How long it stays down eating or feeding. */
    private static final int FEED_TICKS = 100;
    /** Eaglets this close to the nest get fed. */
    private static final double EAGLET_RANGE = 8.0;

    private static final int TAME_TIMEOUT = 1200;
    private static final int WILD_TIMEOUT = 500;
    private static final double PREY_RANGE = 32.0;
    private static final int MIN_WILD_INTERVAL = 3600;
    private static final int MAX_WILD_INTERVAL = 9600;
    /** How high above the quarry it climbs before diving. */
    private static final double STOOP_HEIGHT = 14.0;
    private static final double STRIKE_RANGE = 1.6;
    private static final double STOOP_SPEED = 2.8;
    private static final int GLOW_TICKS = 60;

    private final EagleEntity eagle;
    @Nullable
    private LivingEntity quarry;
    private Phase phase = Phase.CLIMB;
    private int phaseTicks;
    private int huntTicks;
    private int repath;
    private final EagleLanding landing = new EagleLanding();
    @Nullable
    private Vec3 meal;
    private boolean done;

    public EagleStoopGoal(EagleEntity eagle) {
        this.eagle = eagle;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (eagle.isOrderedToSit() || eagle.isBaby() || eagle.isInjured()) return false;
        if (eagle.isTame()) {
            LivingEntity target = eagle.getHuntTarget();
            if (target == null || !target.isAlive()) return false;
            quarry = target;
            return true;
        }
        if (eagle.getEagleState() != EagleEntity.STATE_IDLE || eagle.shouldShelter()) return false;
        if (eagle.level().getGameTime() < eagle.getNextWildHunt()) return false;
        // Only check now and then: scanning for prey every tick isn't worth it.
        if (eagle.getRandom().nextInt(40) != 0) return false;
        quarry = findPrey();
        if (quarry == null) {
            eagle.setNextWildHunt(eagle.level().getGameTime() + 600);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (eagle.isOrderedToSit() || done) return false;
        if (phase == Phase.CARRY || phase == Phase.FEED) return true;
        if (quarry == null || !quarry.isAlive()) return false;
        if (eagle.isTame() && eagle.getHuntTarget() != quarry) return false;
        return eagle.distanceToSqr(quarry) < 96.0 * 96.0;
    }

    @Override
    public void start() {
        if (eagle.isTame()) eagle.setEagleState(EagleEntity.STATE_HUNTING);
        phase = Phase.CLIMB;
        phaseTicks = 0;
        huntTicks = 0;
        repath = 0;
        done = false;
        meal = null;
    }

    @Override
    public void stop() {
        if (eagle.isTame()) {
            eagle.setHuntTarget(null);
        } else {
            eagle.setNextWildHunt(eagle.level().getGameTime() + MIN_WILD_INTERVAL
                    + eagle.getRandom().nextInt(MAX_WILD_INTERVAL - MIN_WILD_INTERVAL));
        }
        quarry = null;
        meal = null;
        // Interrupted on the way home: it eats the prey there and then.
        eagle.setCarriedPrey(net.minecraft.world.item.ItemStack.EMPTY);
        eagle.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (phase == Phase.CARRY || phase == Phase.FEED) {
            tickMeal();
            return;
        }
        LivingEntity target = quarry;
        if (target == null) return;
        if (!eagle.level().isLoaded(target.blockPosition())) return;

        if (++huntTicks > (eagle.isTame() ? TAME_TIMEOUT : WILD_TIMEOUT)) {
            if (eagle.isTame() && eagle.getOwner() instanceof Player owner) {
                owner.displayClientMessage(Component.translatable("message.fletcherstrestle.eagle_disengages"), true);
            }
            quarry = null;
            return;
        }
        phaseTicks++;
        boolean open = openSky(target);

        switch (phase) {
            case CLIMB -> {
                if (!open) {
                    circleOver(target, 6.0, 8.0);
                    return;
                }
                // Up above the quarry, a few blocks out on the side it's coming from.
                Vec3 from = eagle.position().subtract(target.position()).multiply(1, 0, 1);
                Vec3 out = from.lengthSqr() > 1.0E-4 ? from.normalize().scale(6.0) : Vec3.ZERO;
                Vec3 point = target.position().add(out).add(0, STOOP_HEIGHT, 0);
                fly(point, 1.4);
                boolean high = eagle.getY() > target.getY() + STOOP_HEIGHT - 3.0;
                double flat = Math.hypot(eagle.getX() - target.getX(), eagle.getZ() - target.getZ());
                if ((high && flat < 12.0) || phaseTicks > 200) {
                    enter(Phase.STOOP);
                    eagle.playSound(ModSounds.EAGLE_AMBIENT.get(), 1.0f, 0.8f);
                    eagle.playSound(ModSounds.EAGLE_DIVE.get(), 1.2f, 1.0f);
                }
            }
            case STOOP -> {
                Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
                double eta = eagle.position().distanceTo(aim) / 1.1;
                aim = aim.add(target.getDeltaMovement().multiply(eta, 0, eta));
                eagle.getNavigation().stop();
                eagle.getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, STOOP_SPEED);
                if (eagle.getBoundingBox().inflate(STRIKE_RANGE - 1.0).intersects(target.getBoundingBox())
                        || eagle.distanceToSqr(aim) < STRIKE_RANGE * STRIKE_RANGE) {
                    strike(target);
                    if (phase == Phase.STOOP) enter(Phase.PULL_UP);
                } else if (phaseTicks > 70 || !open) {
                    enter(Phase.PULL_UP);
                }
            }
            case PULL_UP -> {
                Vec3 v = eagle.getDeltaMovement().multiply(1, 0, 1);
                Vec3 ahead = v.lengthSqr() > 1.0E-4 ? v.normalize().scale(8.0) : Vec3.ZERO;
                fly(eagle.position().add(ahead).add(0, 6.0, 0), 1.3);
                if (phaseTicks > 25) enter(Phase.CIRCLE);
            }
            case CIRCLE -> {
                circleOver(target, 10.0, 12.0);
                if (phaseTicks > 50) enter(Phase.CLIMB);
            }
        }
    }

    private void strike(LivingEntity target) {
        eagle.playSound(ModSounds.EAGLE_FLAP.get(), 1.0f, 0.7f);
        if (eagle.isTame()) {
            // Players and the owner's own animals are only marked, never struck.
            boolean spared = target instanceof Player
                    || (target instanceof net.minecraft.world.entity.TamableAnimal pet
                    && pet.getOwnerUUID() != null && pet.getOwnerUUID().equals(eagle.getOwnerUUID()));
            if (!spared) {
                eagle.doHurtTarget(target);
                if (target.isDeadOrDying() && eagle.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) {
                    net.frostytrix.fletcherstrestle.progression.ModCriteria.EAGLE.get().trigger(owner,
                            net.frostytrix.fletcherstrestle.progression.EagleTrigger.STOOP_KILL);
                }
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS), eagle);
        } else {
            snatch(target);
        }
    }

    /** A wild strike: the prey is taken whole and carried off. */
    private void snatch(LivingEntity prey) {
        eagle.level().broadcastEntityEvent(prey, (byte) 60);
        eagle.setCarriedPrey(new net.minecraft.world.item.ItemStack(preyItem(prey)));
        prey.discard();
        quarry = null;
        meal = mealSpot();
        landing.reset();
        enter(meal != null ? Phase.CARRY : Phase.FEED);
    }

    private static net.minecraft.world.item.Item preyItem(LivingEntity prey) {
        if (prey instanceof net.minecraft.world.entity.animal.Salmon) return net.minecraft.world.item.Items.SALMON;
        if (prey instanceof net.minecraft.world.entity.animal.Pufferfish) return net.minecraft.world.item.Items.PUFFERFISH;
        if (prey instanceof net.minecraft.world.entity.animal.TropicalFish) return net.minecraft.world.item.Items.TROPICAL_FISH;
        if (prey instanceof AbstractFish) return net.minecraft.world.item.Items.COD;
        return net.minecraft.world.item.Items.RABBIT;
    }

    /** Home to the nest if it has one, otherwise the nearest good lookout. */
    @Nullable
    private Vec3 mealSpot() {
        var nest = eagle.getNestPos();
        if (nest != null && eagle.level().isLoaded(nest)) {
            return new Vec3(nest.getX() + 0.5, nest.getY() + 4.0 / 16.0, nest.getZ() + 0.5);
        }
        return EagleLanding.findPerch(eagle, eagle.blockPosition(), 10, eagle.getRandom());
    }

    private void tickMeal() {
        phaseTicks++;
        if (phase == Phase.CARRY) {
            if (meal == null || landing.tick(eagle, meal, 1.0)) {
                enter(Phase.FEED);
            } else if (phaseTicks > 600) {
                enter(Phase.FEED);
            }
            return;
        }
        if (phaseTicks == 1) {
            feedEaglets();
        }
        if (phaseTicks % 20 == 0) {
            eagle.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 0.6f, 1.2f);
        }
        if (phaseTicks >= FEED_TICKS) {
            eagle.setCarriedPrey(net.minecraft.world.item.ItemStack.EMPTY);
            done = true;
        }
    }

    /** Eaglets round the nest grow a little with every meal brought home. */
    private void feedEaglets() {
        var nest = eagle.getNestPos();
        if (nest == null) return;
        var area = new net.minecraft.world.phys.AABB(nest).inflate(EAGLET_RANGE);
        for (EagleEntity eaglet : eagle.level().getEntitiesOfClass(EagleEntity.class, area, EagleEntity::isBaby)) {
            eaglet.ageUp(net.minecraft.world.entity.AgeableMob.getSpeedUpSecondsWhenFeeding(-eaglet.getAge()), true);
        }
    }

    private void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private void fly(Vec3 point, double speed) {
        if (--repath <= 0 || eagle.getNavigation().isDone()) {
            eagle.getNavigation().moveTo(point.x, point.y, point.z, speed);
            repath = 8;
        }
    }

    private void circleOver(LivingEntity target, double radius, double height) {
        Vec3 point = EagleFlightControl.orbitPoint(eagle.position(), target.position(), radius,
                target.getY() + height, 1);
        eagle.getNavigation().stop();
        eagle.getMoveControl().setWantedPosition(point.x, point.y, point.z, 1.2);
    }

    /** Room to dive: open sky over the quarry, and a clear line from the eagle to it. */
    private boolean openSky(LivingEntity target) {
        if (!eagle.level().canSeeSky(target.blockPosition().above())) return false;
        var hit = eagle.level().clip(new ClipContext(eagle.getEyePosition(), target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, eagle));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** A rabbit in the open, or a fish right under the surface, somewhere below. */
    @Nullable
    private LivingEntity findPrey() {
        var area = eagle.getBoundingBox().inflate(PREY_RANGE, 32.0, PREY_RANGE);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : eagle.level().getEntitiesOfClass(LivingEntity.class, area, this::isPrey)) {
            double d = eagle.distanceToSqr(e);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isPrey(LivingEntity e) {
        if (!e.isAlive() || e.getY() > eagle.getY()) return false;
        if (!eagle.level().canSeeSky(e.blockPosition().above())) return false;
        if (e instanceof Rabbit) return !e.isInWater();
        if (e instanceof AbstractFish) {
            return e.isInWater() && eagle.level().getFluidState(e.blockPosition().above()).isEmpty();
        }
        return false;
    }
}
