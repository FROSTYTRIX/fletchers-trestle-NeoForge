package net.frostytrix.fletcherstrestle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the garlands tied to a nail. Only the nail at the near end of a span
 * owns the garland; the far nail keeps a back-reference so that breaking either
 * end takes the whole span down.
 */
public class NailBlockEntity extends BlockEntity {

    /** How many spans one nail can anchor. Enough to chain and to fan out a little. */
    public static final int MAX_GARLANDS = 4;

    /** One strung garland: where it goes, and the item it was made from. */
    public record Span(BlockPos target, ItemStack garland) {
    }

    private final List<Span> spans = new ArrayList<>();
    /** Nails that have strung a garland to this one. Used only to clean up on break. */
    private final List<BlockPos> incoming = new ArrayList<>();

    public NailBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAIL_BE.get(), pos, state);
    }

    public List<Span> getSpans() {
        return spans;
    }

    public boolean hasRoom() {
        return spans.size() + incoming.size() < MAX_GARLANDS;
    }

    public void addSpan(BlockPos target, ItemStack garland) {
        // One garland per span, and never carrying a pending anchor.
        ItemStack single = garland.copyWithCount(1);
        single.remove(net.frostytrix.fletcherstrestle.component.ModDataComponents.GARLAND_ANCHOR.get());
        spans.add(new Span(target, single));
        sync();
    }

    public void addIncoming(BlockPos source) {
        incoming.add(source);
        sync();
    }

    public void removeLinksTo(BlockPos other) {
        spans.removeIf(s -> s.target().equals(other));
        incoming.removeIf(p -> p.equals(other));
        sync();
    }

    /**
     * Drops every garland anchored here and clears the matching link from the
     * nail at the other end, so no span is left half-attached.
     */
    public void dropAndClearGarlands() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (Span span : spans) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    span.garland().copy());
            if (level.getBlockEntity(span.target()) instanceof NailBlockEntity far) {
                far.removeLinksTo(worldPosition);
            }
        }
        for (BlockPos source : incoming) {
            if (level.getBlockEntity(source) instanceof NailBlockEntity near) {
                // The far nail owns the item, so it drops it.
                near.dropSpanTo(worldPosition);
            }
        }
        spans.clear();
        incoming.clear();
    }

    private void dropSpanTo(BlockPos target) {
        if (level == null || level.isClientSide) {
            return;
        }
        spans.stream()
                .filter(s -> s.target().equals(target))
                .forEach(s -> Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(),
                        worldPosition.getZ(), s.garland().copy()));
        spans.removeIf(s -> s.target().equals(target));
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag spanList = new ListTag();
        for (Span span : spans) {
            CompoundTag entry = new CompoundTag();
            entry.put("Target", NbtUtils.writeBlockPos(span.target()));
            entry.put("Garland", span.garland().save(registries));
            spanList.add(entry);
        }
        // Always write the keys: a completely empty update tag is ignored by the
        // client, which would leave stale garlands drawn after one is taken down.
        tag.put("Spans", spanList);
        ListTag incomingList = new ListTag();
        for (BlockPos pos : incoming) {
            CompoundTag entry = new CompoundTag();
            entry.put("Pos", NbtUtils.writeBlockPos(pos));
            incomingList.add(entry);
        }
        tag.put("Incoming", incomingList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spans.clear();
        incoming.clear();
        ListTag spanList = tag.getList("Spans", 10);
        for (int i = 0; i < spanList.size(); i++) {
            CompoundTag entry = spanList.getCompound(i);
            NbtUtils.readBlockPos(entry, "Target").ifPresent(target ->
                    spans.add(new Span(target, ItemStack.parseOptional(registries, entry.getCompound("Garland")))));
        }
        ListTag incomingList = tag.getList("Incoming", 10);
        for (int i = 0; i < incomingList.size(); i++) {
            NbtUtils.readBlockPos(incomingList.getCompound(i), "Pos").ifPresent(incoming::add);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
