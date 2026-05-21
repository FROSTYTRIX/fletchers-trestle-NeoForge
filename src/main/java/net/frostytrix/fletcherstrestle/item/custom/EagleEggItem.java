package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.block.entity.EagleNestBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

// Eagle Egg. Two uses:
//   1. Right-click a nest block while holding this → add the egg to the nest
//      (capped at the nest's max-egg count). Starts an incubation timer.
//   2. Otherwise inert — broken-nest eggs survive in inventory but lose
//      their incubation progress (they're fresh again when placed).
public class EagleEggItem extends Item {

    public EagleEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof EagleNestBlockEntity nest)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (!nest.hasEggSpace()) {
            /* TODO(port-26.1) ServerPlayer.sendSystemMessage */ if (player instanceof net.minecraft.server.level.ServerPlayer __sp) __sp.sendSystemMessage(Component.literal("This nest is already full."), true);
            return InteractionResult.CONSUME;
        }
        nest.addEgg(context.getLevel().getGameTime());
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
