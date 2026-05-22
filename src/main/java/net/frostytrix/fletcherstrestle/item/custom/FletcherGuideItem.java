package net.frostytrix.fletcherstrestle.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Fletcher's Guide — used to give the player a written book of mod info.
// Now it just opens the project wiki, which is the canonical reference
// (and stays in sync with the mod's actual behaviour without recompiling).
// The item is no longer consumed on use.
public class FletcherGuideItem extends Item {

    private static final String WIKI_URL =
            "https://github.com/FROSTYTRIX/fletchers-trestle-NeoForge/wiki";

    public FletcherGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // Build a clickable underlined link styled like a tooltip URL.
            Component link = Component.literal(WIKI_URL)
                    .withStyle(s -> s
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            // 26.1: ClickEvent/HoverEvent are interfaces; each
                            // variant is its own record (OpenUrl, ShowText, …).
                            .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(WIKI_URL)))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal("Open the wiki in your browser"))));

            Component message = Component.literal("[Fletcher's Trestle] ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Read the wiki for the full guide: ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(link);

            /* TODO(port-26.1) ServerPlayer.sendSystemMessage */ if (player instanceof net.minecraft.server.level.ServerPlayer __sp) __sp.sendSystemMessage(message, false);

            // Soft page-turn cue so the player gets feedback regardless of
            // whether they look at chat.
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.0f);
        }

        return InteractionResult.SUCCESS;
    }
}
