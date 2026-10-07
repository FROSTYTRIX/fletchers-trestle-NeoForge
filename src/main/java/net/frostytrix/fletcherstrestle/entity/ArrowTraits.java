package net.frostytrix.fletcherstrestle.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/**
 * The bow traits that live on the arrow as flags: amphibious (mangrove limbs), punch
 * (spruce limbs) and conductive (copper riser).
 *
 * <p>The bow stamps the flag on whatever arrow it fires. The mod's own arrow acts on
 * it itself; for every other arrow (a vanilla arrow from a modular bow, or any arrow
 * a skeleton or pillager fires) {@code ArrowTraitEvents} calls these instead. One
 * implementation for both, so the two can't drift.</p>
 */
public final class ArrowTraits {
    private ArrowTraits() {
    }

    public static final String AMPHIBIOUS = "fletcherstrestle:amphibious";
    public static final String PUNCH = "fletcherstrestle:punch";
    public static final String CONDUCTIVE = "fletcherstrestle:conductive";

    public static boolean has(AbstractArrow arrow, String trait) {
        return arrow.getPersistentData().getBoolean(trait);
    }

    /** Amphibious: counter most of the water's drag, trailing bubbles. Call every server tick. */
    public static void amphibiousTick(AbstractArrow arrow) {
        if (arrow.level().isClientSide() || !arrow.isInWater() || !has(arrow, AMPHIBIOUS)) return;
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.65D));
        if (arrow.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Punch: an extra knockback impulse on top of whatever vanilla applies, using
     * vanilla's own arrow formula (horizontal velocity, knockback resistance) scaled
     * to roughly match the Punch I enchantment.
     */
    public static void punch(AbstractArrow arrow, LivingEntity target) {
        if (!has(arrow, PUNCH)) return;
        double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 push = arrow.getDeltaMovement()
                .multiply(1.0, 0.0, 1.0)
                .normalize()
                .scale(0.6 * 0.6 * resistance);  // strength 0.6 * vanilla's 0.6 scale
        if (push.lengthSqr() > 0) {
            target.push(push.x, 0.1, push.z);
            target.hurtMarked = true;
        }
    }

    /** Conductive: a hit during a thunderstorm calls lightning down on the target. */
    public static void conduct(AbstractArrow arrow, LivingEntity target) {
        if (!has(arrow, CONDUCTIVE) || !arrow.level().isThundering()) return;
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(arrow.level());
        if (lightning != null) {
            lightning.moveTo(target.position());
            arrow.level().addFreshEntity(lightning);
        }
    }
}
