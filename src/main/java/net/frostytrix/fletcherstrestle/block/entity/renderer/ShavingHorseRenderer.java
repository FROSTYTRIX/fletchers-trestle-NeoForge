package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.block.custom.ShavingHorseBlock;
import net.frostytrix.fletcherstrestle.block.entity.ShavingHorseBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ShavingHorseRenderer implements BlockEntityRenderer<ShavingHorseBlockEntity> {

    public ShavingHorseRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ShavingHorseBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        ItemStack stack = entity.itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        poseStack.translate(0.5, 1.05, 0.5); // Milieu du bloc, un peu en hauteur

        Direction facing = entity.getBlockState().getValue(ShavingHorseBlock.FACING);
        float rotation = -facing.toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));

        poseStack.scale(.9f, .9f, .9f);
            poseStack.translate(0.0, -0.25, 0);

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                combinedLight, combinedOverlay, poseStack, bufferSource, entity.getLevel(), 0);

        poseStack.popPose();
    }
}
