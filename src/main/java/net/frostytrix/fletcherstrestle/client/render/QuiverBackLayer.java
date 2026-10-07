package net.frostytrix.fletcherstrestle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hangs the player's quiver across their back.
 *
 * <p>It draws the quiver's own item model, which already carries the selected
 * arrow (see {@code QuiverBakedModel}), so the back and the inventory icon
 * always match.</p>
 *
 * <p>Placement is all in the constants below, in body-model space, where one unit
 * is one block, +Y points DOWN the back and +Z points out of it.</p>
 */
public class QuiverBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Centre of the quiver on the back: halfway down the 12-pixel torso, just off its surface. */
    private static final float OFFSET_X = 0.0f;
    private static final float OFFSET_Y = 0.35f;
    private static final float OFFSET_Z = 0.17f;
    /** The item models render a block wide; the torso is half a block. */
    private static final float SCALE = 0.62f;
    /** Extra tilt on top of the sprite's own diagonal, in degrees. */
    private static final float TILT = 0.0f;

    private final ItemRenderer itemRenderer;

    public QuiverBackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                           ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!FletcherConfig.QUIVER_ON_BACK.get() || player.isInvisible()) return;
        // An elytra's wings fold over the same spot.
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;

        ItemStack quiver = findQuiver(player);
        if (quiver.isEmpty()) return;

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);
        // Entity models render flipped on X and Y, which is a half turn about Z;
        // turning back puts the sprite upright.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f + TILT));
        poseStack.scale(SCALE, SCALE, SCALE);
        itemRenderer.renderStatic(player, quiver, ItemDisplayContext.FIXED, false, poseStack, buffer,
                player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
        poseStack.popPose();
    }

    /** The quiver the bow would draw from: the first one in the inventory. */
    private static ItemStack findQuiver(AbstractClientPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof ModularQuiverItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
