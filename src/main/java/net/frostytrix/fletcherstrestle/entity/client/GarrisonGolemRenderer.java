package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Crackiness;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the Bolt Warden in the wood it was built from, cracked as it takes
 * damage, with its eyes and heart glowing while it is on duty.
 */
public class GarrisonGolemRenderer extends MobRenderer<GarrisonGolemEntity, GarrisonGolemModel> {

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final ResourceLocation GLOW = texture("glow");
    private static final Map<Crackiness.Level, ResourceLocation> CRACKS = Map.of(
            Crackiness.Level.LOW, texture("crackiness_low"),
            Crackiness.Level.MEDIUM, texture("crackiness_medium"),
            Crackiness.Level.HIGH, texture("crackiness_high"));

    public GarrisonGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GarrisonGolemModel(context.bakeLayer(GarrisonGolemModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new Cracks(this));
        this.addLayer(new Glow(this));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID,
                "textures/entity/garrison_golem/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(GarrisonGolemEntity golem) {
        return TEXTURES.computeIfAbsent(golem.getWood(), GarrisonGolemRenderer::texture);
    }

    /** Cracks in three stages, like the iron golem's, over whatever wood it is. */
    private static class Cracks extends RenderLayer<GarrisonGolemEntity, GarrisonGolemModel> {
        Cracks(RenderLayerParent<GarrisonGolemEntity, GarrisonGolemModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, GarrisonGolemEntity golem,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (golem.isInvisible()) return;
            ResourceLocation cracks = CRACKS.get(golem.getCrackiness());
            if (cracks != null) {
                renderColoredCutoutModel(getParentModel(), cracks, poseStack, buffer, light, golem, -1);
            }
        }
    }

    /** On duty, its eyes and the heart behind its porthole glow. */
    private static class Glow extends RenderLayer<GarrisonGolemEntity, GarrisonGolemModel> {
        Glow(RenderLayerParent<GarrisonGolemEntity, GarrisonGolemModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, GarrisonGolemEntity golem,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            byte state = golem.getGarrisonState();
            if (golem.isInvisible() || (state != GarrisonGolemEntity.ALERT && state != GarrisonGolemEntity.MANNING
                    && state != GarrisonGolemEntity.RELOADING)) {
                return;
            }
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)),
                    0xF00000, OverlayTexture.NO_OVERLAY);
        }
    }
}
