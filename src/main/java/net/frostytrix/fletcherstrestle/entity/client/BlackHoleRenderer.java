package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.entity.custom.BlackHoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Procedural renderer for the {@link BlackHoleEntity}: an opaque black sphere (event horizon) that
 * writes depth and occludes a flat, Doppler-shifted accretion disk, with a lensing arc, photon ring,
 * soft bloom and a collapse flash. All geometry is generated from maths each frame — no textures.
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {

    private static final int SPHERE_STACKS = 16;
    private static final int SPHERE_SLICES = 24;
    private static final int DISK_SEGMENTS = 96;
    private static final int RING_SEGMENTS = 72;

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BlackHoleEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        float horizon = entity.horizonScale(partialTick);
        float age = entity.age() + partialTick;
        BlackHoleEntity.Phase phase = entity.phase();
        Quaternionf camera = this.entityRenderDispatcher.cameraOrientation();

        if (horizon > 0.001f) {
            // 1) Event horizon — opaque black sphere, flushed first so it writes depth and the
            //    additive glow behind it is correctly occluded.
            VertexConsumer core = buffers.getBuffer(BlackHoleRenderTypes.CORE);
            sphere(pose.last().pose(), core, horizon, 0f, 0f, 0f, 1f);
            if (buffers instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch(BlackHoleRenderTypes.CORE);
            }

            VertexConsumer glow = buffers.getBuffer(BlackHoleRenderTypes.GLOW);

            // 2) Lensed halo — camera-facing wrap-around glow (the disk's far side bent up over
            //    and under the void), warm, streaky, brightest over the top and bottom.
            pose.pushPose();
            pose.mulPose(camera);
            lensedHalo(pose.last().pose(), glow, horizon * 0.98f, horizon * 2.0f, age);
            pose.popPose();

            // 3) Accretion disk — flat front band crossing in front of the sphere, Doppler-shifted.
            pose.pushPose();
            pose.mulPose(new Quaternionf().rotationX(Mth.DEG_TO_RAD * 8f));
            disk(pose.last().pose(), glow, horizon * 0.98f, horizon * 1.95f, age);
            pose.popPose();

            // 4) Photon ring — the crisp thin Einstein ring right at the shadow's edge.
            pose.pushPose();
            pose.mulPose(camera);
            ring2D(pose.last().pose(), glow, horizon * 1.0f, horizon * 1.1f,
                    1.0f, 0.95f, 0.9f, 1.0f);
            pose.popPose();
        }

        // 6) Collapse flash.
        if (phase == BlackHoleEntity.Phase.FLASH) {
            float t = (age - BlackHoleEntity.COLLAPSE_END)
                    / (float) (BlackHoleEntity.FLASH_END - BlackHoleEntity.COLLAPSE_END);
            float alpha = Mth.clamp(1f - t, 0f, 1f);
            float flashR = 1.0f + t * 8f;
            pose.pushPose();
            pose.mulPose(camera);
            filledDisc(pose.last().pose(), buffers.getBuffer(BlackHoleRenderTypes.GLOW),
                    flashR, 1f, 0.96f, 0.88f, alpha);
            pose.popPose();
        }

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    // ---- Geometry ----

    /** UV sphere of solid colour, centred at the origin. */
    private static void sphere(Matrix4f m, VertexConsumer vc, float radius,
                               float r, float g, float b, float a) {
        for (int i = 0; i < SPHERE_STACKS; i++) {
            float th0 = (float) Math.PI * i / SPHERE_STACKS;
            float th1 = (float) Math.PI * (i + 1) / SPHERE_STACKS;
            for (int j = 0; j < SPHERE_SLICES; j++) {
                float ph0 = (float) (2 * Math.PI) * j / SPHERE_SLICES;
                float ph1 = (float) (2 * Math.PI) * (j + 1) / SPHERE_SLICES;
                float[] p00 = sph(radius, th0, ph0);
                float[] p10 = sph(radius, th1, ph0);
                float[] p11 = sph(radius, th1, ph1);
                float[] p01 = sph(radius, th0, ph1);
                tri(m, vc, p00, p10, p11, r, g, b, a);
                tri(m, vc, p00, p11, p01, r, g, b, a);
            }
        }
    }

    private static float[] sph(float radius, float theta, float phi) {
        float st = Mth.sin(theta);
        return new float[]{radius * st * Mth.cos(phi), radius * Mth.cos(theta), radius * st * Mth.sin(phi)};
    }

    /**
     * Flat annulus in the local XZ plane. Inner edge is hot and Doppler-tinted (blue-white on the
     * approaching side, amber on the receding side); the outer edge fades to nothing. Flowing
     * turbulence bands give it motion without a rigid spin.
     */
    private static void disk(Matrix4f m, VertexConsumer vc, float ri, float ro, float age) {
        for (int j = 0; j < DISK_SEGMENTS; j++) {
            float a0 = (float) (2 * Math.PI) * j / DISK_SEGMENTS;
            float a1 = (float) (2 * Math.PI) * (j + 1) / DISK_SEGMENTS;
            float[] c0 = innerColor(a0, turbulence(a0, age));
            float[] c1 = innerColor(a1, turbulence(a1, age));

            float[] i0 = {ri * Mth.cos(a0), 0f, ri * Mth.sin(a0)};
            float[] i1 = {ri * Mth.cos(a1), 0f, ri * Mth.sin(a1)};
            float[] o0 = {ro * Mth.cos(a0), 0f, ro * Mth.sin(a0)};
            float[] o1 = {ro * Mth.cos(a1), 0f, ro * Mth.sin(a1)};

            v(m, vc, i0, c0[0], c0[1], c0[2], c0[3]);
            v(m, vc, o0, c0[0], c0[1], c0[2], 0f);
            v(m, vc, o1, c1[0], c1[1], c1[2], 0f);
            v(m, vc, i0, c0[0], c0[1], c0[2], c0[3]);
            v(m, vc, o1, c1[0], c1[1], c1[2], 0f);
            v(m, vc, i1, c1[0], c1[1], c1[2], c1[3]);
        }
    }

    /** Doppler-tinted warm inner-edge colour {r,g,b,a} for a disk angle (pink-white → amber). */
    private static float[] innerColor(float angle, float turb) {
        float f = (Mth.cos(angle) + 1f) * 0.5f;      // 1 = approaching (cooler), 0 = receding (amber)
        float bright = (0.55f + 0.5f * f) * turb;    // approaching side is brighter
        return new float[]{
                bright,                              // R (warm)
                (0.78f + 0.12f * f) * bright,        // G
                (0.62f + 0.28f * f) * bright,        // B: amber 0.62 → pink-white
                (0.40f + 0.28f * f) * turb           // alpha (kept low to avoid blowing out white)
        };
    }

    /** Orbital brightness flicker, 0..1, from two drifting sine bands. */
    private static float turbulence(float angle, float age) {
        float n = 0.5f + 0.3f * Mth.sin(angle * 7f + age * 0.30f)
                + 0.2f * Mth.sin(angle * 13f - age * 0.20f);
        return Mth.clamp(n, 0.15f, 1f);
    }

    /**
     * Camera-facing wrap-around halo: a fat warm ring hugging the shadow that fakes the disk's
     * lensed image bending over the top and under the bottom. Brightest top/bottom (with a floor so
     * it's a full ring), fine angular streaks for filament detail, fading out to the rim.
     */
    private static void lensedHalo(Matrix4f m, VertexConsumer vc, float ri, float ro, float age) {
        for (int j = 0; j < RING_SEGMENTS; j++) {
            float a0 = (float) (2 * Math.PI) * j / RING_SEGMENTS;
            float a1 = (float) (2 * Math.PI) * (j + 1) / RING_SEGMENTS;
            float in0 = haloAlpha(a0, age);
            float in1 = haloAlpha(a1, age);
            float[] i0 = {ri * Mth.cos(a0), ri * Mth.sin(a0), 0f};
            float[] i1 = {ri * Mth.cos(a1), ri * Mth.sin(a1), 0f};
            float[] o0 = {ro * Mth.cos(a0), ro * Mth.sin(a0), 0f};
            float[] o1 = {ro * Mth.cos(a1), ro * Mth.sin(a1), 0f};
            v(m, vc, i0, 1.0f, 0.86f, 0.80f, in0);
            v(m, vc, o0, 1.0f, 0.86f, 0.80f, 0f);
            v(m, vc, o1, 1.0f, 0.86f, 0.80f, 0f);
            v(m, vc, i0, 1.0f, 0.86f, 0.80f, in0);
            v(m, vc, o1, 1.0f, 0.86f, 0.80f, 0f);
            v(m, vc, i1, 1.0f, 0.86f, 0.80f, in1);
        }
    }

    private static float haloAlpha(float angle, float age) {
        float emphasis = 0.35f + 0.65f * (float) Math.pow(Math.abs(Mth.sin(angle)), 1.4);
        float streak = 0.72f + 0.28f * Mth.sin(angle * 22f + age * 0.12f);
        return Mth.clamp(emphasis * streak, 0f, 1f) * 0.6f;
    }

    /** Uniform-colour annulus in the local XY plane (camera-facing). */
    private static void ring2D(Matrix4f m, VertexConsumer vc, float ri, float ro,
                               float r, float g, float b, float innerA) {
        for (int j = 0; j < RING_SEGMENTS; j++) {
            float a0 = (float) (2 * Math.PI) * j / RING_SEGMENTS;
            float a1 = (float) (2 * Math.PI) * (j + 1) / RING_SEGMENTS;
            float[] i0 = {ri * Mth.cos(a0), ri * Mth.sin(a0), 0f};
            float[] i1 = {ri * Mth.cos(a1), ri * Mth.sin(a1), 0f};
            float[] o0 = {ro * Mth.cos(a0), ro * Mth.sin(a0), 0f};
            float[] o1 = {ro * Mth.cos(a1), ro * Mth.sin(a1), 0f};
            v(m, vc, i0, r, g, b, innerA);
            v(m, vc, o0, r, g, b, 0f);
            v(m, vc, o1, r, g, b, 0f);
            v(m, vc, i0, r, g, b, innerA);
            v(m, vc, o1, r, g, b, 0f);
            v(m, vc, i1, r, g, b, innerA);
        }
    }

    /** Filled disc (triangle fan) in the local XY plane, bright centre fading to the rim. */
    private static void filledDisc(Matrix4f m, VertexConsumer vc, float radius,
                                   float r, float g, float b, float centerA) {
        for (int j = 0; j < RING_SEGMENTS; j++) {
            float a0 = (float) (2 * Math.PI) * j / RING_SEGMENTS;
            float a1 = (float) (2 * Math.PI) * (j + 1) / RING_SEGMENTS;
            float[] c = {0f, 0f, 0f};
            float[] e0 = {radius * Mth.cos(a0), radius * Mth.sin(a0), 0f};
            float[] e1 = {radius * Mth.cos(a1), radius * Mth.sin(a1), 0f};
            v(m, vc, c, r, g, b, centerA);
            v(m, vc, e0, r, g, b, 0f);
            v(m, vc, e1, r, g, b, 0f);
        }
    }

    private static void tri(Matrix4f m, VertexConsumer vc, float[] p1, float[] p2, float[] p3,
                            float r, float g, float b, float a) {
        v(m, vc, p1, r, g, b, a);
        v(m, vc, p2, r, g, b, a);
        v(m, vc, p3, r, g, b, a);
    }

    private static void v(Matrix4f m, VertexConsumer vc, float[] p,
                          float r, float g, float b, float a) {
        vc.addVertex(m, p[0], p[1], p[2]).setColor(r, g, b, a);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
