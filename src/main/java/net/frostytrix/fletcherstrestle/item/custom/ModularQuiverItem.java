package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.menu.QuiverMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

public class ModularQuiverItem extends Item {
    public ModularQuiverItem(Properties properties) {
        super(properties.stacksTo(1)); // Quivers shouldn't stack
    }


    @Override
    public boolean overrideOtherStackedOnMe(ItemStack quiver, ItemStack carriedStack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;

        List<ItemStack> list = getQuiverContents(quiver);

        if (carriedStack.isEmpty()) {
            int selected = quiver.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);

            if (selected < list.size() && !list.get(selected).isEmpty()) {
                access.set(list.get(selected).copy());
                list.set(selected, ItemStack.EMPTY);
                saveQuiverContents(quiver, list);
                return true;
            }
        }
        else if (carriedStack.getItem() instanceof ArrowItem) {
            return insert(quiver, carriedStack);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int selected = stack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
        tooltipComponents.add(Component.translatable("gui.fletcherstrestle.selected_slot", selected + 1).withStyle(ChatFormatting.GOLD));

        List<ItemStack> list = getQuiverContents(stack);
        if (!list.get(selected).isEmpty()) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.loaded", list.get(selected).getHoverName()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.loaded",
                    Component.translatable("gui.fletcherstrestle.empty")).withStyle(ChatFormatting.DARK_GRAY));
        }
    }


    /**
     * Moves as many of {@code arrows} into the quiver as fit, topping up matching
     * stacks and filling empty slots. Shrinks {@code arrows}; true if any moved.
     */
    public static boolean insert(ItemStack quiver, ItemStack arrows) {
        if (!(arrows.getItem() instanceof ArrowItem)) return false;
        int maxSlots = quiver.getOrDefault(ModDataComponents.MAX_QUIVER_SLOTS.get(), 9);
        List<ItemStack> list = getQuiverContents(quiver);
        int before = arrows.getCount();
        for (int i = 0; i < Math.min(maxSlots, list.size()) && !arrows.isEmpty(); i++) {
            ItemStack inSlot = list.get(i);
            if (inSlot.isEmpty()) {
                list.set(i, arrows.copy());
                arrows.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(inSlot, arrows) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                int transfer = Math.min(inSlot.getMaxStackSize() - inSlot.getCount(), arrows.getCount());
                inSlot.grow(transfer);
                arrows.shrink(transfer);
            }
        }
        if (arrows.getCount() == before) return false;
        saveQuiverContents(quiver, list);
        return true;
    }

    /**
     * Takes one arrow from the selected slot, moving the selection on to the next
     * loaded slot when that one is empty. Empty if the quiver holds no arrows.
     */
    public static ItemStack takeOne(ItemStack quiver) {
        int maxSlots = quiver.getOrDefault(ModDataComponents.MAX_QUIVER_SLOTS.get(), 9);
        List<ItemStack> list = getQuiverContents(quiver);
        int slots = Math.min(maxSlots, list.size());
        int selected = quiver.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
        for (int step = 0; step < slots; step++) {
            int slot = Math.floorMod(selected + step, slots);
            if (!list.get(slot).isEmpty()) {
                ItemStack one = list.get(slot).split(1);
                saveQuiverContents(quiver, list);
                if (slot != selected) {
                    quiver.set(ModDataComponents.QUIVER_SELECTED_SLOT.get(), slot);
                }
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Whether any slot holds arrows. */
    public static boolean hasArrows(ItemStack quiver) {
        for (ItemStack stack : getQuiverContents(quiver)) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    /** The arrows in the selected slot, or an empty stack. Drawn sticking out of the quiver. */
    public static ItemStack selectedArrows(ItemStack quiver) {
        List<ItemStack> contents = getQuiverContents(quiver);
        int selected = quiver.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
        return selected >= 0 && selected < contents.size() ? contents.get(selected) : ItemStack.EMPTY;
    }
    public static List<ItemStack> getQuiverContents(ItemStack quiver) {
        ItemContainerContents contents = quiver.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> list = NonNullList.withSize(9, ItemStack.EMPTY);
        contents.copyInto(list);
        return list;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new QuiverMenu(id, inv),
                    Component.translatable("gui.fletcherstrestle.quiver")
            ));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void saveQuiverContents(ItemStack quiver, List<ItemStack> list) {
        quiver.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }
}