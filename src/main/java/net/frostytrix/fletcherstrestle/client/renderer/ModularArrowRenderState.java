package net.frostytrix.fletcherstrestle.client.renderer;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;

// Carries the per-frame snapshot of which shaft/head/fletching the modular
// arrow was assembled from. extractRenderState fills these from the entity's
// ArrowAssembly component once per frame; ModularArrowRenderer reads them
// to pick a texture without touching the entity on the render thread.
//
// `currentLayer` is set transiently inside ModularArrowRenderer.submit so
// the same ArrowModel can be drawn three times (shaft, then head, then
// fletching) without anything else in the pipeline needing to know.
public class ModularArrowRenderState extends ArrowRenderState {
    public String shaft = "oak";
    public String head = "flint";
    public String fletching = "feather";

    public Layer currentLayer = Layer.SHAFT;

    public enum Layer { SHAFT, HEAD, FLETCHING }
}
