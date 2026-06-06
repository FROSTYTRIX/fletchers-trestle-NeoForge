package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Custom render types for the black hole's procedural geometry. Subclasses {@link RenderType} purely
 * to reach the engine's {@code protected} blend/depth state shards.
 *
 * <ul>
 *   <li>{@link #GLOW} — untextured position+colour, additive blending, no depth write: the accretion
 *       disk and photon ring (light adds up, so brightness stacks toward the core).</li>
 *   <li>{@link #CORE} — untextured position+colour, opaque, depth-writing: the event-horizon sphere,
 *       which must occlude the glow behind it for the 3D read.</li>
 * </ul>
 */
public final class BlackHoleRenderTypes extends RenderType {
    private BlackHoleRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    public static final RenderType GLOW = create(
            "fletcherstrestle:black_hole_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    public static final RenderType CORE = create(
            "fletcherstrestle:black_hole_core",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));
}
