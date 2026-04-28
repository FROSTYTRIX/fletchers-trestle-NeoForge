package net.frostytrix.fletcherstrestle.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.ModularArrowEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ModularArrowRenderer extends ArrowRenderer<ModularArrowEntity> {

    public ModularArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ModularArrowEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ArrowAssembly assembly = entity.getSyncedItemStack().get(ModDataComponents.ARROW_ASSEMBLY.get());
        if (assembly == null) return;

        // 1. Prepare the rotation and shake (Vanilla Logic)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        float shake = (float)entity.shakeTime - partialTicks;
        if (shake > 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0D, 0.0D, 0.0D);

        // 2. Render the Layers
        // We render three times: Shaft -> Fletching -> Head
        renderPart(poseStack, buffer, packedLight, getTexture(assembly, "shaft"));
        renderPart(poseStack, buffer, packedLight, getTexture(assembly, "fletching"));
        renderPart(poseStack, buffer, packedLight, getTexture(assembly, "head"));

        poseStack.popPose();
    }

    private void renderPart(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(texture));
        // Drawing the 4 vanes/sides of the arrow
        for(int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.vertex(poseStack, vertexConsumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, packedLight);
            this.vertex(poseStack, vertexConsumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, packedLight);
            this.vertex(poseStack, vertexConsumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, packedLight);
            this.vertex(poseStack, vertexConsumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, packedLight);
        }
    }

    private ResourceLocation getTexture(ArrowAssembly assembly, String part) {
        String name = switch (part) {
            case "shaft" -> assembly.shaft().toLowerCase().replace(" ", "_");
            case "head" -> assembly.head().toLowerCase().replace(" ", "_");
            default -> assembly.fletching().toLowerCase().replace(" ", "_");
        }; // Fixes Dark Oak -> dark_oak

        return ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/entity/projectiles/" + part + "/" + name + ".png");
    }

    // This is required by the parent class but ignored by our custom render
    @Override
    public ResourceLocation getTextureLocation(ModularArrowEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    }

    // Helper for vertex construction
    public void vertex(PoseStack poseStack, VertexConsumer consumer, int x, int y, int z, float u, float v, int normalX, int normalZ, int normalY, int packedLight) {
        consumer.addVertex(poseStack.last().pose(), (float)x, (float)y, (float)z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(poseStack.last(), (float)normalX, (float)normalY, (float)normalZ);
    }
}