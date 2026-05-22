package net.frostytrix.fletcherstrestle.block.entity.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

// Per-frame snapshot of a shaving horse's render state. The render thread
// reads these fields without touching the BE; extractRenderState fills
// them on the main thread once per frame.
public class ShavingHorseRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public Direction facing = Direction.NORTH;
}
