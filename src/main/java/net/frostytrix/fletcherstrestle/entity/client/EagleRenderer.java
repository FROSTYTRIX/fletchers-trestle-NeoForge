package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.client.EagleModel;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class EagleRenderer extends MobRenderer<EagleEntity, EagleModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID,
                    "textures/entity/eagle/eagle.png");

    public EagleRenderer(EntityRendererProvider.Context context) {
        super(context,
                new EagleModel(context.bakeLayer(EagleModel.LAYER_LOCATION)),
                0.4f); // shadow radius
    }

    @Override
    public ResourceLocation getTextureLocation(EagleEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(EagleEntity entity, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTicks,
                                  float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);

        // Tilt the whole body forward during flight — feels more aerodynamic
        if (entity.isFlying()) {
            Vec3 motion = entity.getDeltaMovement();
            float speed = (float) motion.horizontalDistance();
            // Pitch forward up to 30 degrees based on horizontal speed
            float pitchAngle = Math.min(speed * 40f, 30f);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitchAngle));
        }
    }
}