package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.registry.ModDataComponents;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FletchingMenu extends AbstractContainerMenu {

    private final Container craftSlots = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    // The output slot
    private final Container resultSlots = new SimpleContainer(1);

    public FletchingMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.FLETCHING_MENU.get(), containerId);

        this.addSlot(new Slot(craftSlots, 0, 45, 17){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.Items.BOW_LIMBS);
            }
        }); // Top Limb
        this.addSlot(new Slot(craftSlots, 1, 45, 53){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.Items.BOW_LIMBS);
            }
        }); // Bottom Limb
        this.addSlot(new Slot(craftSlots, 2, 21, 35){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.Items.BOW_RISERS);
            }
        }); // Riser
        this.addSlot(new Slot(craftSlots, 3, 69, 35){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.Items.BOW_STRINGS);
            }
        }); // String

        // Output Slot
        this.addSlot(new Slot(resultSlots, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                for (int i = 0; i < 4; i++) {
                    craftSlots.removeItem(i, 1);
                }
                super.onTake(player, stack);
            }
        });

        // 2. Add the Player's Inventory (Standard math for standard Minecraft GUI)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        // 3. Add the Player's Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // We'll add distance checking later if needed
    }

    // 1. Checks if the items are correct and limbs match
    public boolean canAssemble() {
        ItemStack top = craftSlots.getItem(0);
        ItemStack bottom = craftSlots.getItem(1);
        ItemStack riser = craftSlots.getItem(2);
        ItemStack string = craftSlots.getItem(3);

        boolean isOutputEmpty = resultSlots.getItem(0).isEmpty();

        return isOutputEmpty &&
                !top.isEmpty() && !bottom.isEmpty() &&
                getMaterialName(top).equals(getMaterialName(bottom)) &&
                !riser.isEmpty() && !string.isEmpty();
    }

    // 2. The method called by our Networking Packet to physically create the item
    public void finalizeBow(float quality) {
        if (canAssemble()) {
            ItemStack result = new ItemStack(ModItems.MODULAR_BOW.get());

            // Create the component using the exact items in the slots
            result.set(ModDataComponents.BOW_ASSEMBLY.get(), new BowAssembly(
                    getMaterialName(craftSlots.getItem(0)),
                    getMaterialName(craftSlots.getItem(2)),
                    getMaterialName(craftSlots.getItem(3)),
                    quality
            ));

            // Plop it into the output slot!
            resultSlots.setItem(0, result);
        }
    }

    // 3. Runs every time an item is moved in the GUI
    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        // If the recipe is no longer valid, but there's a bow in the output, clear the output!
        // This prevents duplication glitches.
        if (!canAssemble() && !resultSlots.getItem(0).isEmpty()) {
            resultSlots.setItem(0, ItemStack.EMPTY);
        }
    }

    // Helper method to turn an item into a String for our component
    private String getMaterialName(ItemStack stack) {
        String name = stack.getItem().toString();

        // Limbs

        if (name.contains("oak") && !name.contains("dark_oak") && !name.contains("pale_oak")) return "Oak";
        if (name.contains("spruce")) return "Spruce";
        if (name.contains("birch")) return "Birch";
        if (name.contains("jungle")) return "Jungle";
        if (name.contains("acacia")) return "Acacia";
        if (name.contains("dark_oak")) return "Dark Oak";
        if (name.contains("mangrove")) return "Mangrove";
        if (name.contains("cherry")) return "Cherry";
        if (name.contains("pale_oak")) return "Pale Oak";
        if (name.contains("crimson")) return "Crimson";
        if (name.contains("warped")) return "Warped";

        // Risers
        if (name.contains("wood")) return "Wood";
        if (name.contains("iron")) return "Iron";
        if (name.contains("copper")) return "Copper";

        // Strings

        if (name.contains("string") && !name.contains("high_tension") && !name.contains("flax")) return "Spider";
        if (name.contains("flax")) return "Flax";
        if (name.contains("high_tension")) return "High Tension";

        return "Unknown";
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Pop the items out when the menu closes
        this.clearContainer(player, this.craftSlots);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // 1. If clicking the OUTPUT slot (index 4)
            if (index == 4) {
                // Move to player inventory/hotbar
                if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            }
            // 2. If clicking an INPUT slot (indices 0-3)
            else if (index >= 0 && index < 4) {
                // Move to player inventory/hotbar
                if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 3. If clicking the PLAYER INVENTORY or HOTBAR (indices 5-40)
            else {
                // Check if it's a Limb, Riser, or String using our Tags
                if (itemstack1.is(ModTags.Items.BOW_LIMBS)) {
                    // Try slot 0 (Top Limb), then slot 1 (Bottom Limb)
                    if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.is(ModTags.Items.BOW_RISERS)) {
                    // Try slot 2 (Riser)
                    if (!this.moveItemStackTo(itemstack1, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.is(ModTags.Items.BOW_STRINGS)) {
                    // Try slot 3 (String)
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // 4. If it's none of the above, move between Hotbar and Main Inventory
                else if (index >= 5 && index < 32) {
                    if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 32 && index < 41) {
                    if (!this.moveItemStackTo(itemstack1, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
