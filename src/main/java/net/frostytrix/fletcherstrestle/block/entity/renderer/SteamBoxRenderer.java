package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.block.entity.SteamBoxBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * Renders the Steam Box's water as a dynamic surface whose height tracks the
 * fluid tank fill ratio: same technique as {@link DippingVatRenderer}, so the
 * blockstate no longer needs discrete water-level models.
 */
public class SteamBoxRenderer implements BlockEntityRenderer<SteamBoxBlockEntity> {

    // Interior cavity of the basin (matches the model: walls at 1..2 / 14..15).
    private static final float MIN_XZ = 0.125f; // 2/16
    private static final float MAX_XZ = 0.875f; // 14/16
    // Water surface sits between the basin floor (2/16) and the full mark (9/16).
    private static final float MIN_HEIGHT = 0.135f;
    private static final float MAX_HEIGHT = 0.5625f; // 9/16

    public SteamBoxRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SteamBoxBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FluidStack fluidStack = blockEntity.fluidTank.getFluid();
        if (fluidStack.isEmpty()) return;

        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite waterSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(clientFluid.getStillTexture(fluidStack));

        float fillRatio = (float) fluidStack.getAmount() / (float) blockEntity.fluidTank.getCapacity();
        float currentHeight = MIN_HEIGHT + ((MAX_HEIGHT - MIN_HEIGHT) * fillRatio);

        // Biome-accurate water tint (the still-water texture is greyscale).
        int tintColor = 0xFF3F76E4;
        if (blockEntity.getLevel() != null) {
            tintColor = 0xFF000000 | BiomeColors.getAverageWaterColor(blockEntity.getLevel(), blockEntity.getBlockPos());
        }
        float r = ((tintColor >> 16) & 0xFF) / 255f;
        float g = ((tintColor >> 8) & 0xFF) / 255f;
        float b = (tintColor & 0xFF) / 255f;

        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        Matrix4f matrix = poseStack.last().pose();

        float u0 = waterSprite.getU0();
        float u1 = waterSprite.getU1();
        float v0 = waterSprite.getV0();
        float v1 = waterSprite.getV1();

        builder.addVertex(matrix, MIN_XZ, currentHeight, MAX_XZ).setColor(r, g, b, 1.0f).setUv(u0, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        builder.addVertex(matrix, MAX_XZ, currentHeight, MAX_XZ).setColor(r, g, b, 1.0f).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        builder.addVertex(matrix, MAX_XZ, currentHeight, MIN_XZ).setColor(r, g, b, 1.0f).setUv(u1, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        builder.addVertex(matrix, MIN_XZ, currentHeight, MIN_XZ).setColor(r, g, b, 1.0f).setUv(u0, v0).setOverlay(packedOverlay).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
    }
}
