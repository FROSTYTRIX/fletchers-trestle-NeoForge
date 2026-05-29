package net.frostytrix.fletcherstrestle.progression;

import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
        } else {
            // Brief progress readout on the action bar (XP into the current level).
            int into = after - xpForLevel(newLevel);
            int span = Math.max(1, xpForLevel(newLevel + 1) - xpForLevel(newLevel));
            player.displayClientMessage(
                    Component.translatable("gui.fletcherstrestle.archery_progress", newLevel, into, span)
                            .withStyle(ChatFormatting.GRAY), true);
        }
    }

    /**
     * Aim-steadiness multiplier from archery level: 1.0 at level 0, scaling
     * down to 0.5 (50% tighter spread) at the configured max level. Returns
     * 1.0 when the skill system is disabled.
     */
    public static float inaccuracyMultiplier(Player player) {
        if (!FletcherConfig.ARCHERY_SKILL_ENABLED.get()) {
            return 1.0f;
        }
        int max = FletcherConfig.ARCHERY_MAX_LEVEL.get();
        float t = max <= 0 ? 0f : Mth.clamp((float) getLevel(player) / max, 0f, 1f);
        return 1.0f - 0.5f * t;
    }
}
