package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

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
            // Handle Naming and Skins
            if (itemstack.has(DataComponents.CUSTOM_NAME)) {

                // FIX: Grab the pure, unformatted string directly from the component
                String plainName = itemstack.get(DataComponents.CUSTOM_NAME).getString();

                dummy.setCustomName(itemstack.get(DataComponents.CUSTOM_NAME));
                dummy.setCustomNameVisible(true);

                if (serverLevel.getServer() != null) {
                    serverLevel.getServer().getProfileCache().getAsync(plainName).thenAccept(profileOpt -> {
                        serverLevel.getServer().execute(() -> {
                            if (profileOpt.isPresent()) {
                                System.out.println("DEBUG: Found UUID " + profileOpt.get().getId() + " for name " + plainName);
                                dummy.setSkin(profileOpt.get().getName(), profileOpt.get().getId());
                            } else {
                                // This is a huge red flag if it triggers
                                System.out.println("DEBUG: Mojang Profile Cache returned EMPTY for: " + plainName);
                                dummy.setSkin(plainName, null);
                            }
                        });

                        serverLevel.getServer().execute(() -> {
                            if (profileOpt.isPresent()) {
                                dummy.setSkin(profileOpt.get().getName(), profileOpt.get().getId());
                            } else {
                                // Fallback: Keeps the name even if the server UUID lookup fails
                                dummy.setSkin(plainName, null);
                            }
                        });
                    });
                }
            }

            // Placement Logic
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