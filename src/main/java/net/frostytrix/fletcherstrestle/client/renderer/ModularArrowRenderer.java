package net.frostytrix.fletcherstrestle.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import org.joml.Matrix4f;

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

        float shake = (float) entity.shakeTime - partialTicks;
        if (shake > 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0D, 0.0D, 0.0D);

        // 2. Render the Layers
        // We render three times: Shaft -> Fletching -> Head
// 2. Render the Layers
        renderPart(poseStack, buffer, packedLight, getTexture(assembly, "shaft"));

// Scale up microscopically so fletching renders just outside the shaft
        poseStack.scale(1.001F, 1.001F, 1.001F);
        renderPart(poseStack, buffer, packedLight, getTexture(assembly, "fletching"));

// Scale up again for the head
        poseStack.scale(1.001F, 1.001F, 1.001F);
        renderPart(poseStack, buffer, packedLight, getTexture(assembly, "head"));

// Glass-vial arrows get a tinted "liquid" overlay on top of the head, using
// the same head sprite layout. The texture should be a white silhouette of
// just the liquid inside the vial: colour is applied at render time so any
// potion can re-use the same texture.
        if ("glass_vial".equals(assembly.head())) {
            PotionContents potion = entity.getSyncedItemStack().get(DataComponents.POTION_CONTENTS);
            if (potion != null) {
                int color = potion.getColor();
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                poseStack.scale(1.001F, 1.001F, 1.001F);
                ResourceLocation liquidTex = ResourceLocation.fromNamespaceAndPath(
                        FletcherTrestle.MOD_ID,
                        "textures/entity/projectiles/head/glass_vial_liquid.png");
                renderPartTinted(poseStack, buffer, packedLight, liquidTex, r, g, b, 255);
            }
        }

        poseStack.popPose();

        if (entity.isHooked()) {
            Entity owner = entity.getOwner();
            if (owner instanceof Player player) {
                renderGrapplingLine(entity, partialTicks, poseStack, buffer, player, packedLight);
            }
        }
    }

    private void renderPart(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(texture));
        // Drawing the 4 vanes/sides of the arrow
        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.vertex(poseStack, vertexConsumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, packedLight);
            this.vertex(poseStack, vertexConsumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, packedLight);
            this.vertex(poseStack, vertexConsumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, packedLight);
            this.vertex(poseStack, vertexConsumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, packedLight);
        }
    }

    // Same geometry as renderPart but tints each vertex with the given RGBA.
    // Uses entityCutout (NOT translucent): the 4 vanes share the central axis,
    // and translucent blending makes them z-fight against each other at the
    // overlap. Cutout is binary (drawn or not), so it renders cleanly. Vanilla
    // TippedArrow uses the same render type for its tip overlay.
    private void renderPartTinted(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                  ResourceLocation texture, int r, int g, int b, int a) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(texture));
        for (int j = 0; j < 4; ++j) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.vertexColored(poseStack, vc, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, packedLight, r, g, b, a);
            this.vertexColored(poseStack, vc, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, packedLight, r, g, b, a);
            this.vertexColored(poseStack, vc, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, packedLight, r, g, b, a);
            this.vertexColored(poseStack, vc, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, packedLight, r, g, b, a);
        }
    }

    private void vertexColored(PoseStack poseStack, VertexConsumer consumer, int x, int y, int z,
                               float u, float v, int normalX, int normalZ, int normalY,
                               int packedLight, int r, int g, int b, int a) {
        consumer.addVertex(poseStack.last().pose(), (float) x, (float) y, (float) z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(poseStack.last(), (float) normalX, (float) normalY, (float) normalZ);
    }

    private void renderGrapplingLine(ModularArrowEntity arrow, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, Player player, int packedLight) {
        double pX = Mth.lerp(partialTicks, player.xo, player.getX());
        double pY = Mth.lerp(partialTicks, player.yo, player.getY()) + player.getBbHeight() * 0.5;
        double pZ = Mth.lerp(partialTicks, player.zo, player.getZ());

        double aX = Mth.lerp(partialTicks, arrow.xo, arrow.getX());
        double aY = Mth.lerp(partialTicks, arrow.yo, arrow.getY());
        double aZ = Mth.lerp(partialTicks, arrow.zo, arrow.getZ());

        float dx = (float) (pX - aX);
        float dy = (float) (pY - aY);
        float dz = (float) (pZ - aZ);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.leash());
        Matrix4f matrix = poseStack.last().pose();

        int r = 110, g = 85, b = 45;
        int r2 = 80, g2 = 60, b2 = 30;

        for (int i = 0; i <= 24; ++i) {
            float f = (float) i / 24.0F;
            float x = dx * f;
            float y = dy * f;
            float z = dz * f;
            vertexConsumer.addVertex(matrix, x, y, z).setColor(r, g, b, 255).setLight(packedLight);
            vertexConsumer.addVertex(matrix, x + 0.035f, y + 0.035f, z).setColor(r2, g2, b2, 255).setLight(packedLight);
        }
    }

    private ResourceLocation getTexture(ArrowAssembly assembly, String part) {
        // Honors a def's texture override; otherwise builds the conventional
        // path in the material's own namespace. ".png" suffix because the
        // entity renderer expects a full texture path including extension.
        return switch (part) {
            case "shaft" -> net.frostytrix.fletcherstrestle.material.Materials.arrowShaftTexture(
                    assembly.shaft(), "textures/entity/projectiles/shaft", ".png");
            case "head" -> net.frostytrix.fletcherstrestle.material.Materials.arrowHeadTexture(
                    assembly.head(), "textures/entity/projectiles/head", ".png");
            default -> net.frostytrix.fletcherstrestle.material.Materials.arrowFletchingTexture(
                    assembly.fletching(), "textures/entity/projectiles/fletching", ".png");
        };
    }


    @Override
    public ResourceLocation getTextureLocation(ModularArrowEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    }

    public void vertex(PoseStack poseStack, VertexConsumer consumer, int x, int y, int z, float u, float v, int normalX, int normalZ, int normalY, int packedLight) {
        consumer.addVertex(poseStack.last().pose(), (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(poseStack.last(), (float) normalX, (float) normalY, (float) normalZ);
    }
}