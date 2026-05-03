package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.block.entity.ArcheryTargetBlockEntity;
import net.frostytrix.fletcherstrestle.block.entity.ShotRecord;
import net.frostytrix.fletcherstrestle.network.TargetSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ArcheryTargetMenu extends AbstractContainerMenu {

    private final BlockPos targetPos;
    private final ArcheryTargetBlockEntity blockEntity;
    private List<ShotRecord> shots = new ArrayList<>();

    // Server constructor
    public ArcheryTargetMenu(int containerId, Inventory playerInventory, ArcheryTargetBlockEntity be) {
        super(ModMenuTypes.ARCHERY_TARGET_MENU.get(), containerId);
        this.blockEntity = be;
        this.targetPos = be.getBlockPos();
        this.shots = new ArrayList<>(be.getShots());

        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    public BlockPos getTargetPos() { return targetPos ;}

    // Client constructor
    public ArcheryTargetMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.ARCHERY_TARGET_MENU.get(), containerId);
        this.blockEntity = null;
        this.targetPos = BlockPos.ZERO;
    }

    public void syncToClient(ServerPlayer player) {
        if (blockEntity != null) {
            PacketDistributor.sendToPlayer(player, new TargetSyncPacket(this.containerId, blockEntity.getShots()));
        }
    }

    public boolean isFor(BlockPos pos) {
        return targetPos.equals(pos);
    }

    public List<ShotRecord> getShots() {
        return shots;
    }

    public void setShots(List<ShotRecord> shots) {
        this.shots = new ArrayList<>(shots);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}