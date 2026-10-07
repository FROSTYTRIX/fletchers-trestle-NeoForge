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

    /** Master switch for mobs spawning with modular weapons (see the mob_armory data map). */
    public static final ModConfigSpec.BooleanValue ARMED_MOBS;

    /** Share of a weapon's durability that one restring gives back. */
    public static final ModConfigSpec.DoubleValue RESTRING_REPAIR;

    /** Share of a weapon's durability that retuning it at the table costs. */
    public static final ModConfigSpec.DoubleValue RETUNE_COST;

    // --- GARRISON ---
    public static final ModConfigSpec.IntValue GARRISON_RANGE;
    public static final ModConfigSpec.IntValue GARRISON_SCOPED_RANGE;
    public static final ModConfigSpec.IntValue GARRISON_HALF_CONE;
    public static final ModConfigSpec.IntValue GARRISON_HOME_RADIUS;

    // --- MARKSMANSHIP: per-player archery XP / leveling ---
    public static final ModConfigSpec.BooleanValue ARCHERY_SKILL_ENABLED;
    public static final ModConfigSpec.IntValue ARCHERY_XP_PER_HIT;
    public static final ModConfigSpec.IntValue ARCHERY_XP_HEADSHOT_BONUS;
    public static final ModConfigSpec.IntValue ARCHERY_XP_PER_KILL;
    public static final ModConfigSpec.IntValue ARCHERY_MAX_LEVEL;
    public static final ModConfigSpec.IntValue CAPSTONE_COST;
    public static final ModConfigSpec.IntValue MAX_CAPSTONES;

    // --- CLIENT CONFIG (Local UI only) ---
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.DoubleValue QUIVER_HUD_X;
    public static final ModConfigSpec.DoubleValue QUIVER_HUD_Y;
    public static final ModConfigSpec.BooleanValue QUIVER_ON_BACK;

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
        CAPSTONE_COST = serverBuilder
                .comment(
                        "Skill points a capstone costs. Capstones unlock at the tip of a maxed branch.",
                        "At the defaults (max level 50, 3 branches of 10 ranks, cost 10, two capstones)",
                        "a fully levelled archer spends exactly every point.")
                .defineInRange("capstone_cost", 10, 1, 1000);
        MAX_CAPSTONES = serverBuilder
                .comment("How many of the three capstones one archer may own. Choosing is the point.")
                .defineInRange("max_capstones", 2, 0, 3);
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
        RESTRING_REPAIR = serverBuilder
                .comment(
                        "Restringing a bow or crossbow at the Fletching Table restores this share of its",
                        "durability (0.5 = half). Photosynthesis is the other repair route, for wooden bows.")
                .defineInRange("restring_repair", 0.5, 0.0, 1.0);
        RETUNE_COST = serverBuilder
                .comment(
                        "Retuning a bow or crossbow without restringing it costs this share of its durability:",
                        "tuning works the string. A retune that would break the weapon is refused.")
                .defineInRange("retune_cost", 0.1, 0.0, 1.0);
        serverBuilder.pop();

        serverBuilder.push("world");
        ARMED_MOBS = serverBuilder
                .comment(
                        "Whether skeletons, strays, bogged and pillagers can spawn carrying modular bows and",
                        "crossbows. Which mobs, how often and which woods live in the data map",
                        "data/fletcherstrestle/data_maps/entity_type/mob_armory.json, so packs can extend it.")
                .define("armed_mobs", true);
        serverBuilder.pop();

        serverBuilder.push("garrison");
        GARRISON_RANGE = serverBuilder
                .comment("How far an emplacement shoots, in blocks.")
                .defineInRange("range", 16, 1, 128);
        GARRISON_SCOPED_RANGE = serverBuilder
                .comment("Its range with a scope (any attachment that zooms) fitted.")
                .defineInRange("scoped_range", 32, 1, 128);
        GARRISON_HALF_CONE = serverBuilder
                .comment("Half the firing cone, in degrees either side of the emplacement's facing.")
                .defineInRange("half_cone", 45, 1, 180);
        GARRISON_HOME_RADIUS = serverBuilder
                .comment(
                        "How far a Bolt Warden strays from where it was built, and how far from there it",
                        "looks for emplacements to crew.")
                .defineInRange("home_radius", 16, 2, 64);
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
        clientBuilder.push("rendering");
        QUIVER_ON_BACK = clientBuilder
                .comment("Show quivers on players' backs.")
                .define("quiver_on_back", true);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }
}