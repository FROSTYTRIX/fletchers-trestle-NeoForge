package net.frostytrix.fletcherstrestle.entity.client;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.GarrisonGolemEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Placeholder look for the garrison golem: a plain humanoid in a wooden texture,
 * until its own model exists.
 */
public class GarrisonGolemRenderer extends HumanoidMobRenderer<GarrisonGolemEntity, HumanoidModel<GarrisonGolemEntity>> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "garrison_golem"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/entity/garrison_golem.png");

    public GarrisonGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER_LOCATION)), 0.5f);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 32);
    }

    @Override
    public ResourceLocation getTextureLocation(GarrisonGolemEntity entity) {
        return TEXTURE;
    }
}
