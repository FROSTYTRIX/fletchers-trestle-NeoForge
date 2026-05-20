package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

// Phase A — Eagle Whistle. Owner-side remote control for tamed eagles so the
// player isn't forced to walk over to the eagle to toggle fetch mode.
//
//   - Right-click an eagle               → bind the whistle to that eagle (UUID
//                                          stored as a data component). When
//                                          bound, all other interactions only
//                                          affect the bound eagle.
//   - Sneak + right-click an eagle       → unbind (whistle returns to "all
//                                          owned eagles in range" mode).
//   - Right-click in air                 → toggle fetch mode on the targeted
//                                          eagles. The first eagle's current
//                                          mode is flipped and all matched
//                                          eagles are set to that new value.
//   - Sneak + right-click in air         → recall. Clears any active hunt and
//                                          paths each eagle toward the player.
public class EagleWhistleItem extends Item {

    // Radius around the player searched for owned eagles when no specific
    // eagle is bound to the whistle.
    private static final double SEARCH_RADIUS = 64.0;

    public EagleWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            List<EagleEntity> targets = findTargets(stack, player);
            if (targets.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("No eagles in range."), true);
            } else if (player.isShiftKeyDown()) {
                recall(targets, player);
            } else {
                toggleFetchMode(targets, player);
            }
            // Audible cue regardless — gives feedback even when "no eagles in range".
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS,
                    0.8f, 1.4f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (!(target instanceof EagleEntity eagle) || !eagle.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            if (player.isShiftKeyDown()) {
                stack.remove(ModDataComponents.BOUND_EAGLE.get());
                player.displayClientMessage(
                        Component.literal("Whistle unbound."), true);
            } else {
                stack.set(ModDataComponents.BOUND_EAGLE.get(), eagle.getUUID());
                player.displayClientMessage(
                        Component.literal("Whistle bound to this eagle."), true);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS,
                    0.6f, 1.6f);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    // Returns the eagles the whistle should affect for this interaction:
    //   - If bound: at most one eagle (the bound one, if present in the search radius)
    //   - Else:     all eagles owned by the player within SEARCH_RADIUS
    private List<EagleEntity> findTargets(ItemStack stack, Player player) {
        UUID bound = stack.get(ModDataComponents.BOUND_EAGLE.get());
        return player.level().getEntitiesOfClass(
                EagleEntity.class,
                player.getBoundingBox().inflate(SEARCH_RADIUS),
                e -> e.isOwnedBy(player)
                        && (bound == null || e.getUUID().equals(bound)));
    }

    private static void toggleFetchMode(List<EagleEntity> targets, Player player) {
        // Use the first matched eagle's mode as the reference. All targets
        // get flipped to the opposite — keeps the squad in sync rather than
        // each eagle drifting to its own state.
        boolean newMode = !targets.get(0).isFetchModeEnabled();
        for (EagleEntity e : targets) e.setFetchModeEnabled(newMode);
        player.displayClientMessage(
                Component.literal("Eagles: fetch " + (newMode ? "ON" : "OFF")), true);
    }

    private static void recall(List<EagleEntity> targets, Player player) {
        for (EagleEntity e : targets) {
            e.setHuntTarget(null);
            // Path a touch above the player so the eagle approaches from
            // above rather than colliding at body level.
            e.getNavigation().moveTo(player.getX(), player.getY() + 1.5, player.getZ(), 1.4);
        }
        player.displayClientMessage(
                Component.literal("Eagles recalled."), true);
    }
}
