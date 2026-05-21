package net.frostytrix.fletcherstrestle.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.frostytrix.fletcherstrestle.block.custom.EagleNestBlock;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class EagleNestBlockEntity extends BlockEntity {

    public static final int MAX_EGGS                  = 3;
    public static final int MAX_CLAIMED_EAGLES        = 2;
    public static final long DEFAULT_HATCH_TIME_TICKS = 24000L;

    @Nullable private UUID ownerUUID;
    @Nullable private String ownerName;
    private final List<UUID> claimedEagles = new ArrayList<>(MAX_CLAIMED_EAGLES);
    private final List<EggData> eggs       = new ArrayList<>(MAX_EGGS);

    public record EggData(long laidAtTick, long hatchAtTick) {
        static final Codec<EggData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.fieldOf("Laid").forGetter(EggData::laidAtTick),
                Codec.LONG.fieldOf("Hatch").forGetter(EggData::hatchAtTick)
        ).apply(inst, EggData::new));
    }

    public EagleNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EAGLE_NEST_BE.get(), pos, state);
    }

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

    public boolean hasEggSpace()      { return eggs.size() < MAX_EGGS; }
    public int eggCount()             { return eggs.size(); }
    public List<EggData> getEggs()    { return eggs; }

    public void addEgg(long currentGameTick) {
        if (eggs.size() >= MAX_EGGS) return;
        eggs.add(new EggData(currentGameTick, currentGameTick + DEFAULT_HATCH_TIME_TICKS));
        updateEggCountBlockstate();
        setChanged();
    }

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
        // 26.1: EntityType.create requires (ServerLevel, EntitySpawnReason).
        EagleEntity eaglet = ModEntities.EAGLE.get().create(level, EntitySpawnReason.BREEDING);
        if (eaglet == null) return;
        // 26.1: Entity.moveTo(x,y,z,yaw,pitch) removed; use setPos + setYRot.
        eaglet.setPos(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 1.0,
                worldPosition.getZ() + 0.5);
        eaglet.setYRot(level.getRandom().nextFloat() * 360f);
        eaglet.setAge(-24000);
        if (ownerUUID != null) {
            eaglet.setTame(true, true);
            // TamableAnimal.setOwnerUUID gone; resolve via getOwner relationship.
            // Owner UUID stored via entity data; using setOwnerReference helper.
            net.minecraft.world.entity.player.Player owner = level.getPlayerByUUID(ownerUUID);
            if (owner != null) eaglet.tame(owner);
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (ownerUUID != null) output.store("Owner", UUIDUtil.CODEC, ownerUUID);
        if (ownerName != null) output.putString("OwnerName", ownerName);

        var eagleList = output.list("ClaimedEagles", UUIDUtil.CODEC);
        for (UUID e : claimedEagles) eagleList.add(e);

        var eggList = output.list("Eggs", EggData.CODEC);
        for (EggData egg : eggs) eggList.add(egg);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ownerUUID = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = input.getString("OwnerName").orElse(null);

        this.claimedEagles.clear();
        input.list("ClaimedEagles", UUIDUtil.CODEC)
                .ifPresent(list -> list.forEach(claimedEagles::add));

        this.eggs.clear();
        input.list("Eggs", EggData.CODEC)
                .ifPresent(list -> list.forEach(eggs::add));
    }
}
