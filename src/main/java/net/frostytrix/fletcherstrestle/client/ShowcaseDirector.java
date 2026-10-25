package net.frostytrix.fletcherstrestle.client;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Takes the mod page's screenshots by itself. Development only: it does
 * nothing in a released jar, nor in a dev run unless the system property
 * {@code fletcherstrestle.showcase} names a shot list (the {@code showcase} run
 * in build.gradle sets it to {@code tools/showcase/shots.txt}).
 *
 * <p>Once the player is in a world it works down the list, one instruction a
 * line: {@code cmd <command>} runs a command, {@code wait <ticks>} waits,
 * {@code hud on|off} shows or hides the HUD, {@code shot <name>} saves the
 * screen to {@code screenshots/<name>.png}, and {@code quit} closes the game.
 * Lines starting with {@code #} are skipped.</p>
 */
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public final class ShowcaseDirector {

    private static final String PROPERTY = "fletcherstrestle.showcase";
    /** Ticks to let the world settle after joining before the first instruction. */
    private static final int SETTLE_TICKS = 100;

    private static Deque<String> script;
    private static int waiting = SETTLE_TICKS;
    private static boolean finished;

    private ShowcaseDirector() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (finished || FMLEnvironment.production) return;
        String path = System.getProperty(PROPERTY);
        Minecraft minecraft = Minecraft.getInstance();
        if (path == null || minecraft.player == null || minecraft.level == null) return;
        if (script == null) {
            script = load(Path.of(path));
            if (script == null) {
                finished = true;
                return;
            }
            // Unattended: the window won't have focus, and the pause menu must stay out of the shots.
            minecraft.options.pauseOnLostFocus = false;
        }
        if (minecraft.screen != null) {
            minecraft.setScreen(null);
        }
        if (waiting > 0) {
            waiting--;
            return;
        }
        while (!script.isEmpty()) {
            String line = script.poll().strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int space = line.indexOf(' ');
            String verb = space < 0 ? line : line.substring(0, space);
            String rest = space < 0 ? "" : line.substring(space + 1).strip();
            switch (verb) {
                case "cmd" -> minecraft.player.connection.sendCommand(rest.startsWith("/") ? rest.substring(1) : rest);
                case "wait" -> {
                    waiting = Integer.parseInt(rest);
                    return;
                }
                case "hud" -> minecraft.options.hideGui = rest.equals("off");
                case "shot" -> {
                    Screenshot.grab(minecraft.gameDirectory, rest + ".png", minecraft.getMainRenderTarget(),
                            message -> FletcherTrestle.LOGGER.info("[showcase] {}", message.getString()));
                    // A frame for the next view to replace this one before anything else happens.
                    waiting = 2;
                    return;
                }
                case "quit" -> {
                    finished = true;
                    minecraft.stop();
                    return;
                }
                default -> FletcherTrestle.LOGGER.warn("[showcase] unknown instruction: {}", line);
            }
        }
        finished = true;
        FletcherTrestle.LOGGER.info("[showcase] shot list done");
    }

    private static Deque<String> load(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            FletcherTrestle.LOGGER.info("[showcase] running {} ({} lines)", path, lines.size());
            return new ArrayDeque<>(lines);
        } catch (IOException e) {
            FletcherTrestle.LOGGER.error("[showcase] can't read shot list {}", path, e);
            return null;
        }
    }
}
