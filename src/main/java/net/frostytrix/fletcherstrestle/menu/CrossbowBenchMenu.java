package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CrossbowBenchMenu extends AbstractContainerMenu {

    public static final int SLOT_INPUT = 0;       // bow / crossbow
    public static final int SLOT_ATTACHMENT = 1;
    public static final int SLOT_TRIGGER = 2;
    private static final int FUNCTIONAL_SLOTS = 3;

    private boolean mutating = false;
    // Previous-state snapshot, used to tell "a crossbow was just placed" (unpack)
    // apart from "the trigger was just removed" (disassemble) — the two look
    // identical in the resulting slots, so we diff against the prior state.
    private boolean lastInputCrossbow = false;
    private boolean lastTriggerPresent = false;
    private final Container container = new SimpleContainer(FUNCTIONAL_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            CrossbowBenchMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;

    public CrossbowBenchMenu(int id, Inventory playerInv) {
        this(id, playerInv, ContainerLevelAccess.NULL);
    }

    public CrossbowBenchMenu(int id, Inventory playerInv, ContainerLevelAccess access) {
        super(ModMenuTypes.CROSSBOW_BENCH_MENU.get(), id);
        this.access = access;

        // Functional slots — coordinates match the GUI texture.
        // The input slot consumes the fitted parts when a finished crossbow is
        // taken out (they were only slot representations of what it contains).
        this.addSlot(new Slot(this.container, SLOT_INPUT, 81, 23) {
            @Override
            public void onTake(Player p, ItemStack stack) {
                if (stack.is(ModItems.MODULAR_CROSSBOW.get())) {
                    CrossbowBenchMenu.this.consumeFittedParts();
                }
                super.onTake(p, stack);
            }
        });
        this.addSlot(new Slot(this.container, SLOT_TRIGGER, 64, 46));
        this.addSlot(new Slot(this.container, SLOT_ATTACHMENT, 98, 46));

        // Player inventory + hotbar.
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, invY + 58));
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
                this.updateBench(level);
            } finally {
                this.mutating = false;
            }
        });
    }

    /**
     * Keeps the input item consistent with the trigger/attachment slots:
     * trigger present &lt;-&gt; crossbow, trigger absent &lt;-&gt; bow. The slot contents are
     * representations of what the crossbow contains; they're consumed when the
     * finished crossbow leaves the input slot (see {@link #consumeFittedParts}).
     */
    private void updateBench(Level level) {
        ItemStack input = this.container.getItem(SLOT_INPUT);
        ItemStack attachment = this.container.getItem(SLOT_ATTACHMENT);
        ItemStack trigger = this.container.getItem(SLOT_TRIGGER);

        boolean inputCrossbow = input.is(ModItems.MODULAR_CROSSBOW.get());
        boolean inputBow = input.is(ModItems.MODULAR_BOW.get());
        boolean triggerPresent = trigger.is(ModItems.MECHANICAL_TRIGGER.get());

        if (inputCrossbow && !this.lastInputCrossbow && !triggerPresent) {
            // A finished crossbow was just placed -> unpack its parts into the slots.
            this.container.setItem(SLOT_TRIGGER, new ItemStack(ModItems.MECHANICAL_TRIGGER.get()));
            ResourceLocation att = input.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
            if (att != null && attachment.isEmpty()) {
                ItemStack attItem = attachmentItemFor(level, att);
                if (!attItem.isEmpty()) {
                    this.container.setItem(SLOT_ATTACHMENT, attItem);
                }
            }
        } else if (inputBow && triggerPresent && !this.lastTriggerPresent) {
            // Trigger added to a bow -> assemble into a crossbow (carry the assembly).
            this.container.setItem(SLOT_INPUT, withAssemblyOf(input, ModItems.MODULAR_CROSSBOW.get()));
        } else if (inputCrossbow && !triggerPresent && this.lastTriggerPresent) {
            // Trigger pulled out of a crossbow -> revert to a bow. The attachment
            // can't stay on a bow; it's left sitting in the attachment slot.
            this.container.setItem(SLOT_INPUT, withAssemblyOf(input, ModItems.MODULAR_BOW.get()));
        }

        // Mirror the attachment slot onto the current crossbow's component.
        ItemStack current = this.container.getItem(SLOT_INPUT);
        if (current.is(ModItems.MODULAR_CROSSBOW.get())) {
            ItemStack att = this.container.getItem(SLOT_ATTACHMENT);
            if (!att.isEmpty()) {
                ResourceLocation id = findAttachmentId(level, att);
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

    /** A new {@code resultItem} stack carrying {@code source}'s bow assembly. */
    private static ItemStack withAssemblyOf(ItemStack source, net.minecraft.world.item.Item resultItem) {
        ItemStack out = new ItemStack(resultItem);
        var assembly = source.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            out.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);
        }
        return out;
    }

    /** Clears the trigger + attachment slot representations (their item is baked into the taken crossbow). */
    private void consumeFittedParts() {
        this.mutating = true;
        this.container.setItem(SLOT_TRIGGER, ItemStack.EMPTY);
        this.container.setItem(SLOT_ATTACHMENT, ItemStack.EMPTY);
        this.lastTriggerPresent = false;
        this.mutating = false;
    }

    /** The crossbow_attachment def id whose ingredient accepts {@code stack}, or null. */
    @org.jetbrains.annotations.Nullable
    private static ResourceLocation findAttachmentId(Level level, ItemStack stack) {
        var registry = level.registryAccess().registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT);
        for (var entry : registry.entrySet()) {
            if (entry.getValue().ingredient().test(stack)) {
                return entry.getKey().location();
            }
        }
        return null;
    }

    /** A representative item that installs the attachment def {@code id} (its ingredient's first item). */
    private static ItemStack attachmentItemFor(Level level, ResourceLocation id) {
        var registry = level.registryAccess().registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT);
        var def = registry.get(id);
        if (def == null) {
            return ItemStack.EMPTY;
        }
        ItemStack[] items = def.ingredient().getItems();
        return items.length > 0 ? items[0].copy() : ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // A finished crossbow in the input slot already contains its trigger +
        // attachment, so drop the slot representations before returning items.
        if (this.container.getItem(SLOT_INPUT).is(ModItems.MODULAR_CROSSBOW.get())) {
            this.mutating = true;
            this.container.setItem(SLOT_TRIGGER, ItemStack.EMPTY);
            this.container.setItem(SLOT_ATTACHMENT, ItemStack.EMPTY);
            this.mutating = false;
        }
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
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
                if (!this.moveItemStackTo(stack, FUNCTIONAL_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, FUNCTIONAL_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            // Shift-taking a finished crossbow consumes its fitted-part representations.
            if (index == SLOT_INPUT && result.is(ModItems.MODULAR_CROSSBOW.get()) && !slot.hasItem()) {
                this.consumeFittedParts();
            }
        }
        return result;
    }
}
