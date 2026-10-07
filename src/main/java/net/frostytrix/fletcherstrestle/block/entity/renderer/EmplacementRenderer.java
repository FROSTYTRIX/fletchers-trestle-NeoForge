package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.block.entity.EmplacementBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the emplacement's crossbow lying flat on its pivot, turned to the synced
 * aim, and its quiver propped against the base.
 *
 * <p>The aim only arrives every couple of degrees, so the drawn angle eases toward
 * it each frame: the crossbow slews instead of snapping.</p>
 */
public class EmplacementRenderer implements BlockEntityRenderer<EmplacementBlockEntity> {

    /** How quickly the drawn aim catches up with the synced one, per frame. */
    private static final float SLEW = 0.25f;
    /** Turns the crossbow sprite's diagonal so its front points along the aim. */
    private static final float SPRITE_SPIN = -45.0f;
    private static final float CROSSBOW_SCALE = 0.9f;

    /** Quiver placement, in block units from the block's centre, before the facing turn. */
    private static final float QUIVER_X = 0.32f;
    private static final float QUIVER_Y = 0.35f;
    private static final float QUIVER_Z = 0.22f;
    private static final float QUIVER_SCALE = 0.5f;

    private final Map<EmplacementBlockEntity, float[]> drawnAim = new WeakHashMap<>();

    public EmplacementRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EmplacementBlockEntity post, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        var itemRenderer = Minecraft.getInstance().getItemRenderer();

        ItemStack crossbow = post.getCrossbow();
        if (!crossbow.isEmpty()) {
            float[] aim = drawnAim.computeIfAbsent(post, p -> new float[]{p.getAimYaw(), p.getAimPitch()});
            aim[0] += Mth.wrapDegrees(post.getAimYaw() - aim[0]) * SLEW;
            aim[1] += (post.getAimPitch() - aim[1]) * SLEW;

            poseStack.pushPose();
            poseStack.translate(0.5, EmplacementBlockEntity.PIVOT_HEIGHT, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-aim[0]));
            poseStack.mulPose(Axis.XP.rotationDegrees(aim[1]));
            // Lay the sprite flat, its top toward +Z, which is "forward" at yaw 0.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(SPRITE_SPIN));
            // Item models carry a half turn about Y in the FIXED context; undo it.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.scale(CROSSBOW_SCALE, CROSSBOW_SCALE, CROSSBOW_SCALE);
            itemRenderer.renderStatic(crossbow, ItemDisplayContext.FIXED, light, overlay,
                    poseStack, buffer, post.getLevel(), 0);
            poseStack.popPose();
        }

        ItemStack quiver = post.getQuiver();
        if (!quiver.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-post.restYaw()));
            poseStack.translate(QUIVER_X, QUIVER_Y, QUIVER_Z);
            poseStack.scale(QUIVER_SCALE, QUIVER_SCALE, QUIVER_SCALE);
            itemRenderer.renderStatic(quiver, ItemDisplayContext.FIXED, light, overlay,
                    poseStack, buffer, post.getLevel(), 1);
            poseStack.popPose();
        }
    }

    /** The crossbow sits above the block, so the culling box has to reach it. */
    @Override
    public AABB getRenderBoundingBox(EmplacementBlockEntity post) {
        return new AABB(post.getBlockPos()).inflate(0.5, 0.0, 0.5).expandTowards(0.0, 1.0, 0.0);
    }
}
