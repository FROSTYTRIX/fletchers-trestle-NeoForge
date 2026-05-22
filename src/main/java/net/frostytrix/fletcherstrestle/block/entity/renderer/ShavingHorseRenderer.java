package net.frostytrix.fletcherstrestle.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.block.custom.ShavingHorseBlock;
import net.frostytrix.fletcherstrestle.block.entity.ShavingHorseBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// 26.1 BlockEntityRenderer<T, S> — draws the item currently clamped in
// the shaving horse on top of the block so the player can see what
// they're shaving. The 1.21.1 implementation rendered an ItemStack
// directly inside render(); the new deferred pipeline splits that into
// extractRenderState (snapshot the ItemStack via ItemModelResolver) and
// submit (issue the draw against the SubmitNodeCollector).
public class ShavingHorseRenderer implements BlockEntityRenderer<ShavingHorseBlockEntity, ShavingHorseRenderState> {

    private final ItemModelResolver itemModelResolver;

    public ShavingHorseRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemModelResolver = ctx.itemModelResolver();
    }

    @Override
    public ShavingHorseRenderState createRenderState() {
        return new ShavingHorseRenderState();
    }

    @Override
    public void extractRenderState(ShavingHorseBlockEntity be,
                                   ShavingHorseRenderState state,
                                   float partialTick,
                                   Vec3 cameraPos,
                                   ModelFeatureRenderer.CrumblingOverlay crumbling) {
        // BlockEntityRenderState.extractBase fills blockPos / blockState /
        // blockEntityType / lightCoords / breakProgress for us.
        BlockEntityRenderState.extractBase(be, state, crumbling);
        state.facing = be.getBlockState().getValue(ShavingHorseBlock.FACING);
        ItemStack stack = be.itemHandler.getStackInSlot(0);
        // Snapshot the held stack into an ItemStackRenderState the render
        // thread can consume without touching the BE. updateForNonLiving
        // is the BE-friendly path (no LivingEntity in scope).
        if (stack.isEmpty()) {
            state.item.clear();
        } else if (be.getLevel() != null) {
            // updateForNonLiving internally calls entity.level() / entity.getId(),
            // so passing null throws an NPE silently and the item never renders.
            // updateForTopItem is the lower-level path that accepts a null
            // ItemOwner — exactly what we need for a block-entity-held stack.
            itemModelResolver.updateForTopItem(
                    state.item, stack, ItemDisplayContext.FIXED,
                    be.getLevel(), null, 0);
        }
    }

    @Override
    public void submit(ShavingHorseRenderState state,
                       PoseStack pose,
                       SubmitNodeCollector coll,
                       CameraRenderState camera) {
        if (state.item.isEmpty()) return;

        pose.pushPose();
        // Center the item on top of the block, tilted up so it looks like
        // it's clamped between the jaws of the horse. The angles match
        // the 1.21.1 implementation so the visual stays consistent.
        pose.translate(0.5, 1.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.scale(0.9F, 0.9F, 0.9F);
        pose.translate(0.0, -0.25, 0.0);

        // 5th int is OUTLINE color, not tint. ItemFeatureRenderer.NO_TINT
        // (-1) requests the white-outline-overlay-everywhere look used for
        // glowing entities, which is why the log was rendering on top of
        // blocks as a solid white silhouette. 0 = no outline.
        state.item.submit(pose, coll, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        pose.popPose();
    }
}
