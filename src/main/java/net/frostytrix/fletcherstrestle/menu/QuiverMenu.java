package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QuiverMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final Container quiverContainer;

    public QuiverMenu(int id, Inventory playerInv) {
        super(ModMenuTypes.QUIVER_MENU.get(), id);
        this.player = playerInv.player;

        if (player.getMainHandItem().getItem() instanceof ModularQuiverItem) {
            this.hand = InteractionHand.MAIN_HAND;
        } else {
            this.hand = InteractionHand.OFF_HAND;
        }

        this.quiverContainer = new SimpleContainer(9) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveToItem();
            }
        };

        // Load items from the Quiver into the Container
        ItemStack quiver = player.getItemInHand(hand);
        List<ItemStack> list = ModularQuiverItem.getQuiverContents(quiver);
        for (int i = 0; i < 9; i++) {
            this.quiverContainer.setItem(i, list.get(i).copy());
        }

        // Add the 9 Quiver Slots (1 Row)
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(quiverContainer, i, 8 + i * 18, 18) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ArrowItem; }
            });
        }

        // Add Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 49 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, 8 + k * 18, 107));
        }
    }

    private void saveToItem() {
        ItemStack quiver = player.getItemInHand(hand);
        if (quiver.getItem() instanceof ModularQuiverItem) {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < 9; i++) list.add(this.quiverContainer.getItem(i).copy());
            ModularQuiverItem.saveQuiverContents(quiver, list);
        }
    }

    @Override
    public boolean stillValid(Player player) { return player.getItemInHand(hand).getItem() instanceof ModularQuiverItem; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            itemstack = slotItem.copy();
            if (index < 9) { // From Quiver to Inventory
                if (!this.moveItemStackTo(slotItem, 9, 45, true)) return ItemStack.EMPTY;
            } else if (slotItem.getItem() instanceof ArrowItem) { // From Inventory to Quiver
                if (!this.moveItemStackTo(slotItem, 0, 9, false)) return ItemStack.EMPTY;
            } else { return ItemStack.EMPTY; }
            if (slotItem.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return itemstack;
    }
}