package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.recipe.ArrowRecipeInput;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FletchingMenu extends AbstractContainerMenu {
    public int activeTab = 0;
    public float customTuning = -1.0f; // Tracks the minigame score!

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
        // OUTPUT SLOT (Index 0)
        // ==========================================
        this.addSlot(new Slot(this.resultSlots, 0, 124, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            // This fixes the vanishing bow! Items are ONLY consumed when you actually pick up the result.
            @Override
            public void onTake(Player playerIn, ItemStack stack) {
                FletchingMenu.this.shrinkInputs();
                FletchingMenu.this.customTuning = -1.0f; // Reset tuning for the next craft
                super.onTake(playerIn, stack);
            }
        });

        // ==========================================
        // TAB 0: BOW SLOTS (Indices 0, 1, 2, 3)
        // ==========================================
        this.addSlot(new Slot(craftSlots, 0, 45, 17) { // Top Limb
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.BOW_LIMBS);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 0;
            }
        });
        this.addSlot(new Slot(craftSlots, 1, 45, 53) { // Bottom Limb
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.BOW_LIMBS);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 0;
            }
        });
        this.addSlot(new Slot(craftSlots, 2, 21, 35) { // Riser
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.BOW_RISERS);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 0;
            }
        });
        this.addSlot(new Slot(craftSlots, 3, 69, 35) { // String
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.BOW_STRINGS);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 0;
            }
        });

        // ==========================================
        // TAB 1: ARROW SLOTS (Indices 4, 5, 6)
        // ==========================================
        this.addSlot(new Slot(craftSlots, 4, 66, 17) { // Arrow Head
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.ARROW_HEADS);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 1;
            }
        });
        this.addSlot(new Slot(craftSlots, 5, 48, 35) { // Arrow Shaft
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.ROUGH_LIMBS);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 1;
            }
        });
        this.addSlot(new Slot(craftSlots, 6, 30, 53) { // Arrow Fletching
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModTags.Items.ARROW_FLETCHING);
            }

            @Override
            public boolean isActive() {
                return FletchingMenu.this.activeTab == 1;
            }
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

        if (this.activeTab == 0) {
            // Check Bow Recipe
            FletchingRecipeInput input = new FletchingRecipeInput(
                    this.craftSlots.getItem(2), // Riser
                    this.craftSlots.getItem(0), // Top Limb
                    this.craftSlots.getItem(1), // Bottom Limb
                    this.craftSlots.getItem(3)  // String
            );

            var recipeHolder = this.level.getRecipeManager().getRecipeFor(ModRecipes.MODULAR_WEAPON_TYPE.get(), input, this.level);

            if (recipeHolder.isPresent()) {
                ItemStack output = recipeHolder.get().value().assemble(input, this.level.registryAccess());

                // INJECT CUSTOM TUNING (Order fixed: limbs, riser, string)
                if (this.customTuning != -1.0f) {
                    var base = output.get(ModDataComponents.BOW_ASSEMBLY.get());
                    if (base != null) {
                        // withTuning keeps every other part, including a
                        // composite's second wood.
                        output.set(ModDataComponents.BOW_ASSEMBLY.get(), base.withTuning(this.customTuning));
                    }
                }
                this.resultSlots.setItem(0, output);
            } else {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }

        } else if (this.activeTab == 1) {
            // Check Arrow Recipe
            ArrowRecipeInput arrowInput = new ArrowRecipeInput(
                    this.craftSlots.getItem(4), // Head
                    this.craftSlots.getItem(5), // Shaft
                    this.craftSlots.getItem(6)  // Fletching
            );

            var arrowRecipeHolder = this.level.getRecipeManager().getRecipeFor(ModRecipes.MODULAR_ARROW_TYPE.get(), arrowInput, this.level);

            if (arrowRecipeHolder.isPresent()) {
                ItemStack output = arrowRecipeHolder.get().value().assemble(arrowInput, this.level.registryAccess());
                output.setCount(4);
                this.resultSlots.setItem(0, output);
            } else {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }
        }

        // Tells the client UI to refresh the assemble button state
        this.broadcastChanges();
    }

    public void shrinkInputs() {
        int startSlot = activeTab == 0 ? 0 : 4;
        int endSlot = activeTab == 0 ? 4 : 7;
        for (int i = startSlot; i < endSlot; i++) {
            ItemStack stack = this.craftSlots.getItem(i);
            if (!stack.isEmpty()) stack.shrink(1);
        }
        this.slotsChanged(this.craftSlots);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.resultSlots.clearContent(); // Prevents ghost items dropping
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
                    // Route shift-clicked parts to the matching arrow slot by tag.
                    if (itemstack1.is(ModTags.Items.ARROW_HEADS)) {
                        if (!this.moveItemStackTo(itemstack1, 5, 6, false)) return ItemStack.EMPTY;
                    } else if (itemstack1.is(ModTags.Items.ROUGH_LIMBS)) {
                        if (!this.moveItemStackTo(itemstack1, 6, 7, false)) return ItemStack.EMPTY;
                    } else if (itemstack1.is(ModTags.Items.ARROW_FLETCHING)) {
                        if (!this.moveItemStackTo(itemstack1, 7, 8, false)) return ItemStack.EMPTY;
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