package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.DippingVatBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class DippingVatRenderer implements BlockEntityRenderer<DippingVatBlockEntity> {

    public DippingVatRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DippingVatBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FluidStack fluidStack = blockEntity.fluidTank.getFluid();
        if (fluidStack.isEmpty()) return;

        // 1. On récupère la texture par défaut du fluide
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite waterSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(clientFluid.getStillTexture(fluidStack));

        // On récupère notre masque blanc pour le fond coloré (le fluid_mask fait un aplat parfait)
        TextureAtlasSprite whiteSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(net.minecraft.resources.Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "block/fluid_mask"));

        // 2. Calcul de la hauteur du fluide
        float minHeight = 0.125f;
        float maxHeight = 0.875f;
        float fillRatio = (float) fluidStack.getAmount() / (float) blockEntity.fluidTank.getCapacity();
        float currentHeight = minHeight + ((maxHeight - minHeight) * fillRatio);

        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        Matrix4f matrix = poseStack.last().pose();

        // 3. Extraction de la couleur
        // Le fluide donne lui-même sa couleur (grâce à notre ModClientFluidEvents !)
        // On force l'alpha à l'opacité maximale (0xFF000000) pour un rendu parfait du fond
        int tintColor = clientFluid.getTintColor(fluidStack) | 0xFF000000;

        float r = ((tintColor >> 16) & 0xFF) / 255f;
        float g = ((tintColor >> 8) & 0xFF) / 255f;
        float b = (tintColor & 0xFF) / 255f;

        // On vérifie juste si c'est notre fluide spécial pour appliquer l'effet d'overlay "fluo"
        boolean isPotion = fluidStack.getFluid() == net.frostytrix.fletcherstrestle.fluid.ModFluids.LIQUID_POTION_SOURCE.get();

        if (isPotion) {
            // --- RENDU POTION (Double-Pass) ---
            // Passe 1 : Le fond opaque coloré
            float u0_w = whiteSprite.getU0(); float u1_w = whiteSprite.getU1();
            float v0_w = whiteSprite.getV0(); float v1_w = whiteSprite.getV1();
            builder.addVertex(matrix, 0.125f, currentHeight, 0.875f).setColor(r, g, b, 1.0f).setUv(u0_w, v1_w).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.875f, currentHeight, 0.875f).setColor(r, g, b, 1.0f).setUv(u1_w, v1_w).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.875f, currentHeight, 0.125f).setColor(r, g, b, 1.0f).setUv(u1_w, v0_w).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.125f, currentHeight, 0.125f).setColor(r, g, b, 1.0f).setUv(u0_w, v0_w).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);

            // Passe 2 : Les vagues d'eau translucides par-dessus
            float currentHeightOverlay = currentHeight + 0.0005f;
            float u0_h = waterSprite.getU0(); float u1_h = waterSprite.getU1();
            float v0_h = waterSprite.getV0(); float v1_h = waterSprite.getV1();
            float waterAlpha = 0.35f;
            builder.addVertex(matrix, 0.125f, currentHeightOverlay, 0.875f).setColor(1.0f, 1.0f, 1.0f, waterAlpha).setUv(u0_h, v1_h).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.875f, currentHeightOverlay, 0.875f).setColor(1.0f, 1.0f, 1.0f, waterAlpha).setUv(u1_h, v1_h).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.875f, currentHeightOverlay, 0.125f).setColor(1.0f, 1.0f, 1.0f, waterAlpha).setUv(u1_h, v0_h).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.125f, currentHeightOverlay, 0.125f).setColor(1.0f, 1.0f, 1.0f, waterAlpha).setUv(u0_h, v0_h).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        } else {
            // --- RENDU FLUIDE CLASSIQUE (Eau pure, Lave, Fluides moddés) ---
            // On dessine une seule passe à opacité maximale (1.0f) avec la vraie texture du fluide (ex: l'animation de la lave)
            float u0_f = waterSprite.getU0(); float u1_f = waterSprite.getU1();
            float v0_f = waterSprite.getV0(); float v1_f = waterSprite.getV1();
            builder.addVertex(matrix, 0.125f, currentHeight, 0.875f).setColor(r, g, b, 1.0f).setUv(u0_f, v1_f).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.875f, currentHeight, 0.875f).setColor(r, g, b, 1.0f).setUv(u1_f, v1_f).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.875f, currentHeight, 0.125f).setColor(r, g, b, 1.0f).setUv(u1_f, v0_f).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
            builder.addVertex(matrix, 0.125f, currentHeight, 0.125f).setColor(r, g, b, 1.0f).setUv(u0_f, v0_f).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        }
    }
}