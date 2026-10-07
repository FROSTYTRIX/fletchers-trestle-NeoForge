package net.frostytrix.fletcherstrestle.progression;

import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Archery XP / level helpers.
 *
 * <p>Level curve is a simple quadratic: cumulative XP to reach level L is
 * {@code 10 * L^2}, so each level costs a bit more than the last. Cheap to
 * compute and easy to reason about; can be retuned later without migration
 * since only raw XP is stored.</p>
 */
public final class ArcheryProgression {
    private ArcheryProgression() {
    }

    private static final int XP_PER_LEVEL_FACTOR = 10;

    public static int getXp(Player player) {
        return player.getData(ModAttachments.ARCHERY_XP.get());
    }

    public static int getLevel(Player player) {
        return levelForXp(getXp(player));
    }

    /** Cumulative XP needed to reach {@code level}. */
    public static int xpForLevel(int level) {
        return XP_PER_LEVEL_FACTOR * level * level;
    }

    public static int levelForXp(int xp) {
        int level = (int) Math.floor(Math.sqrt((double) xp / XP_PER_LEVEL_FACTOR));
        return Math.min(level, FletcherConfig.ARCHERY_MAX_LEVEL.get());
    }

    /**
     * Grants XP to a player, clamped to the max-level cap, and announces any
     * level-ups. Server-side only.
     */
    public static void addXp(ServerPlayer player, int amount) {
        if (amount <= 0 || !FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return;
        }
        int cap = xpForLevel(FletcherConfig.ARCHERY_MAX_LEVEL.get());
        int before = getXp(player);
        if (before >= cap) {
            return;
        }
        int oldLevel = levelForXp(before);
        int after = Math.min(cap, before + amount);
        player.setData(ModAttachments.ARCHERY_XP.get(), after);

        int newLevel = levelForXp(after);
        if (newLevel > oldLevel) {
            player.displayClientMessage(
                    Component.translatable("gui.fletcherstrestle.archery_level_up", newLevel)
                            .withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            ModCriteria.ARCHERY_LEVEL.get().trigger(player, newLevel);
        } else {
            // Brief progress readout on the action bar (XP into the current level).
            int into = after - xpForLevel(newLevel);
            int span = Math.max(1, xpForLevel(newLevel + 1) - xpForLevel(newLevel));
            player.displayClientMessage(
                    Component.translatable("gui.fletcherstrestle.archery_progress", newLevel, into, span)
                            .withStyle(ChatFormatting.GRAY), true);
        }
        syncToClient(player);
    }

    // ---------------- Skill tree ----------------

    public static ArcherySkills getSkills(Player player) {
        return player.getData(ModAttachments.ARCHERY_SKILLS.get());
    }

    public static int getRank(Player player, ArcherySkill skill) {
        return skill.rank(getSkills(player));
    }

    public static int pointsSpent(Player player) {
        return getSkills(player).total();
    }

    /** Unspent skill points: one earned per level. */
    public static int pointsAvailable(Player player) {
        return Math.max(0, getLevel(player) - pointsSpent(player));
    }

    /**
     * Spends one point in {@code skill} if the player has a point free and the
     * branch isn't maxed. Returns true on success. Server-side.
     */
    public static boolean trySpend(ServerPlayer player, ArcherySkill skill) {
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return false;
        }
        ArcherySkills skills = getSkills(player);
        if (pointsAvailable(player) <= 0 || skill.rank(skills) >= ArcherySkill.MAX_RANK) {
            return false;
        }
        player.setData(ModAttachments.ARCHERY_SKILLS.get(), skill.increment(skills));
        syncToClient(player);
        return true;
    }

    /**
     * Buys the capstone at the tip of {@code skill}'s branch: the branch must be
     * maxed, the capstone not yet owned, the archer under the capstone limit, and
     * holding enough points. Returns true on success. Server-side.
     */
    public static boolean trySpendCapstone(ServerPlayer player, ArcherySkill skill) {
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return false;
        }
        ArcherySkills skills = getSkills(player);
        if (skill.rank(skills) < ArcherySkill.MAX_RANK
                || skills.hasCapstone(skill)
                || skills.capstoneCount() >= FletcherConfig.MAX_CAPSTONES.get()
                || pointsAvailable(player) < FletcherConfig.CAPSTONE_COST.get()) {
            return false;
        }
        ArcherySkills updated = skills.withCapstone(skill);
        player.setData(ModAttachments.ARCHERY_SKILLS.get(), updated);
        syncToClient(player);
        ModCriteria.CAPSTONE.get().trigger(player, updated.capstoneCount());
        return true;
    }

    public static boolean hasCapstone(Player player, ArcherySkill skill) {
        return FletcherConfig.ARCHERY_SKILL_ENABLED.get() && getSkills(player).hasCapstone(skill);
    }

    /**
     * Hitboxes at least this wide for their height belong to long, low bodies whose
     * head sits out front (cows, pigs, horses, wolves, spiders). Narrower ones are
     * upright (players, zombies, skeletons, creepers), with the head on top.
     */
    private static final float LONG_BODY_RATIO = 0.6f;

    /**
     * The headshot rule, shared by the XP bonus, the advancements and Called Shot
     * so they can never disagree. No per-mob setup: the hitbox's shape decides.
     * <ul>
     *   <li>Upright mobs: the top 30% of the hitbox.</li>
     *   <li>Long, low mobs: the front of the body (toward where it's facing), at
     *       head height. A cow's head is out front, not on top of its back.</li>
     * </ul>
     */
    public static boolean isHeadshot(net.minecraft.world.entity.projectile.AbstractArrow arrow,
                                     net.minecraft.world.entity.LivingEntity target) {
        net.minecraft.world.phys.Vec3 hit = impactPoint(arrow, target);
        float height = Math.max(0.1f, target.getBbHeight());
        float width = target.getBbWidth();
        double up = hit.y - target.getY();
        if (width / height < LONG_BODY_RATIO) {
            return up / height >= 0.7;
        }
        float yaw = target.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
        double forward = -net.minecraft.util.Mth.sin(yaw) * (hit.x - target.getX())
                + net.minecraft.util.Mth.cos(yaw) * (hit.z - target.getZ());
        return forward >= width * 0.2 && up >= target.getEyeHeight() - height * 0.3;
    }

    /**
     * Where the arrow actually entered the target. Vanilla resolves the hit before it
     * moves the arrow, so the arrow's own position is still where the tick began, up
     * to a few blocks short; its path is clipped against the hitbox instead, inflated
     * the same 0.3 that arrow hit detection uses.
     */
    public static net.minecraft.world.phys.Vec3 impactPoint(net.minecraft.world.entity.projectile.AbstractArrow arrow,
                                                         net.minecraft.world.entity.Entity target) {
        net.minecraft.world.phys.Vec3 from = arrow.position();
        net.minecraft.world.phys.Vec3 to = from.add(arrow.getDeltaMovement());
        return target.getBoundingBox().inflate(0.3).clip(from, to).orElse(from);
    }

    /** Set on an arrow whose Crit skill roll succeeded, so Called Shot never doubles it. */
    public static final String SKILL_CRIT_TAG = "fletcherstrestle:skill_crit";

    // ---------------- Branch effects ----------------

    /** Draw-time multiplier: 1.0 down to 0.8 at DRAW max rank. */
    public static float drawMultiplier(Player player) {
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return 1.0f;
        }
        return 1.0f - 0.02f * getRank(player, ArcherySkill.DRAW);
    }

    /** Crit chance: 0 up to 0.30 at CRIT max rank. */
    public static float critChance(Player player) {
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return 0.0f;
        }
        return 0.03f * getRank(player, ArcherySkill.CRIT);
    }

    /** Aim spread multiplier: 1.0 down to 0.7 at AIM max rank. */
    public static float inaccuracyMultiplier(Player player) {
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return 1.0f;
        }
        return 1.0f - 0.03f * getRank(player, ArcherySkill.AIM);
    }

    /** Rolls a crit from CRIT rank; on success boosts arrow damage 1.5x and marks it crit. */
    public static void rollCrit(Player player, net.minecraft.world.entity.projectile.AbstractArrow arrow) {
        float chance = critChance(player);
        if (chance > 0 && player.getRandom().nextFloat() < chance) {
            arrow.setBaseDamage(arrow.getBaseDamage() * 1.5);
            arrow.setCritArrow(true);
            arrow.getPersistentData().putBoolean(SKILL_CRIT_TAG, true);
        }
    }

    /** Extra ticks before a flax string starts shaking the aim (40 base, +8/AIM rank). */
    public static int flaxGraceTicks(Player player) {
        int base = 40;
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return base;
        }
        return base + 8 * getRank(player, ArcherySkill.AIM);
    }

    /** Pushes current XP + skill ranks to the owning client (for the HUD/skill screen). */
    public static void syncToClient(ServerPlayer player) {
        ArcherySkills skills = getSkills(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new net.frostytrix.fletcherstrestle.network.ArcherySyncPacket(
                        getXp(player), skills.draw(), skills.crit(), skills.aim(), skills.capstones()));
    }
}
