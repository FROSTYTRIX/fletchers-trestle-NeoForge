package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.component.ModDataComponents;
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
    private final int maxSlots;

    public QuiverMenu(int id, Inventory playerInv) {
        super(ModMenuTypes.QUIVER_MENU.get(), id);
        this.player = playerInv.player;

        // 1. Determine which hand holds the quiver
        if (player.getMainHandItem().getItem() instanceof ModularQuiverItem) {
            this.hand = InteractionHand.MAIN_HAND;
        } else {
            this.hand = InteractionHand.OFF_HAND;
        }

        ItemStack quiver = player.getItemInHand(hand);

        // 2. Fetch the dynamic capacity
        this.maxSlots = quiver.getOrDefault(ModDataComponents.MAX_QUIVER_SLOTS.get(), 9);

        // 3. Initialize the dynamic container
        this.quiverContainer = new SimpleContainer(this.maxSlots) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveToItem();
            }
        };

        // 4. Load items from the Quiver into the Container
        List<ItemStack> list = ModularQuiverItem.getQuiverContents(quiver);
        for (int i = 0; i < this.maxSlots; i++) {
            if (i < list.size()) {
                this.quiverContainer.setItem(i, list.get(i).copy());
            }
        }

        // --- SLOT LAYOUT MATH ---

        int rows = (int) Math.ceil((double) this.maxSlots / 9.0);

        // 5. Add Dynamic Quiver Slots
        for (int i = 0; i < this.maxSlots; i++) {
            int row = i / 9;
            int col = i % 9;

            // Determine how many items are in this specific row
            int itemsInRow = Math.min(9, this.maxSlots - (row * 9));

            // A full row of 9 slots is 162 pixels wide (9 * 18).
            // Subtract the actual width of the items in this row, divide by 2 to get the left offset.
            int centeringOffset = (162 - (itemsInRow * 18)) / 2;

            // Apply the offset to the X coordinate
            this.addSlot(new Slot(quiverContainer, i, 8 + centeringOffset + (col * 18), 18 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ArrowItem;
                }
            });
        }

        // 6. Add Player Inventory (Dynamically shifted down!)
        int playerInvY = 31 + (rows * 18);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, playerInvY + i * 18));
            }
        }

        // 7. Add Player Hotbar
        int hotbarY = playerInvY + 58;
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, hotbarY));
        }
    }

    private void saveToItem() {
        ItemStack quiver = player.getItemInHand(hand);
        if (quiver.getItem() instanceof ModularQuiverItem) {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < this.maxSlots; i++) {
                list.add(this.quiverContainer.getItem(i).copy());
            }
            ModularQuiverItem.saveQuiverContents(quiver, list);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // Menu closes automatically if the player drops or moves the quiver
        return player.getItemInHand(hand).getItem() instanceof ModularQuiverItem;
    }

    // --- SHIFT-CLICK LOGIC ---
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Total slots in player inventory = 36
            int containerSize = this.maxSlots;

            // If shift-clicking from Quiver -> Player Inventory
            if (index < containerSize) {
                if (!this.moveItemStackTo(itemstack1, containerSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If shift-clicking from Player Inventory -> Quiver
            else {
                // Only allow arrows to be shift-clicked into the Quiver
                if (itemstack1.getItem() instanceof ArrowItem) {
                    if (!this.moveItemStackTo(itemstack1, 0, containerSize, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // If it's not an arrow, just move it between hotbar and main inventory
                    if (index < containerSize + 27) {
                        if (!this.moveItemStackTo(itemstack1, containerSize + 27, this.slots.size(), false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= containerSize + 27 && index < this.slots.size()) {
                        if (!this.moveItemStackTo(itemstack1, containerSize, containerSize + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
}