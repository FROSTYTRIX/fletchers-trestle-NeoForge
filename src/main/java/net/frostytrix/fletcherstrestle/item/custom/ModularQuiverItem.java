package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.menu.QuiverMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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


    // --- BUNDLE STYLE: Clicking an item ONTO the Quiver ---
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack quiver, ItemStack carriedStack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        int maxSlots = quiver.getOrDefault(ModDataComponents.MAX_QUIVER_SLOTS.get(), 9);

        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;

        List<ItemStack> list = getQuiverContents(quiver);

        // Extracting from Quiver
        if (carriedStack.isEmpty()) {
            int selected = quiver.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);

            // Safety check in case the selected slot exceeds the list size
            if (selected < list.size() && !list.get(selected).isEmpty()) {
                access.set(list.get(selected).copy());
                list.set(selected, ItemStack.EMPTY);
                saveQuiverContents(quiver, list);
                return true;
            }
        }
        // Inserting into Quiver
        else if (carriedStack.getItem() instanceof ArrowItem) {
            // DYNAMIC UPDATE: Use maxSlots instead of 9
            for (int i = 0; i < maxSlots; i++) {

                // DYNAMIC UPDATE: Ensure the list has enough empty slots to prevent out-of-bounds errors
                if (i >= list.size()) {
                    list.add(ItemStack.EMPTY);
                }

                ItemStack inSlot = list.get(i);
                if (inSlot.isEmpty()) {
                    list.set(i, carriedStack.copy());
                    carriedStack.setCount(0);
                    saveQuiverContents(quiver, list);
                    return true;
                } else if (ItemStack.isSameItemSameComponents(inSlot, carriedStack) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                    int space = inSlot.getMaxStackSize() - inSlot.getCount();
                    int transfer = Math.min(space, carriedStack.getCount());
                    inSlot.grow(transfer);
                    carriedStack.shrink(transfer);
                    saveQuiverContents(quiver, list);
                    if (carriedStack.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    // --- TOOLTIP: Show Selected Arrow ---
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int selected = stack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
        tooltipComponents.add(Component.literal("Selected Slot: " + (selected + 1)).withStyle(ChatFormatting.GOLD));

        List<ItemStack> list = getQuiverContents(stack);
        if (!list.get(selected).isEmpty()) {
            tooltipComponents.add(Component.literal("Loaded: ").withStyle(ChatFormatting.GRAY).append(list.get(selected).getHoverName()));
        } else {
            tooltipComponents.add(Component.literal("Loaded: Empty").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // --- Helpers ---
    public static List<ItemStack> getQuiverContents(ItemStack quiver) {
        ItemContainerContents contents = quiver.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        // 1. Create a NonNullList pre-filled with exactly 9 Empty ItemStacks
        NonNullList<ItemStack> list = NonNullList.withSize(9, ItemStack.EMPTY);

        // 2. Safely copy the contents into our properly sized Minecraft list
        contents.copyInto(list);

        return list;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    // We removed the 'hand' parameter here!
                    (id, inv, p) -> new QuiverMenu(id, inv),
                    Component.literal("Quiver")
            ));
        }
        return InteractionResult.SUCCESS;
    }

    public static void saveQuiverContents(ItemStack quiver, List<ItemStack> list) {
        quiver.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }
}