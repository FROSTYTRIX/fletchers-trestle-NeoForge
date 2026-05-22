package net.frostytrix.fletcherstrestle.entity.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

// Per-frame snapshot of the eagle's animation state. The renderer's
// extractRenderState fills these from the entity once per frame; the
// model reads them in setupAnim without touching the entity on the
// render thread.
public class EagleRenderState extends LivingEntityRenderState {
    public boolean flying;
    /** horizontal motion magnitude (x/z), used to scale the in-flight
     *  body-pitch and wing-flap amplitude. */
    public float horizontalSpeed;
}
