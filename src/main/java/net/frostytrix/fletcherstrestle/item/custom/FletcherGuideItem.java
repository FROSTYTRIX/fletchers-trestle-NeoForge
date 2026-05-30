package net.frostytrix.fletcherstrestle.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Fletcher's Guide — opens the in-game guidebook screen (Phase 4). A button
// inside still links out to the full project wiki.
public class FletcherGuideItem extends Item {

    public FletcherGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // Client-only: open the guide screen.
            net.frostytrix.fletcherstrestle.client.guide.FletcherGuideScreen.open();
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.0f);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
