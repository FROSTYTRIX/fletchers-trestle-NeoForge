package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.block.custom.NailBlock;
import net.frostytrix.fletcherstrestle.block.entity.NailBlockEntity;
import net.frostytrix.fletcherstrestle.component.GarlandColours;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bunting, strung between two {@link NailBlock}s.
 *
 * <p>Right-click one nail to set the near end, then right-click another to hang
 * the garland between them. The pending first end lives on the stack itself, so
 * it survives you wandering off, and can be cleared by right-clicking air.</p>
 */
public class GarlandItem extends Item {

    /** How far apart the two nails may be. Long spans risk one end being unloaded. */
    private static final double MAX_SPAN = 12.0;

    public GarlandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (!(level.getBlockEntity(pos) instanceof NailBlockEntity nail)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        var player = context.getPlayer();

        if (!nail.hasRoom()) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.fletcherstrestle.nail_full").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.CONSUME;
        }

        GlobalPos pending = stack.get(ModDataComponents.GARLAND_ANCHOR.get());

        // First click: remember this nail as the near end.
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            stack.set(ModDataComponents.GARLAND_ANCHOR.get(), new GlobalPos(level.dimension(), pos));
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.fletcherstrestle.garland_start").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.CONSUME;
        }

        BlockPos start = pending.pos();
        if (start.equals(pos)) {
            // Clicked the same nail again: forget the pending end.
            stack.remove(ModDataComponents.GARLAND_ANCHOR.get());
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(start) instanceof NailBlockEntity from) || !from.hasRoom()) {
            stack.remove(ModDataComponents.GARLAND_ANCHOR.get());
            return InteractionResult.CONSUME;
        }
        if (Math.sqrt(start.distSqr(pos)) > MAX_SPAN) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.fletcherstrestle.garland_too_far", (int) MAX_SPAN)
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.CONSUME;
        }

        // Second click: string it up. Store a clean single garland: copying the
        // held stack wholesale would drop the whole stack when the nail breaks,
        // and would keep the anchor component so the dropped item still thought
        // it was tied to a nail.
        ItemStack strung = stack.copyWithCount(1);
        strung.remove(ModDataComponents.GARLAND_ANCHOR.get());
        from.addSpan(pos, strung);
        nail.addIncoming(start);
        stack.remove(ModDataComponents.GARLAND_ANCHOR.get());
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        GarlandColours colours = stack.get(ModDataComponents.GARLAND_COLOURS.get());
        if (colours != null) {
            tooltip.add(Component.translatable("gui.fletcherstrestle.garland_feathers", colours.colours().size())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stack.get(ModDataComponents.GARLAND_ANCHOR.get()) != null) {
            tooltip.add(Component.translatable("gui.fletcherstrestle.garland_anchored")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("gui.fletcherstrestle.garland_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
