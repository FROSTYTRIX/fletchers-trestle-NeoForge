package net.frostytrix.fletcherstrestle.client;

import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.progression.ArcheryProgression;
import net.frostytrix.fletcherstrestle.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * A soft click the moment a modular bow reaches full draw.
 *
 * <p>Draw time depends on the wood, the tuning and the Draw skill, so the pull
 * texture and the FOV aren't a reliable cue. Played locally, for the archer only.</p>
 */
public final class DrawReadyCue {
    private DrawReadyCue() {
    }

    /** Whether the cue already sounded for the current draw. */
    private static boolean sounded;

    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof ModularBowItem bow)) {
            sounded = false;
            return;
        }
        // The same threshold the release uses for a full-power shot.
        float fullDraw = bow.getDrawTime(player.getUseItem()) * ArcheryProgression.drawMultiplier(player);
        if (!sounded && player.getTicksUsingItem() >= fullDraw) {
            sounded = true;
            player.level().playLocalSound(player.getX(), player.getEyeY(), player.getZ(),
                    ModSounds.BOW_READY.get(), SoundSource.PLAYERS, 1.0f, 1.0f, false);
        }
    }
}
