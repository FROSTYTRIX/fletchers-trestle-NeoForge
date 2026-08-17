package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.entity.NailBlockEntity;
import net.frostytrix.fletcherstrestle.component.GarlandColours;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the garlands strung from a nail: a drooping cord with triangular
 * pennants hanging off it, coloured by the feathers the garland was woven from.
 *
 * <p>Only the nail that owns a span draws it, so a garland is never rendered
 * twice from both ends. The cord uses vanilla's leash render type, and the sag
 * is a parabola, which is close enough to a real catenary at these lengths.</p>
 */
public class NailRenderer implements BlockEntityRenderer<NailBlockEntity> {

    private static final ResourceLocation PENNANT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/entity/garland_pennant.png");

    /** Segments used to draw the cord. Vanilla leashes use 24. */
    private static final int CORD_SEGMENTS = 24;
    /**
     * How far the cord droops, eased between a short span and a long one. Longer
     * garlands carry more cord, so they hang deeper.
     */
    private static final float SAG_CLOSE = 0.35f;
    private static final float SAG_FAR = 0.5f;
    /** Span at which the garland is considered fully stretched. */
    private static final float SAG_RANGE = 12.0f;
    /** Half-thickness of the cord. Also how far the pennants hang below it. */
    private static final double CORD_RADIUS = 0.03;
    /** Pennant size in blocks. */
    private static final float PENNANT_WIDTH = 0.22f;
    private static final float PENNANT_HEIGHT = 0.3f;
    /** Pennants per block of span. */
    private static final float PENNANT_DENSITY = 4.0f;
    /** How far back from the block centre the cord ties on, into the nail head. */
    private static final double ANCHOR_INSET = 0.33;

    public NailRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NailBlockEntity nail, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockPos origin = nail.getBlockPos();

        // A garland anchored to this nail trails from the player's hand until
        // they pick its second nail, the way a lead does.
        renderPending(nail, origin, partialTick, poseStack, bufferSource, packedLight);

        if (nail.getSpans().isEmpty()) {
            return;
        }

        for (NailBlockEntity.Span span : nail.getSpans()) {
            // Tie the cord to the nail heads, not the block centres, so it runs
            // back into the wall each nail is driven into.
            Vec3 start = new Vec3(0.5, 0.5, 0.5).add(anchorInset(nail.getBlockState()));
            Vec3 farInset = nail.getLevel() == null
                    ? Vec3.ZERO
                    : anchorInset(nail.getLevel().getBlockState(span.target()));
            Vec3 end = new Vec3(
                    span.target().getX() - origin.getX() + 0.5,
                    span.target().getY() - origin.getY() + 0.5,
                    span.target().getZ() - origin.getZ() + 0.5).add(farInset);

            GarlandColours colours = span.garland().get(ModDataComponents.GARLAND_COLOURS.get());
            if (colours == null) {
                colours = GarlandColours.EMPTY;
            }
            renderSpan(poseStack, bufferSource, packedLight, start, end, colours);
        }
    }

    private void renderSpan(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                            Vec3 start, Vec3 end, GarlandColours colours) {
        float span = (float) start.distanceTo(end);
        float slack = 1.0f - Mth.clamp(span / SAG_RANGE, 0f, 1f);
        float droop = Mth.lerp(slack, SAG_FAR, SAG_CLOSE);

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        // Cord, drawn as two ribbons crossed at right angles. That gives it real
        // volume from every angle, and means the pennants hanging beneath it are
        // never coplanar with it, which is what caused the z-fighting.
        Vec3 along = end.subtract(start);
        Vec3 flat = new Vec3(along.x, 0, along.z);
        Vec3 perp = flat.lengthSqr() < 1.0E-6
                ? new Vec3(1, 0, 0)
                : new Vec3(-flat.z, 0, flat.x).normalize();

        VertexConsumer cord = bufferSource.getBuffer(RenderType.leash());
        // Horizontal ribbon.
        for (int i = 0; i <= CORD_SEGMENTS; i++) {
            Vec3 p = pointOn(start, end, (float) i / CORD_SEGMENTS, droop);
            cordVertex(cord, pose, p.subtract(perp.scale(CORD_RADIUS)), light);
            cordVertex(cord, pose, p.add(perp.scale(CORD_RADIUS)), light);
        }
        // Vertical ribbon, back along the span so the strip closes cleanly.
        for (int i = CORD_SEGMENTS; i >= 0; i--) {
            Vec3 p = pointOn(start, end, (float) i / CORD_SEGMENTS, droop);
            cordVertex(cord, pose, p.add(0, CORD_RADIUS, 0), light);
            cordVertex(cord, pose, p.subtract(0, CORD_RADIUS, 0), light);
        }

        // Pennants, spaced along the cord and spread through the colour mix.
        int count = Mth.clamp(Mth.floor(span * PENNANT_DENSITY), 3, 64);
        VertexConsumer flags = bufferSource.getBuffer(RenderType.entityCutoutNoCull(PENNANT_TEXTURE));

        // Half a pennant's width expressed as a fraction of the span, so the top
        // corners can be sampled from the curve itself and follow its slope.
        float halfStep = span < 1.0E-4f ? 0.01f : (PENNANT_WIDTH / 2f) / span;

        for (int i = 0; i < count; i++) {
            float t = (i + 0.5f) / count;
            Vec3 left = pointOn(start, end, Math.max(0f, t - halfStep), droop)
                    .subtract(0, CORD_RADIUS, 0);
            Vec3 right = pointOn(start, end, Math.min(1f, t + halfStep), droop)
                    .subtract(0, CORD_RADIUS, 0);
            int colour = colours.colourAt(i, count);
            addPennant(flags, pose, left, right,
                    (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, light);
        }

        poseStack.popPose();
    }

    /**
     * Draws the garland the local player is currently holding, running from this
     * nail to their hand, if they have anchored it here.
     */
    private void renderPending(NailBlockEntity nail, BlockPos origin, float partialTick,
                               PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || nail.getLevel() == null) {
            return;
        }
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
            if (!(held.getItem() instanceof net.frostytrix.fletcherstrestle.item.custom.GarlandItem)) {
                continue;
            }
            var anchor = held.get(ModDataComponents.GARLAND_ANCHOR.get());
            if (anchor == null
                    || !anchor.dimension().equals(nail.getLevel().dimension())
                    || !anchor.pos().equals(origin)) {
                continue;
            }
            // Same hold point vanilla uses for leads, so it sits in the hand.
            Vec3 hand3d = player.getRopeHoldPosition(partialTick)
                    .subtract(origin.getX(), origin.getY(), origin.getZ());
            Vec3 start = new Vec3(0.5, 0.5, 0.5).add(anchorInset(nail.getBlockState()));

            GarlandColours colours = held.get(ModDataComponents.GARLAND_COLOURS.get());
            renderSpan(poseStack, bufferSource, light, start, hand3d,
                    colours == null ? GarlandColours.EMPTY : colours);
            return;
        }
    }

    /** Point along the span at {@code t}, pulled down by a parabolic sag. */
    private static Vec3 pointOn(Vec3 start, Vec3 end, float t, float droop) {
        double x = Mth.lerp(t, start.x, end.x);
        double y = Mth.lerp(t, start.y, end.y) - droop * 4.0 * t * (1.0 - t);
        double z = Mth.lerp(t, start.z, end.z);
        return new Vec3(x, y, z);
    }

    /**
     * A triangle hanging point-down, with its top edge running between two
     * points taken from the curve so it sits flush on the sagging cord. Drawn as
     * a degenerate quad (the apex twice).
     */
    private static void addPennant(VertexConsumer buffer, Matrix4f pose, Vec3 left, Vec3 right,
                                   int r, int g, int b, int light) {
        // The point hangs straight down from the middle of the top edge.
        double midX = (left.x + right.x) / 2.0;
        double midY = (left.y + right.y) / 2.0;
        double midZ = (left.z + right.z) / 2.0;
        float apexY = (float) midY - PENNANT_HEIGHT;

        vertex(buffer, pose, (float) left.x, (float) left.y, (float) left.z, 0f, 0f, r, g, b, light);
        vertex(buffer, pose, (float) right.x, (float) right.y, (float) right.z, 1f, 0f, r, g, b, light);
        vertex(buffer, pose, (float) midX, apexY, (float) midZ, 1f, 1f, r, g, b, light);
        vertex(buffer, pose, (float) midX, apexY, (float) midZ, 0f, 1f, r, g, b, light);
    }

    /**
     * How far the cord ties back from the block centre, toward the face the nail
     * is driven into, so the garland meets the nail head instead of hanging in
     * the middle of the block.
     */
    private static Vec3 anchorInset(net.minecraft.world.level.block.state.BlockState state) {
        if (!(state.getBlock() instanceof net.frostytrix.fletcherstrestle.block.custom.NailBlock)) {
            return Vec3.ZERO;
        }
        var support = net.frostytrix.fletcherstrestle.block.custom.NailBlock.supportFace(state);
        return new Vec3(support.getStepX(), support.getStepY(), support.getStepZ()).scale(ANCHOR_INSET);
    }

    private static void cordVertex(VertexConsumer buffer, Matrix4f pose, Vec3 p, int light) {
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(70, 55, 40, 255)
                .setLight(light);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, float x, float y, float z,
                               float u, float v, int r, int g, int b, int light) {
        buffer.addVertex(pose, x, y, z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0f, 1f, 0f);
    }
}
