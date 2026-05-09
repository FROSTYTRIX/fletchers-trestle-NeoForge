package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class HeavyDummyItem extends Item {
    public HeavyDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemstack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();

        BlockPos spawnBlockPos = clickedPos.relative(face);

        HeavyDummyEntity dummy = ModEntities.HEAVY_DUMMY.get().create(serverLevel);
        if (dummy != null) {
            if (itemstack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                dummy.setCustomName(itemstack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));

                // This makes the name render even if the player isn't looking directly at it (like an Armor Stand)
                dummy.setCustomNameVisible(true);
            }

            float yRot = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
            float snappedRot = (float) Mth.floor((Mth.wrapDegrees(yRot) + 22.5F) / 45.0F) * 45.0F;

            dummy.moveTo(spawnBlockPos.getX() + 0.5D, spawnBlockPos.getY(), spawnBlockPos.getZ() + 0.5D, snappedRot, 0.0F);

            dummy.setYBodyRot(snappedRot);
            dummy.setYHeadRot(snappedRot);

            serverLevel.addFreshEntity(dummy);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}