package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class EagleRenderer extends MobRenderer<EagleEntity, EagleModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID,
                    "textures/entity/eagle.png");

    public EagleRenderer(EntityRendererProvider.Context context) {
        super(context,
                new EagleModel(context.bakeLayer(EagleModel.LAYER_LOCATION)),
                0.4f); // shadow radius
        this.addLayer(new EagleCarryLayer(this, context.getItemRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(EagleEntity entity) {
        return TEXTURE;
    }

    /** Height of the body's middle above the feet, in blocks: the point it pitches and rolls about. */
    private static final float BODY_CENTRE = 0.5f;

    @Override
    protected void setupRotations(EagleEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks,
                                  float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);

        // Flight attitude. In this frame forward is -Z and the eagle's right is +X,
        // so a positive X turn lifts the nose and a positive Z turn lifts the right wing.
        float pitch = entity.getFlightPitch(partialTicks);
        float bank = entity.getBank(partialTicks);
        if (pitch != 0f || bank != 0f) {
            poseStack.translate(0.0f, BODY_CENTRE, 0.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-bank));
            poseStack.translate(0.0f, -BODY_CENTRE, 0.0f);
        }
    }
}