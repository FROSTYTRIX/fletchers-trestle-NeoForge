package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FletchingMenu extends AbstractContainerMenu {
    public int activeTab = 0;

    private final Player player;
    private final Level level;

    // Expanded to 7 slots! (0-3 for Bow, 4-6 for Arrow)
    private final Container craftSlots = new SimpleContainer(7) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public net.minecraft.world.Container getContainer() {
        return this.craftSlots;
    }

    private final Container resultSlots = new SimpleContainer(1);

    public FletchingMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.FLETCHING_MENU.get(), containerId);

        this.player = playerInventory.player;
        this.level = playerInventory.player.level();

        // ==========================================
        // TAB 0: BOW SLOTS (Indices 0, 1, 2, 3)
        // ==========================================
        this.addSlot(new Slot(craftSlots, 0, 45, 17){ // Top Limb
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModTags.Items.BOW_LIMBS); }
            @Override public boolean isActive() { return activeTab == 0; } // Only active on Tab 0!
        });
        this.addSlot(new Slot(craftSlots, 1, 45, 53){ // Bottom Limb
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModTags.Items.BOW_LIMBS); }
            @Override public boolean isActive() { return activeTab == 0; }
        });
        this.addSlot(new Slot(craftSlots, 2, 21, 35){ // Riser
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModTags.Items.BOW_RISERS); }
            @Override public boolean isActive() { return activeTab == 0; }
        });
        this.addSlot(new Slot(craftSlots, 3, 69, 35){ // String
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModTags.Items.BOW_STRINGS); }
            @Override public boolean isActive() { return activeTab == 0; }
        });

        // ==========================================
        // TAB 1: ARROW SLOTS (Indices 4, 5, 6)
        // (Arranged Vertically: X=56, Y=17, 35, 53)
        // ==========================================
        this.addSlot(new Slot(craftSlots, 4, 66, 17){ // Arrow Head
            @Override public boolean mayPlace(ItemStack stack) { return getArrowHead(stack) != null; }
            @Override public boolean isActive() { return activeTab == 1; } // Only active on Tab 1!
        });
        this.addSlot(new Slot(craftSlots, 5, 48, 35){ // Arrow Shaft
            @Override public boolean mayPlace(ItemStack stack) { return getArrowShaft(stack) != null; }
            @Override public boolean isActive() { return activeTab == 1; }
        });
        this.addSlot(new Slot(craftSlots, 6, 30, 53){ // Arrow Fletching
            @Override public boolean mayPlace(ItemStack stack) { return getArrowFletching(stack) != null; }
            @Override public boolean isActive() { return activeTab == 1; }
        });

        // ==========================================
        // OUTPUT SLOT
        // ==========================================
        this.addSlot(new Slot(resultSlots, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // Dynamically drain the correct slots based on the tab!
                if (activeTab == 0) {
                    for (int i = 0; i < 4; i++) craftSlots.removeItem(i, 1);
                } else if (activeTab == 1) {
                    for (int i = 4; i < 7; i++) craftSlots.removeItem(i, 1);
                }
                super.onTake(player, stack);
            }
        });

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    public boolean canAssemble() {
        ItemStack top = craftSlots.getItem(0);
        ItemStack bottom = craftSlots.getItem(1);
        ItemStack riser = craftSlots.getItem(2);
        ItemStack string = craftSlots.getItem(3);

        return resultSlots.getItem(0).isEmpty() &&
                !top.isEmpty() && !bottom.isEmpty() &&
                getMaterialName(top).equals(getMaterialName(bottom)) &&
                !riser.isEmpty() && !string.isEmpty();
    }

    public void finalizeBow(float quality) {
        if (canAssemble() && this.activeTab == 0) {
            ItemStack result = new ItemStack(ModItems.MODULAR_BOW.get());
            result.set(ModDataComponents.BOW_ASSEMBLY.get(), new BowAssembly(
                    getMaterialName(craftSlots.getItem(0)),
                    getMaterialName(craftSlots.getItem(2)),
                    getMaterialName(craftSlots.getItem(3)),
                    quality
            ));
            resultSlots.setItem(0, result);
        }
    }

    public void updateCraftingResult() {
        if (this.level.isClientSide) return;

        if (this.activeTab == 0) {
            if (!canAssemble() && !resultSlots.getItem(0).isEmpty()) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                this.broadcastChanges();
            }
        } else if (this.activeTab == 1) {
            // Read from the ARROW slots (4, 5, 6)
            ItemStack slot1 = this.craftSlots.getItem(4);
            ItemStack slot2 = this.craftSlots.getItem(5);
            ItemStack slot3 = this.craftSlots.getItem(6);

            if (!slot1.isEmpty() && !slot2.isEmpty() && !slot3.isEmpty()) {
                String head = getArrowHead(slot1);
                String shaft = getArrowShaft(slot2);
                String fletching = getArrowFletching(slot3);

                if (head != null && shaft != null && fletching != null) {
                    ItemStack result = new ItemStack(ModItems.MODULAR_ARROW.get(), 4);
                    result.set(ModDataComponents.ARROW_ASSEMBLY.get(), new ArrowAssembly(head, shaft, fletching));
                    this.resultSlots.setItem(0, result);
                    this.broadcastChanges();
                    return;
                }
            }
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.broadcastChanges();
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.updateCraftingResult();
    }

    // --- Helper Methods ---
    private String getMaterialName(ItemStack stack) {
        String name = stack.getItem().toString();
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

        if (name.contains("wood")) return "Wood";
        if (name.contains("iron")) return "Iron";
        if (name.contains("copper")) return "Copper";

        if (name.contains("string") && !name.contains("high_tension") && !name.contains("flax")) return "Spider";
        if (name.contains("flax")) return "Flax";
        if (name.contains("high_tension")) return "High Tension";

        return "Unknown";
    }

    private String getArrowHead(ItemStack stack) {
        if (stack.is(Items.FLINT)) return "flint";
        if (stack.is(Items.IRON_INGOT)) return "broadhead";
        if (stack.is(Items.COPPER_INGOT)) return "bodkin_point";
        if (stack.is(Items.ECHO_SHARD)) return "resonance_tip";
        if (stack.is(Items.IRON_NUGGET)) return "barbed_tip";
        if (stack.is(Items.GOLD_INGOT)) return "weighted_blunt";
        return null;
    }

    private String getArrowShaft(ItemStack stack) {
        if (stack.is(ModItems.ROUGH_OAK_LIMB.get())) return "oak";
        if (stack.is(Items.STICK)) return "oak";
        if (stack.is(ModItems.ROUGH_SPRUCE_LIMB.get())) return "spruce";
        if (stack.is(ModItems.ROUGH_BIRCH_LIMB.get())) return "birch";
        if (stack.is(ModItems.ROUGH_DARK_OAK_LIMB.get())) return "dark_oak";
        if (stack.is(ModItems.ROUGH_JUNGLE_LIMB.get())) return "jungle";
        if (stack.is(ModItems.ROUGH_ACACIA_LIMB.get())) return "acacia";
        if (stack.is(ModItems.ROUGH_MANGROVE_LIMB.get())) return "mangrove";
        if (stack.is(ModItems.ROUGH_CHERRY_LIMB.get())) return "cherry";
        if (stack.is(ModItems.ROUGH_PALE_OAK_LIMB.get())) return "pale_oak";
        if (stack.is(ModItems.ROUGH_CRIMSON_LIMB.get())) return "crimson";
        if (stack.is(ModItems.ROUGH_WARPED_LIMB.get())) return "warped";
        return null;
    }

    private String getArrowFletching(ItemStack stack) {
        if (stack.is(Items.FEATHER)) return "feather";
        if (stack.is(Items.FLINT)) return "rigid";
        if (stack.is(Items.STRING)) return "trailing";
        if (stack.is(Items.PHANTOM_MEMBRANE)) return "serrated";
        if (stack.is(Items.LEATHER)) return "bound";
        if (stack.is(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE)) return "vex";
        return null;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.craftSlots);
    }

    // --- Shift-Click Routing ---
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Index 7 is Output Slot
            if (index == 7) {
                if (!this.moveItemStackTo(itemstack1, 8, 44, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            }
            // Indices 0-6 are Input Slots
            else if (index >= 0 && index < 7) {
                if (!this.moveItemStackTo(itemstack1, 8, 44, false)) return ItemStack.EMPTY;
            }
            // Player Inventory -> Table
            else if (index >= 8 && index < 44) {
                if (this.activeTab == 0) { // Bow Tab
                    if (itemstack1.is(ModTags.Items.BOW_LIMBS)) {
                        if (!this.moveItemStackTo(itemstack1, 0, 2, false)) return ItemStack.EMPTY;
                    } else if (itemstack1.is(ModTags.Items.BOW_RISERS)) {
                        if (!this.moveItemStackTo(itemstack1, 2, 3, false)) return ItemStack.EMPTY;
                    } else if (itemstack1.is(ModTags.Items.BOW_STRINGS)) {
                        if (!this.moveItemStackTo(itemstack1, 3, 4, false)) return ItemStack.EMPTY;
                    } else if (index >= 8 && index < 35) {
                        if (!this.moveItemStackTo(itemstack1, 35, 44, false)) return ItemStack.EMPTY;
                    } else if (index >= 35 && index < 44) {
                        if (!this.moveItemStackTo(itemstack1, 8, 35, false)) return ItemStack.EMPTY;
                    }
                } else if (this.activeTab == 1) { // Arrow Tab
                    if (getArrowHead(itemstack1) != null) {
                        if (!this.moveItemStackTo(itemstack1, 4, 5, false)) return ItemStack.EMPTY;
                    } else if (getArrowShaft(itemstack1) != null) {
                        if (!this.moveItemStackTo(itemstack1, 5, 6, false)) return ItemStack.EMPTY;
                    } else if (getArrowFletching(itemstack1) != null) {
                        if (!this.moveItemStackTo(itemstack1, 6, 7, false)) return ItemStack.EMPTY;
                    } else if (index >= 8 && index < 35) {
                        if (!this.moveItemStackTo(itemstack1, 35, 44, false)) return ItemStack.EMPTY;
                    } else if (index >= 35 && index < 44) {
                        if (!this.moveItemStackTo(itemstack1, 8, 35, false)) return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }
}