package net.frostytrix.fletcherstrestle.block.entity.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

// Per-frame snapshot of the fluid surface to draw. extractRenderState
// fills these once; submit reads them on the render thread without
// touching the BE.
public class DippingVatRenderState extends BlockEntityRenderState {
    /** 0..1 fill ratio of the tank. */
    public float fillRatio;
    /** ARGB tint applied to the fluid quad. 0xFFRRGGBB. Alpha is forced
     *  opaque for clean colour blending against the wood inside. */
    public int color = 0xFF3F76E4;
    /** True when this is our LIQUID_POTION fluid carrying a potion id —
     *  triggers the two-pass render (opaque colour bed + translucent
     *  water ripple overlay) so dipped potions look distinct from a
     *  flat pool of water. */
    public boolean isPotion;
}
