package net.frostytrix.fletcherstrestle.progression;

import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Archery XP / level helpers (Phase 2).
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
                        getXp(player), skills.draw(), skills.crit(), skills.aim()));
    }
}
