package net.frostytrix.fletcherstrestle.block.entity;

import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.block.custom.EmplacementBlock;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An emplacement's crossbow, its quiver, who is crewing it, and where it's aimed.
 *
 * <p>The crossbow and quiver are synced so the renderer can draw them. The aim is
 * synced too, but only when it moves by more than {@link #AIM_SYNC_DEGREES}, and
 * the renderer eases toward it, so a slewing crossbow doesn't flood the network.</p>
 */
public class EmplacementBlockEntity extends BlockEntity {

    /** Height of the crossbow's pivot above the block's base. */
    public static final double PIVOT_HEIGHT = 1.15;
    /** A crew claim lapses if the golem stops refreshing it for this long. */
    private static final long CREW_TIMEOUT = 20;
    private static final float AIM_SYNC_DEGREES = 2.0f;

    private ItemStack crossbow = ItemStack.EMPTY;
    private ItemStack quiver = ItemStack.EMPTY;

    @Nullable
    private UUID crew;
    private long crewSeen;

    /** World yaw and pitch, in the entity convention: yaw 0 faces south, pitch > 0 looks down. */
    private float aimYaw;
    private float aimPitch;
    private float syncedYaw;
    private float syncedPitch;
    private boolean aimed;

    public EmplacementBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EMPLACEMENT_BE.get(), pos, state);
        this.aimYaw = restYaw();
        this.syncedYaw = this.aimYaw;
    }

    public static boolean canMount(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem;
    }

    // ---------------- contents ----------------

    public ItemStack getCrossbow() {
        return crossbow;
    }

    public ItemStack getQuiver() {
        return quiver;
    }

    public void setCrossbow(ItemStack stack) {
        this.crossbow = stack;
        contentsChanged();
    }

    public void setQuiver(ItemStack stack) {
        this.quiver = stack;
        contentsChanged();
    }

    /** Call after changing the crossbow or quiver in place (a shot, an arrow taken). */
    public void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    public boolean hasAmmo() {
        return !quiver.isEmpty() && ModularQuiverItem.hasArrows(quiver);
    }

    /** Ready to fire: a crossbow, arrows, and no redstone signal holding it. */
    public boolean isReady() {
        return level != null && !crossbow.isEmpty() && hasAmmo() && !level.hasNeighborSignal(getBlockPos());
    }

    // ---------------- crew ----------------

    public boolean isCrewedByOther(LivingEntity golem) {
        return crew != null && !crew.equals(golem.getUUID())
                && level != null && level.getGameTime() - crewSeen <= CREW_TIMEOUT;
    }

    /** Claims the post, or keeps an existing claim alive. Call every tick while crewing. */
    public void claim(LivingEntity golem) {
        this.crew = golem.getUUID();
        this.crewSeen = level != null ? level.getGameTime() : 0;
    }

    public void release(LivingEntity golem) {
        if (golem.getUUID().equals(crew)) {
            crew = null;
            setAim(restYaw(), 0.0f);
        }
    }

    // ---------------- aim ----------------

    public Direction facing() {
        return getBlockState().getValue(EmplacementBlock.FACING);
    }

    public float restYaw() {
        return facing().toYRot();
    }

    /** Where the crossbow's pivot sits, and where its bolts leave from. */
    public Vec3 muzzle() {
        return Vec3.atBottomCenterOf(getBlockPos()).add(0.0, PIVOT_HEIGHT, 0.0);
    }

    public double range() {
        ResourceLocation id = crossbow.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
        if (id != null && level != null) {
            var def = level.registryAccess().registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT).get(id);
            if (def != null && def.stats().zoom() != 1.0f) {
                return FletcherConfig.GARRISON_SCOPED_RANGE.get();
            }
        }
        return FletcherConfig.GARRISON_RANGE.get();
    }

    /** Whether a point lies inside this post's cone and range. */
    public boolean covers(Vec3 point) {
        Vec3 muzzle = muzzle();
        Vec3 to = point.subtract(muzzle);
        if (to.lengthSqr() > range() * range()) return false;
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f;
        return Math.abs(Mth.wrapDegrees(yaw - restYaw())) <= FletcherConfig.GARRISON_HALF_CONE.get();
    }

    public void setAim(float yaw, float pitch) {
        this.aimYaw = yaw;
        this.aimPitch = pitch;
        this.aimed = true;
        if (level != null && !level.isClientSide
                && (Math.abs(Mth.wrapDegrees(yaw - syncedYaw)) > AIM_SYNC_DEGREES
                || Math.abs(pitch - syncedPitch) > AIM_SYNC_DEGREES)) {
            syncedYaw = yaw;
            syncedPitch = pitch;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public float getAimYaw() {
        return aimed ? aimYaw : restYaw();
    }

    public float getAimPitch() {
        return aimed ? aimPitch : 0.0f;
    }

    // ---------------- save & sync ----------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Always write both keys: an empty update tag is ignored by the client.
        tag.put("Crossbow", crossbow.isEmpty() ? new CompoundTag() : crossbow.save(registries));
        tag.put("Quiver", quiver.isEmpty() ? new CompoundTag() : quiver.save(registries));
        tag.putFloat("AimYaw", getAimYaw());
        tag.putFloat("AimPitch", getAimPitch());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crossbow = ItemStack.parseOptional(registries, tag.getCompound("Crossbow"));
        quiver = ItemStack.parseOptional(registries, tag.getCompound("Quiver"));
        if (tag.contains("AimYaw")) {
            aimYaw = tag.getFloat("AimYaw");
            aimPitch = tag.getFloat("AimPitch");
            aimed = true;
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
