package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.block.entity.DippingVatBlockEntity;
import net.frostytrix.fletcherstrestle.fluid.ModFluids;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

// 26.1 BE renderer for the dipping vat fluid surface. The 1.21.1
// implementation used MultiBufferSource.getBuffer + VertexConsumer
// quads directly; in 26.1 we go through SubmitNodeCollector's
// `submitCustomGeometry` lambda which exposes the same VertexConsumer.
// The fluid sprite is still water_still off the BLOCK atlas, but the
// per-fluid tint moved off IClientFluidTypeExtensions in 26.1 so we
// compute it from the FluidStack's CUSTOM_DATA potion-id directly:
//   LIQUID_POTION + custom-data "potion" → PotionContents.getColor()
//   anything else                         → flat water blue
public class DippingVatRenderer implements BlockEntityRenderer<DippingVatBlockEntity, DippingVatRenderState> {

    // 26.1: TextureAtlasSprite lookup goes via SpriteGetter from the
    // renderer context. We grab the BLOCK atlas's water_still entry.
    private static final SpriteId WATER_STILL_SPRITE = new SpriteId(
            TextureAtlas.LOCATION_BLOCKS,
            Identifier.withDefaultNamespace("block/water_still"));
    private static final int DEFAULT_WATER_COLOR = 0xFF3F76E4;
    private static final int EMPTY_POTION_COLOR  = 0xFF385DC6;

    private final SpriteGetter sprites;

    public DippingVatRenderer(BlockEntityRendererProvider.Context ctx) {
        this.sprites = ctx.sprites();
    }

    @Override
    public DippingVatRenderState createRenderState() {
        return new DippingVatRenderState();
    }

    @Override
    public void extractRenderState(DippingVatBlockEntity be,
                                   DippingVatRenderState state,
                                   float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(be, state, crumbling);

        FluidStack stack = be.fluidTank.getFluid();
        if (stack.isEmpty()) {
            state.fillRatio = 0F;
            state.isPotion = false;
            return;
        }

        state.fillRatio = (float) stack.getAmount() / (float) be.fluidTank.getCapacity();
        state.isPotion = stack.getFluid() == ModFluids.LIQUID_POTION_SOURCE.get();

        if (state.isPotion) {
            net.minecraft.world.item.component.CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
            int color = EMPTY_POTION_COLOR;
            if (custom != null && custom.contains("potion")) {
                String potionId = custom.copyTag().getString("potion").orElse("");
                if (!potionId.isEmpty()) {
                    Identifier id = Identifier.parse(potionId);
                    color = BuiltInRegistries.POTION.get(id)
                            .map(holder -> 0xFF000000 | (new PotionContents(holder).getColor() & 0xFFFFFF))
                            .orElse(EMPTY_POTION_COLOR);
                }
            }
            state.color = color;
        } else {
            state.color = DEFAULT_WATER_COLOR;
        }
    }

    @Override
    public void submit(DippingVatRenderState state,
                       PoseStack pose,
                       SubmitNodeCollector coll,
                       CameraRenderState camera) {
        if (state.fillRatio <= 0F) return;

        // Fluid sits between Y 2/16 and Y 14/16 inside the vat block, scaled
        // by fillRatio. Anything below this is bare wood floor.
        float minHeight = 0.125F;
        float maxHeight = 0.875F;
        float h = minHeight + (maxHeight - minHeight) * state.fillRatio;

        TextureAtlasSprite sprite = sprites.get(WATER_STILL_SPRITE);
        RenderType layer = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

        int color = state.color;
        boolean twoPass = state.isPotion;
        int light = state.lightCoords;

        // CRITICAL: don't capture pose.last().pose() — PoseStack reuses the
        // same Matrix4f across pushes, so by the time the deferred submit
        // queue flushes the lambda the matrix has been mutated by whichever
        // BE was rendered last. That caused (a) "only one vat shows fluid"
        // (every vat drew at the last vat's position), and (b) "double
        // overlay rising" (the same matrix referenced multiple submitted
        // states). Use the lambda's poseRef snapshot — submitCustomGeometry
        // hands us a *copied* PoseStack.Pose that survives the deferred
        // flush.
        coll.submitCustomGeometry(pose, layer, (poseRef, builder) -> {
            Matrix4f m = poseRef.pose();
            float u0 = sprite.getU0(), u1 = sprite.getU1();
            float v0 = sprite.getV0(), v1 = sprite.getV1();
            float r = ((color >> 16) & 0xFF) / 255F;
            float g = ((color >> 8) & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;

            // Pass 1 — opaque colour fluid surface (single quad, top-facing).
            quad(builder, m, 0.125F, h, 0.875F, r, g, b, 1.0F, u0, v1, light);
            quad(builder, m, 0.875F, h, 0.875F, r, g, b, 1.0F, u1, v1, light);
            quad(builder, m, 0.875F, h, 0.125F, r, g, b, 1.0F, u1, v0, light);
            quad(builder, m, 0.125F, h, 0.125F, r, g, b, 1.0F, u0, v0, light);

            // Pass 2 (potion only) — translucent moving-water ripple over
            // the colour bed so dipped potions feel alive.
            if (twoPass) {
                float h2 = h + 0.0005F;
                float a = 0.35F;
                quad(builder, m, 0.125F, h2, 0.875F, 1F, 1F, 1F, a, u0, v1, light);
                quad(builder, m, 0.875F, h2, 0.875F, 1F, 1F, 1F, a, u1, v1, light);
                quad(builder, m, 0.875F, h2, 0.125F, 1F, 1F, 1F, a, u1, v0, light);
                quad(builder, m, 0.125F, h2, 0.125F, 1F, 1F, 1F, a, u0, v0, light);
            }
        });
    }

    private static void quad(VertexConsumer builder, Matrix4f matrix,
                             float x, float y, float z,
                             float r, float g, float b, float a,
                             float u, float v, int light) {
        builder.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0F, 1F, 0F);
    }
}
