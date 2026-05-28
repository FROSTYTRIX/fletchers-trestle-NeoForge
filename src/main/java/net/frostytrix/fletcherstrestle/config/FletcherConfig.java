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
     * in, and the eagle-nest world-gen feature. Default OFF for now — the
     * eagle model is still WIP. Flip back on once it ships.
     */
    public static final ModConfigSpec.BooleanValue EAGLES_NATURAL_SPAWNING;

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

        serverBuilder.push("eagles");
        EAGLES_NATURAL_SPAWNING = serverBuilder
                .comment(
                        "Whether eagles spawn naturally in the world (mountain-biome spawn pool + nest worldgen feature).",
                        "Disabled by default while the eagle model is WIP. The spawn-egg item still works regardless.",
                        "Flip to true to re-enable everything; no other changes needed.")
                .define("natural_spawning", false);
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