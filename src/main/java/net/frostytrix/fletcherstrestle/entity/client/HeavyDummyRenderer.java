package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class HeavyDummyRenderer extends LivingEntityRenderer<HeavyDummyEntity, HeavyDummyModel<HeavyDummyEntity>> {

    public HeavyDummyRenderer(EntityRendererProvider.Context context) {
        // Use LivingEntityRenderer constructor
        super(context, new HeavyDummyModel<>(context.bakeLayer(HeavyDummyModel.LAYER_LOCATION)), 0.5f);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(HeavyDummyEntity heavyDummyEntity) {
        return ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/entity/heavy_dummy.png");
    }

    @Override
    protected float getBob(HeavyDummyEntity livingBase, float partialTick) {
        return 0f;
    }
}