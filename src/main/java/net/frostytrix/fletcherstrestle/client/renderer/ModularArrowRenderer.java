package net.frostytrix.fletcherstrestle.client.renderer;

// TODO(port-26.1): Rewrite for the new deferred render pipeline.
//
// In 26.1, ArrowRenderer<T extends AbstractArrow, S extends ArrowRenderState>
// uses a 3-method extract/submit pattern. RenderType is also gone,
// replaced by RenderPipeline. The 1.21.1 implementation drew the
// modular arrow as three layered quads (shaft, fletching, head) plus
// an optional potion-tinted overlay for glass-vial arrows.
// See git history pre-port for the original implementation.
//
// Stubbed so the rest of the mod compiles; registration in
// ModClientEvents is commented out alongside this.
public final class ModularArrowRenderer {
    private ModularArrowRenderer() {}
}
