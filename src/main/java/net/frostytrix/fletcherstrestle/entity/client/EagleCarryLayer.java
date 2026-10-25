package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws whatever the eagle has in its talons: a fish, a rabbit, fetched arrows.
 * In flight it hangs under the tucked legs; on the ground it lies at its feet.
 *
 * <p>Positions are in model space, where one unit is one block and +Y points down
 * from the top of the model; the feet stand at 1.5.</p>
 */
public class EagleCarryLayer extends RenderLayer<EagleEntity, EagleModel> {

    private static final float FLYING_Y = 1.30f;
    private static final float FLYING_Z = 0.28f;
    private static final float STANDING_Y = 1.46f;
    private static final float STANDING_Z = -0.12f;
    private static final float SCALE = 0.55f;

    private final ItemRenderer itemRenderer;

    public EagleCarryLayer(RenderLayerParent<EagleEntity, EagleModel> parent, ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, EagleEntity eagle,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack carried = eagle.getCarried();
        if (carried.isEmpty()) return;
        boolean flying = eagle.isFlying();
        poseStack.pushPose();
        poseStack.translate(0.0f, flying ? FLYING_Y : STANDING_Y, flying ? FLYING_Z : STANDING_Z);
        // Lying flat, nose forward.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        poseStack.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(carried, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, eagle.level(), eagle.getId());
        poseStack.popPose();
    }
}
