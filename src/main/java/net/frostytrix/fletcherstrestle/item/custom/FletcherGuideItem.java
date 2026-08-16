package net.frostytrix.fletcherstrestle.item.custom;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Fletcher's Guide: opens the in-game guidebook screen. A button inside links out to the wiki.
public class FletcherGuideItem extends Item {

    public FletcherGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // always show the enchantment glint
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // The guide is a Patchouli book. Its recipe is gated on Patchouli being
        // installed, so in survival the item only exists when the book does.
        // PatchouliCompat is only touched behind the isLoaded guard, so it never
        // classloads in packs without Patchouli.
        if (!level.isClientSide) {
            if (net.neoforged.fml.ModList.get().isLoaded("patchouli")
                    && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.frostytrix.fletcherstrestle.compat.PatchouliCompat.openGuide(serverPlayer);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.0f);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
