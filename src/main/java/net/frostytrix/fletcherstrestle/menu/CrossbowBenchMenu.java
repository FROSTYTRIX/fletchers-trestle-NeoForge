package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.attachment.CrossbowAttachmentDef;
import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CrossbowBenchMenu extends AbstractContainerMenu {

    // Container (block-entity inventory) indices.
    public static final int SLOT_INPUT = 0;       // bow / crossbow
    public static final int SLOT_ATTACHMENT = 1;
    public static final int SLOT_TRIGGER = 2;
    private static final int FUNCTIONAL_SLOTS = 3;

    // Menu-slot indices (order the slots are added below).
    private static final int MENU_INPUT = 0;
    private static final int MENU_TRIGGER = 1;
    private static final int MENU_ATTACHMENT = 2;

    private final Container container;
    private final ContainerLevelAccess access;
    private final Registry<CrossbowAttachmentDef> attachments;

    private boolean mutating = false;
    // Previous-state snapshot — lets us tell "a crossbow was just placed"
    // (unpack) apart from "the trigger was just removed" (disassemble), which
    // produce identical slot contents.
    private boolean lastInputCrossbow = false;
    private boolean lastTriggerPresent = false;

    private final ContainerListener listener = c -> this.slotsChanged(c);

    /** Client-side constructor (MenuType factory). */
    public CrossbowBenchMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(FUNCTIONAL_SLOTS), ContainerLevelAccess.NULL);
    }

    /** Server-side constructor — {@code container} is the block entity's inventory. */
    public CrossbowBenchMenu(int id, Inventory playerInv, Container container, ContainerLevelAccess access) {
        super(ModMenuTypes.CROSSBOW_BENCH_MENU.get(), id);
        checkContainerSize(container, FUNCTIONAL_SLOTS);
        this.container = container;
        this.access = access;
        this.attachments = playerInv.player.level().registryAccess()
                .registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT);

        // Input slot — bow or crossbow. Taking a finished crossbow consumes the
        // fitted-part representations (they are baked into the crossbow).
        this.addSlot(new Slot(container, SLOT_INPUT, 81, 23) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(ModItems.MODULAR_BOW.get()) || s.is(ModItems.MODULAR_CROSSBOW.get());
            }

            @Override
            public void onTake(Player p, ItemStack s) {
                if (s.is(ModItems.MODULAR_CROSSBOW.get())) {
                    CrossbowBenchMenu.this.consumeFittedParts();
                }
                super.onTake(p, s);
            }
        });
        // Trigger slot — exactly one mechanical trigger.
        this.addSlot(new Slot(container, SLOT_TRIGGER, 64, 46) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(ModItems.MECHANICAL_TRIGGER.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        // Attachment slot — one item that some attachment def accepts.
        this.addSlot(new Slot(container, SLOT_ATTACHMENT, 98, 46) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return CrossbowBenchMenu.this.isAttachmentItem(s);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 58));
        }

        // Seed the diff snapshot from current (possibly reloaded) contents so
        // the first interaction after opening compares against the real state.
        this.lastInputCrossbow = container.getItem(SLOT_INPUT).is(ModItems.MODULAR_CROSSBOW.get());
        this.lastTriggerPresent = container.getItem(SLOT_TRIGGER).is(ModItems.MECHANICAL_TRIGGER.get());

        if (container instanceof SimpleContainer sc) {
            sc.addListener(this.listener);
        }
    }

    @Override
    public void slotsChanged(Container c) {
        super.slotsChanged(c);
        if (this.mutating) {
            return;
        }
        // access is NULL on the client menu, so this only mutates server-side.
        this.access.execute((level, pos) -> {
            this.mutating = true;
            try {
                this.updateBench();
            } finally {
                this.mutating = false;
            }
        });
    }

    /** Keeps the input item consistent with the trigger/attachment slots. */
    private void updateBench() {
        ItemStack input = this.container.getItem(SLOT_INPUT);
        ItemStack attachment = this.container.getItem(SLOT_ATTACHMENT);
        ItemStack trigger = this.container.getItem(SLOT_TRIGGER);

        boolean inputCrossbow = input.is(ModItems.MODULAR_CROSSBOW.get());
        boolean inputBow = input.is(ModItems.MODULAR_BOW.get());
        boolean triggerPresent = trigger.is(ModItems.MECHANICAL_TRIGGER.get());

        if (inputCrossbow && !this.lastInputCrossbow && !triggerPresent) {
            // A finished crossbow was just placed -> unpack its parts into slots.
            this.container.setItem(SLOT_TRIGGER, new ItemStack(ModItems.MECHANICAL_TRIGGER.get()));
            ResourceLocation att = input.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
            if (att != null && attachment.isEmpty()) {
                ItemStack attItem = attachmentItemFor(att);
                if (!attItem.isEmpty()) {
                    this.container.setItem(SLOT_ATTACHMENT, attItem);
                }
            }
        } else if (inputBow && triggerPresent && !this.lastTriggerPresent) {
            // Trigger added to a bow -> assemble into a crossbow (carry assembly).
            this.container.setItem(SLOT_INPUT, withAssemblyOf(input, ModItems.MODULAR_CROSSBOW.get()));
        } else if (inputCrossbow && !triggerPresent && this.lastTriggerPresent) {
            // Trigger pulled out of a crossbow -> revert to a bow. Any attachment
            // can't stay on a bow; it's left as a real item in the slot.
            this.container.setItem(SLOT_INPUT, withAssemblyOf(input, ModItems.MODULAR_BOW.get()));
        }

        // Mirror the attachment slot onto the current crossbow's component.
        ItemStack current = this.container.getItem(SLOT_INPUT);
        if (current.is(ModItems.MODULAR_CROSSBOW.get())) {
            ItemStack att = this.container.getItem(SLOT_ATTACHMENT);
            if (!att.isEmpty()) {
                ResourceLocation id = findAttachmentId(att);
                if (id != null) {
                    current.set(ModDataComponents.CROSSBOW_ATTACHMENT.get(), id);
                }
            } else {
                current.remove(ModDataComponents.CROSSBOW_ATTACHMENT.get());
            }
        }

        this.lastInputCrossbow = this.container.getItem(SLOT_INPUT).is(ModItems.MODULAR_CROSSBOW.get());
        this.lastTriggerPresent = this.container.getItem(SLOT_TRIGGER).is(ModItems.MECHANICAL_TRIGGER.get());
    }

    private static ItemStack withAssemblyOf(ItemStack source, net.minecraft.world.item.Item resultItem) {
        ItemStack out = new ItemStack(resultItem);
        var assembly = source.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            out.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);
        }
        return out;
    }

    /** Clears the trigger + attachment representations (baked into the taken crossbow). */
    private void consumeFittedParts() {
        this.mutating = true;
        this.container.setItem(SLOT_TRIGGER, ItemStack.EMPTY);
        this.container.setItem(SLOT_ATTACHMENT, ItemStack.EMPTY);
        this.lastTriggerPresent = false;
        this.lastInputCrossbow = false;
        this.mutating = false;
    }

    private boolean isAttachmentItem(ItemStack stack) {
        return !stack.isEmpty() && findAttachmentId(stack) != null;
    }

    @Nullable
    private ResourceLocation findAttachmentId(ItemStack stack) {
        for (var entry : this.attachments.entrySet()) {
            if (entry.getValue().ingredient().test(stack)) {
                return entry.getKey().location();
            }
        }
        return null;
    }

    /** A representative item that installs the attachment def {@code id}. */
    private ItemStack attachmentItemFor(ResourceLocation id) {
        CrossbowAttachmentDef def = this.attachments.get(id);
        if (def == null) {
            return ItemStack.EMPTY;
        }
        ItemStack[] items = def.ingredient().getItems();
        return items.length > 0 ? items[0].copy() : ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.container instanceof SimpleContainer sc) {
            sc.removeListener(this.listener);
        }
        // Items intentionally stay in the block entity (persist); nothing is
        // returned to the player here.
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate(
                (level, pos) -> level.getBlockState(pos).is(ModBlocks.CROSSBOW_BENCH.get())
                        && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < FUNCTIONAL_SLOTS) {
                // Functional slot -> player inventory.
                if (!this.moveItemStackTo(stack, FUNCTIONAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                if (index == MENU_INPUT && result.is(ModItems.MODULAR_CROSSBOW.get()) && !slot.hasItem()) {
                    this.consumeFittedParts();
                }
            } else {
                // Player inventory -> the one matching functional slot.
                int target = targetSlotFor(stack);
                if (target < 0 || !this.moveItemStackTo(stack, target, target + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** Menu-slot index a shift-clicked inventory item should go to, or -1. */
    private int targetSlotFor(ItemStack stack) {
        if (stack.is(ModItems.MODULAR_BOW.get()) || stack.is(ModItems.MODULAR_CROSSBOW.get())) {
            return MENU_INPUT;
        }
        if (stack.is(ModItems.MECHANICAL_TRIGGER.get())) {
            return MENU_TRIGGER;
        }
        if (isAttachmentItem(stack)) {
            return MENU_ATTACHMENT;
        }
        return -1;
    }
}
