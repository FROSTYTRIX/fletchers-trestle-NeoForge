package net.frostytrix.fletcherstrestle.client.renderer;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;

// Per-frame snapshot of which shaft/head/fletching the modular arrow
// was assembled from, plus the potion tint when the head is a glass
// vial carrying potion contents. extractRenderState fills these from
// the entity's ArrowAssembly + POTION_CONTENTS; ModularArrowRenderer
// reads them to pick textures + a tint without touching the entity on
// the render thread.
//
// `currentLayer` is set transiently inside ModularArrowRenderer.submit
// so the same ArrowModel can be drawn 3–4 times (shaft, head, fletching,
// optional liquid) without the model needing to know about the per-pass
// material id.
public class ModularArrowRenderState extends ArrowRenderState {
    public String shaft = "oak";
    public String head = "flint";
    public String fletching = "feather";

    public Layer currentLayer = Layer.SHAFT;

    /** True when head is a glass vial AND POTION_CONTENTS is set. */
    public boolean hasLiquid = false;
    /** Packed ARGB tint for the liquid layer; only read when hasLiquid. */
    public int liquidColor = 0xFF385DC6;

    public enum Layer { SHAFT, HEAD, FLETCHING, LIQUID }
}
