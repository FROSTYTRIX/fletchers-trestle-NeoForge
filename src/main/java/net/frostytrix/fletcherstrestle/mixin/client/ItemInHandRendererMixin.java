package net.frostytrix.fletcherstrestle.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Vanilla's {@code ItemInHandRenderer.renderArmWithItem}'s {@code case BOW}
 * computes a normalised draw progress as {@code f8 / 20.0F}, then feeds
 * that into a Z-axis scale ({@code 1 + f12 * 0.2}) — the visible
 * "stretching" you see as the bow is drawn back.
 *
 * <p>The 20.0F divisor is hardcoded for the vanilla bow's 20-tick draw.
 * Our modular bow's draw time varies per limb material (10 for birch,
 * 35 for dark oak, …). Without intervention the stretch animation runs
 * on a 20-tick schedule while the texture-stage transitions and the
 * FOV zoom run on the per-limb schedule — visually disjoint.</p>
 *
 * <p>Fix: when the held stack is a {@link ModularBowItem}, replace the
 * 20.0F divisor with the limb-specific draw time. {@code 20.0F} is
 * unique to the BOW case in this method (the crossbow case uses
 * {@code CrossbowItem.getChargeDuration} which is already per-item;
 * the brush + spear cases use 10.0F), so a plain
 * {@link ModifyConstant} without a slice is safe.</p>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @ModifyConstant(method = "renderArmWithItem", constant = @Constant(floatValue = 20.0F))
    private float fletcherstrestle$bowDrawDivisor(float original, @Local(argsOnly = true) ItemStack stack) {
        if (stack.getItem() instanceof ModularBowItem bow) {
            float drawTime = bow.getDrawTime(stack);
            // Sanity-floor at 1 tick so a misconfigured material can't
            // produce a divide-by-zero or negative scale.
            return drawTime < 1.0F ? 1.0F : drawTime;
        }
        return original;
    }
}
