package net.frostytrix.fletcherstrestle.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class FletcherConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue MINIGAME_SPEED;
    public static final ModConfigSpec.DoubleValue MINIGAME_PUNISH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MINIGAME_MIN_SCORE;

    static {
        BUILDER.push("minigame_settings");

        MINIGAME_SPEED = BUILDER.comment("Speed of the cursor in the fletching minigame. Higher = faster. (Default: 0.02)")
                .defineInRange("cursor_speed", 0.02, 0.001, 0.5);

        MINIGAME_PUNISH_MULTIPLIER = BUILDER.comment("Multiplier for how punishing missing the sweet spot is. (Default: 3.0)")
                .defineInRange("punish_multiplier", 3.0, 0.0, 10.0);

        MINIGAME_MIN_SCORE = BUILDER.comment("The absolute minimum quality score a bow can get if you miss completely. (Default: 0.2)")
                .defineInRange("minimum_score", 0.2, 0.0, 1.0);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}