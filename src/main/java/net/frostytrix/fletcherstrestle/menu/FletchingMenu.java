package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.recipe.FletchingRecipeInput;
import net.frostytrix.fletcherstrestle.recipe.ModRecipes;
import net.frostytrix.fletcherstrestle.tags.ModTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FletchingMenu extends AbstractContainerMenu {
    public int activeTab = 0;
    public float customTuning = -1.0f;

    private final Player player;
    private final Level level;

    public final Container craftSlots = new SimpleContainer(7) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };

    public Container getContainer() {
        return this.craftSlots;
    }

    public final ResultContainer resultSlots = new ResultContainer();

    public FletchingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(7));
    }

    public FletchingMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenuTypes.FLETCHING_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.level = playerInventory.player.level();

        // ==========================================
        // OUTPUT SLOT (Index 0 in menu.slots)
        // ==========================================
        this.addSlot(new Slot(this.resultSlots, 0, 124, 35) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }

            @Override
            public void onTake(Player playerIn, ItemStack stack) {
                FletchingMenu.this.shrinkInputs();
                FletchingMenu.this.customTuning = -1.0f; // Reset tuning for next craft
                super.onTake(playerIn, stack);
            }
        });

        // ==========================================
        // TAB 0: BOW SLOTS
        // ==========================================
        this.addSlot(new Slot(craftSlots, 0, 45, 17) { // Top Limb
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.is(ModTags.Items.BOW_LIMBS); }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 0; }
        });
        this.addSlot(new Slot(craftSlots, 1, 45, 53) { // Bottom Limb
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.is(ModTags.Items.BOW_LIMBS); }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 0; }
        });
        this.addSlot(new Slot(craftSlots, 2, 21, 35) { // Riser
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.is(ModTags.Items.BOW_RISERS); }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 0; }
        });
        this.addSlot(new Slot(craftSlots, 3, 69, 35) { // String
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return stack.is(ModTags.Items.BOW_STRINGS); }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 0; }
        });

        // ==========================================
        // TAB 1: ARROW SLOTS
        // ==========================================
        this.addSlot(new Slot(craftSlots, 4, 66, 17) { // Arrow Head
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return getArrowHead(stack) != null; }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 1; }
        });
        this.addSlot(new Slot(craftSlots, 5, 48, 35) { // Arrow Shaft
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return getArrowShaft(stack) != null; }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 1; }
        });
        this.addSlot(new Slot(craftSlots, 6, 30, 53) { // Arrow Fletching
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return getArrowFletching(stack) != null; }
            @Override public boolean isActive() { return FletchingMenu.this.activeTab == 1; }
        });

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);

        // Removed the early client-side return so the UI updates instantly!
        if (this.activeTab == 0) {
            FletchingRecipeInput input = new FletchingRecipeInput(
                    this.craftSlots.getItem(2), // Riser
                    this.craftSlots.getItem(0), // Top Limb
                    this.craftSlots.getItem(1), // Bottom Limb
                    this.craftSlots.getItem(3)  // String
            );

            var recipeHolder = this.level.getRecipeManager().getRecipeFor(ModRecipes.MODULAR_WEAPON_TYPE.get(), input, this.level);

            if (recipeHolder.isPresent()) {
                ItemStack output = recipeHolder.get().value().assemble(input, this.level.registryAccess());

                // INJECT CUSTOM TUNING IF APPLICABLE
                if (this.customTuning != -1.0f) {
                    var base = output.get(ModDataComponents.BOW_ASSEMBLY.get());
                    if (base != null) {
                        // FIXED ORDER: Limbs first, then Riser!
                        output.set(ModDataComponents.BOW_ASSEMBLY.get(), new BowAssembly(
                                base.limbMaterial(), base.riserMaterial(), base.stringMaterial(), this.customTuning
                        ));
                    }
                }

                this.resultSlots.setItem(0, output);
            } else {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }
        } else if (this.activeTab == 1) {
            ItemStack head = this.craftSlots.getItem(4);
            ItemStack shaft = this.craftSlots.getItem(5);
            ItemStack fletch = this.craftSlots.getItem(6);

            if (!head.isEmpty() && !shaft.isEmpty() && !fletch.isEmpty()) {
                String headName = getArrowHead(head);
                String shaftName = getArrowShaft(shaft);
                String fletchName = getArrowFletching(fletch);

                if (headName != null && shaftName != null && fletchName != null) {
                    ItemStack output = new ItemStack(ModItems.MODULAR_ARROW.get(), 4);
                    ArrowAssembly assembly = new ArrowAssembly(headName, shaftName, fletchName);
                    output.set(ModDataComponents.ARROW_ASSEMBLY.get(), assembly);
                    this.resultSlots.setItem(0, output);
                } else {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                }
            } else {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }
        }

        // Tells the client to refresh the button state
        this.broadcastChanges();
    }

    public void shrinkInputs() {
        int startSlot = activeTab == 0 ? 0 : 4;
        int endSlot = activeTab == 0 ? 4 : 7;
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack stack = this.craftSlots.getItem(i);
            if (!stack.isEmpty()) stack.shrink(1);
        }
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
    public boolean stillValid(@NotNull Player player) { return true; }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Your exact original Shift-Click logic
            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 8, 44, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index >= 1 && index < 8) {
                if (!this.moveItemStackTo(itemstack1, 8, 44, false)) return ItemStack.EMPTY;
            } else if (index >= 8 && index < 44) {
                if (this.activeTab == 0) {
                    if (itemstack1.is(ModTags.Items.BOW_LIMBS)) {
                        if (!this.moveItemStackTo(itemstack1, 1, 3, false)) return ItemStack.EMPTY;
                    } else if (itemstack1.is(ModTags.Items.BOW_RISERS)) {
                        if (!this.moveItemStackTo(itemstack1, 3, 4, false)) return ItemStack.EMPTY;
                    } else if (itemstack1.is(ModTags.Items.BOW_STRINGS)) {
                        if (!this.moveItemStackTo(itemstack1, 4, 5, false)) return ItemStack.EMPTY;
                    } else if (index >= 8 && index < 35) {
                        if (!this.moveItemStackTo(itemstack1, 35, 44, false)) return ItemStack.EMPTY;
                    } else if (index >= 35 && index < 44) {
                        if (!this.moveItemStackTo(itemstack1, 8, 35, false)) return ItemStack.EMPTY;
                    }
                } else if (this.activeTab == 1) {
                    if (getArrowHead(itemstack1) != null) {
                        if (!this.moveItemStackTo(itemstack1, 5, 6, false)) return ItemStack.EMPTY;
                    } else if (getArrowShaft(itemstack1) != null) {
                        if (!this.moveItemStackTo(itemstack1, 6, 7, false)) return ItemStack.EMPTY;
                    } else if (getArrowFletching(itemstack1) != null) {
                        if (!this.moveItemStackTo(itemstack1, 7, 8, false)) return ItemStack.EMPTY;
                    } else if (index >= 8 && index < 35) {
                        if (!this.moveItemStackTo(itemstack1, 35, 44, false)) return ItemStack.EMPTY;
                    } else if (index >= 35 && index < 44) {
                        if (!this.moveItemStackTo(itemstack1, 8, 35, false)) return ItemStack.EMPTY;
                    }
                }
            }
            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }
}