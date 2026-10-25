package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.EmplacementBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the moving parts of an emplacement: the swivel yoke turned to the
 * synced aim, the cradle pitched on its trunnion pin with the crossbow lying in
 * it, and the quiver hung on the back right leg.
 *
 * <p>The yoke and cradle are block models of their own ({@code
 * block/emplacement_yoke} and {@code block/emplacement_cradle}), made, like the
 * stand, by {@code tools/garrison/make_garrison_art.py}. The aim only arrives
 * every couple of degrees, so the drawn angle eases toward it each frame: the
 * crossbow slews instead of snapping.</p>
 */
public class EmplacementRenderer implements BlockEntityRenderer<EmplacementBlockEntity> {

    public static final ModelResourceLocation YOKE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "block/emplacement_yoke"));
    public static final ModelResourceLocation CRADLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "block/emplacement_cradle"));

    /** How quickly the drawn aim catches up with the synced one, per frame. */
    private static final float SLEW = 0.25f;
    /** Turns the crossbow sprite's diagonal so its front points along the aim. */
    private static final float SPRITE_SPIN = -45.0f;
    private static final float CROSSBOW_SCALE = 0.9f;

    /**
     * Where the quiver's centre sits, in block pixels with the stand facing north:
     * against the back right leg, low enough that its foot (3.9 px down, the arrow
     * tip) stands on the leg's iron foot, and out far enough to clear the leg.
     */
    private static final float QUIVER_X = 13.9f;
    private static final float QUIVER_Y = 4.95f;
    private static final float QUIVER_Z = 12.0f;
    /** Leans the quiver along the leg it hangs on. */
    private static final float QUIVER_LEAN = 22.5f;
    private static final float QUIVER_SCALE = 0.5f;
    /** Turns the quiver sprite, drawn bottom-left to top-right, upright. */
    private static final float QUIVER_SPIN = -45.0f;

    private final Map<EmplacementBlockEntity, float[]> drawnAim = new WeakHashMap<>();

    public EmplacementRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EmplacementBlockEntity post, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        float[] aim = drawnAim.computeIfAbsent(post, p -> new float[]{p.getAimYaw(), p.getAimPitch()});
        aim[0] += Mth.wrapDegrees(post.getAimYaw() - aim[0]) * SLEW;
        aim[1] += (post.getAimPitch() - aim[1]) * SLEW;

        poseStack.pushPose();
        // The yoke turns about the block's vertical axis; its model faces north, which is yaw 180.
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - aim[0]));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderPart(YOKE, poseStack, buffer, light, overlay);

        // The cradle pitches on the trunnion pin; pitch is positive looking down.
        poseStack.translate(0.5, EmplacementBlockEntity.TRUNNION_HEIGHT, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(-aim[1]));
        poseStack.translate(-0.5, -EmplacementBlockEntity.TRUNNION_HEIGHT, -0.5);
        renderPart(CRADLE, poseStack, buffer, light, overlay);

        ItemStack crossbow = post.getCrossbow();
        if (!crossbow.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, EmplacementBlockEntity.PIVOT_HEIGHT, 0.5);
            // Back to a frame whose forward is +Z, the way the sprite is laid out below.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            // Lay the sprite flat, its top toward +Z.
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(SPRITE_SPIN));
            // Item models carry a half turn about Y in the FIXED context; undo it.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.scale(CROSSBOW_SCALE, CROSSBOW_SCALE, CROSSBOW_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(crossbow, ItemDisplayContext.FIXED, light, overlay,
                    poseStack, buffer, post.getLevel(), 0);
            poseStack.popPose();
        }
        poseStack.popPose();

        ItemStack quiver = post.getQuiver();
        if (!quiver.isEmpty()) {
            poseStack.pushPose();
            // Into the stand's own frame, as its blockstate turns it.
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-(post.restYaw() + 180.0f)));
            poseStack.translate(-0.5, 0.0, -0.5);
            poseStack.translate(QUIVER_X / 16.0f, QUIVER_Y / 16.0f, QUIVER_Z / 16.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(QUIVER_LEAN));
            // Face it outward; FIXED adds a half turn of its own.
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
            // Item sprites are drawn on the diagonal: stand it upright along the leg.
            poseStack.mulPose(Axis.ZP.rotationDegrees(QUIVER_SPIN));
            poseStack.scale(QUIVER_SCALE, QUIVER_SCALE, QUIVER_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(quiver, ItemDisplayContext.FIXED, light, overlay,
                    poseStack, buffer, post.getLevel(), 1);
            poseStack.popPose();
        }
    }

    private static void renderPart(ModelResourceLocation id, PoseStack poseStack, MultiBufferSource buffer,
                                   int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(id);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffer.getBuffer(Sheets.cutoutBlockSheet()), null, model, 1.0f, 1.0f, 1.0f, light, overlay);
    }

    /** The yoke and crossbow stand above the block, so the culling box has to reach them. */
    @Override
    public AABB getRenderBoundingBox(EmplacementBlockEntity post) {
        return new AABB(post.getBlockPos()).inflate(0.5, 0.0, 0.5).expandTowards(0.0, 1.0, 0.0);
    }
}
