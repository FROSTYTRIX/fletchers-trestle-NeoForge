package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.block.custom.EagleNestBlock;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// Nest block entity. Tracks ownership (cached name + UUID), the breeding
// pair that claims this nest, and the eggs incubating inside. Eggs hatch
// on a per-egg timer; on hatch a baby EagleEntity spawns above the nest
// and is auto-tamed to the nest's owner.
public class EagleNestBlockEntity extends BlockEntity {

    public static final int MAX_EGGS                  = 3;
    public static final int MAX_CLAIMED_EAGLES        = 2;
    public static final long DEFAULT_HATCH_TIME_TICKS = 24000L; // ~20 in-game minutes

    @Nullable private UUID ownerUUID;
    @Nullable private String ownerName;          // cached so display works offline
    private final List<UUID> claimedEagles = new ArrayList<>(MAX_CLAIMED_EAGLES);
    private final List<EggData> eggs       = new ArrayList<>(MAX_EGGS);

    public record EggData(long laidAtTick, long hatchAtTick) {}

    public EagleNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EAGLE_NEST_BE.get(), pos, state);
    }

    // ----------------- Ownership / claim API -----------------
    public boolean isClaimed()                  { return ownerUUID != null; }
    @Nullable public UUID   getOwnerUUID()      { return ownerUUID; }
    @Nullable public String getOwnerName()      { return ownerName; }
    public List<UUID>       getClaimedEagles()  { return claimedEagles; }

    public void claim(UUID owner, String name) {
        this.ownerUUID = owner;
        this.ownerName = name;
        setChanged();
    }

    public void unclaim() {
        this.ownerUUID = null;
        this.ownerName = null;
        this.claimedEagles.clear();
        setChanged();
    }

    public boolean canAddBreedingEagle(UUID eagleUUID) {
        if (claimedEagles.contains(eagleUUID)) return true;
        return claimedEagles.size() < MAX_CLAIMED_EAGLES;
    }

    public void addBreedingEagle(UUID eagleUUID) {
        if (!claimedEagles.contains(eagleUUID) && claimedEagles.size() < MAX_CLAIMED_EAGLES) {
            claimedEagles.add(eagleUUID);
            setChanged();
        }
    }

    // ----------------- Egg API -----------------
    public boolean hasEggSpace()      { return eggs.size() < MAX_EGGS; }
    public int eggCount()             { return eggs.size(); }
    public List<EggData> getEggs()    { return eggs; }

    // Drop a freshly-laid egg into the nest. Hatch time is gameTime + offset.
    public void addEgg(long currentGameTick) {
        if (eggs.size() >= MAX_EGGS) return;
        eggs.add(new EggData(currentGameTick, currentGameTick + DEFAULT_HATCH_TIME_TICKS));
        updateEggCountBlockstate();
        setChanged();
    }

    // ----------------- Ticking (hatch logic) -----------------
    public static void serverTick(Level level, BlockPos pos, BlockState state, EagleNestBlockEntity nest) {
        if (nest.eggs.isEmpty()) return;
        long now = level.getGameTime();

        boolean changed = false;
        Iterator<EggData> it = nest.eggs.iterator();
        while (it.hasNext()) {
            EggData egg = it.next();
            if (now >= egg.hatchAtTick()) {
                nest.spawnEaglet((ServerLevel) level);
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            nest.updateEggCountBlockstate();
            nest.setChanged();
        }
    }

    private void spawnEaglet(ServerLevel level) {
        EagleEntity eaglet = ModEntities.EAGLE.get().create(level);
        if (eaglet == null) return;
        eaglet.moveTo(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5,
                level.random.nextFloat() * 360f,
                0f);
        eaglet.setAge(-24000); // baby, ~20 minutes to grow up
        // Auto-tame to nest owner so the eaglet immediately belongs to them.
        if (ownerUUID != null) {
            eaglet.setTame(true, true);
            eaglet.setOwnerUUID(ownerUUID);
        }
        level.addFreshEntity(eaglet);
    }

    private void updateEggCountBlockstate() {
        if (level == null) return;
        BlockState current = getBlockState();
        if (!current.hasProperty(EagleNestBlock.EGG_COUNT)) return;
        int clamped = Math.min(eggs.size(), MAX_EGGS);
        if (current.getValue(EagleNestBlock.EGG_COUNT) != clamped) {
            level.setBlock(worldPosition,
                    current.setValue(EagleNestBlock.EGG_COUNT, clamped), 3);
        }
    }

    // ----------------- Save / load -----------------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        if (ownerName != null) tag.putString("OwnerName", ownerName);

        ListTag eagleList = new ListTag();
        for (UUID e : claimedEagles) {
            CompoundTag c = new CompoundTag();
            c.putUUID("UUID", e);
            eagleList.add(c);
        }
        tag.put("ClaimedEagles", eagleList);

        ListTag eggList = new ListTag();
        for (EggData egg : eggs) {
            CompoundTag c = new CompoundTag();
            c.putLong("Laid", egg.laidAtTick());
            c.putLong("Hatch", egg.hatchAtTick());
            eggList.add(c);
        }
        tag.put("Eggs", eggList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.ownerName = tag.contains("OwnerName") ? tag.getString("OwnerName") : null;

        this.claimedEagles.clear();
        ListTag eagleList = tag.getList("ClaimedEagles", Tag.TAG_COMPOUND);
        for (int i = 0; i < eagleList.size(); i++) {
            CompoundTag c = eagleList.getCompound(i);
            if (c.hasUUID("UUID")) claimedEagles.add(c.getUUID("UUID"));
        }

        this.eggs.clear();
        ListTag eggList = tag.getList("Eggs", Tag.TAG_COMPOUND);
        for (int i = 0; i < eggList.size(); i++) {
            CompoundTag c = eggList.getCompound(i);
            this.eggs.add(new EggData(c.getLong("Laid"), c.getLong("Hatch")));
        }
    }
}
