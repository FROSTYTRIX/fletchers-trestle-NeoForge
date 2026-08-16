package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.block.custom.WeaponRackBlock;
import net.frostytrix.fletcherstrestle.block.entity.WeaponRackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the weapon resting on a rack. The item is rendered separately from the
 * block model so it shows the player's actual assembly: a cherry-limbed bow
 * looks different on the wall to a dark-oak one.
 */
public class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity> {

    public WeaponRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WeaponRackBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        ItemStack stack = entity.getDisplayedItem();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        // Centre of the block, then turn to face out of the wall.
        poseStack.translate(0.5, 0.5, 0.5);
        Direction facing = entity.getBlockState().getValue(WeaponRackBlock.FACING);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // Push out to the pegs. Tune this one number if the weapon sits inside
        // the wall or floats off it.
        poseStack.translate(0.0, 0.0, -0.3);

        // Spin the weapon within its own flat plane so it hangs at an angle
        // across the pegs.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45f));

        // Every item model carries a 180 degree Y flip in the FIXED context
        // (vanilla's item/generated defines it, and our modular models now match).
        // Undo it here so the weapon faces out of the wall.
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));

        poseStack.scale(0.8f, 0.8f, 0.8f);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                combinedLight, combinedOverlay, poseStack, bufferSource, entity.getLevel(), 0);

        poseStack.popPose();
    }
}
