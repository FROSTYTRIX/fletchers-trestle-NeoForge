package net.frostytrix.fletcherstrestle.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FletcherConfig {
    // --- SERVER CONFIG (Synced & Locked to Server) ---
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue MINIGAME_SPEED;
    public static final ModConfigSpec.DoubleValue MINIGAME_PUNISH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MINIGAME_MIN_SCORE;
    /**
     * Master toggle for everything that adds eagles to a freshly-generated
     * world: the spawn-placement entry that lets the spawn pool tick them
     * in, and the eagle-nest world-gen feature. Enabled by default now that
     * the eagle model has shipped; players can still turn it off.
     */
    public static final ModConfigSpec.BooleanValue EAGLES_NATURAL_SPAWNING;

    /**
     * Whether two different woods can be laminated into one composite bow.
     * Off by default: a composite blends both woods' stats, which is strong
     * enough that a server should opt into it deliberately.
     */
    public static final ModConfigSpec.BooleanValue COMPOSITE_BOWS;

    // --- MARKSMANSHIP (Phase 2): per-player archery XP / leveling ---
    public static final ModConfigSpec.BooleanValue ARCHERY_SKILL_ENABLED;
    public static final ModConfigSpec.IntValue ARCHERY_XP_PER_HIT;
    public static final ModConfigSpec.IntValue ARCHERY_XP_HEADSHOT_BONUS;
    public static final ModConfigSpec.IntValue ARCHERY_XP_PER_KILL;
    public static final ModConfigSpec.IntValue ARCHERY_MAX_LEVEL;

    // --- CLIENT CONFIG (Local UI only) ---
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.DoubleValue QUIVER_HUD_X;
    public static final ModConfigSpec.DoubleValue QUIVER_HUD_Y;

    static {
        // Build Server Config
        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        serverBuilder.push("minigame_settings");
        MINIGAME_SPEED = serverBuilder.comment("Speed of the cursor. (Default: 0.02)")
                .defineInRange("cursor_speed", 0.02, 0.001, 0.5);
        MINIGAME_PUNISH_MULTIPLIER = serverBuilder.comment("Multiplier for missing the sweet spot. (Default: 3.0)")
                .defineInRange("punish_multiplier", 3.0, 0.0, 10.0);
        MINIGAME_MIN_SCORE = serverBuilder.comment("Minimum quality score if you miss completely. (Default: 0.2)")
                .defineInRange("minimum_score", 0.2, 0.0, 1.0);
        serverBuilder.pop();

        serverBuilder.push("marksmanship");
        ARCHERY_SKILL_ENABLED = serverBuilder
                .comment("Master toggle for the archery skill / XP system.")
                .define("archery_skill_enabled", true);
        ARCHERY_XP_PER_HIT = serverBuilder
                .comment("XP gained for landing an arrow on a living target.")
                .defineInRange("xp_per_hit", 2, 0, 1000);
        ARCHERY_XP_HEADSHOT_BONUS = serverBuilder
                .comment("Extra XP when the arrow hits the top of the target's hitbox (a headshot).")
                .defineInRange("xp_headshot_bonus", 3, 0, 1000);
        ARCHERY_XP_PER_KILL = serverBuilder
                .comment("Extra XP when an arrow hit kills the target.")
                .defineInRange("xp_per_kill", 5, 0, 1000);
        ARCHERY_MAX_LEVEL = serverBuilder
                .comment("Maximum archery level a player can reach.")
                .defineInRange("max_level", 50, 1, 1000);
        serverBuilder.pop();

        serverBuilder.push("eagles");
        EAGLES_NATURAL_SPAWNING = serverBuilder
                .comment(
                        "Whether eagles spawn naturally in the world (mountain-biome spawn pool + nest worldgen feature).",
                        "Enabled by default. The spawn-egg item still works regardless.",
                        "Set to false to disable natural spawning; no other changes needed.")
                .define("natural_spawning", true);
        serverBuilder.pop();

        serverBuilder.push("crafting");
        COMPOSITE_BOWS = serverBuilder
                .comment(
                        "Whether a bow can be built from two different woods, creating a composite that",
                        "blends both limbs' stats. Powerful, so it is off by default.",
                        "While off, two different limbs simply will not assemble.")
                .define("composite_bows", false);
        serverBuilder.pop();

        SERVER_SPEC = serverBuilder.build();

        // Build Client Config
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.push("hud_settings");
        QUIVER_HUD_X = clientBuilder.comment("X offset of the Quiver HUD")
                .defineInRange("quiver_hud_x", 0f, -2000, 2000);
        QUIVER_HUD_Y = clientBuilder.comment("Y position of the Quiver HUD")
                .defineInRange("quiver_hud_y", 15f, 0, 2000);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }
}