package net.frostytrix.fletcherstrestle.mixin.client;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Jungle limb "agility": the player walks at full speed while drawing.
 *
 * <p>Vanilla {@code LocalPlayer.aiStep()} slows the local player while using an
 * item by multiplying the movement impulses by {@code 0.2F}:
 * <pre>
 *   if (this.isUsingItem() && !this.isPassenger()) {
 *       this.input.leftImpulse *= 0.2F;
 *       this.input.forwardImpulse *= 0.2F;
 *   }
 * </pre>
 * Both {@code 0.2F} constants are unique to this block within {@code aiStep},
 * so a plain {@link ModifyConstant} (same technique as the bow-draw divisor
 * mixin) can flip them to {@code 1.0F} when the player is drawing an agility
 * bow: cancelling the slowdown. The callback only fires inside the
 * {@code isUsingItem()} guard, so the held item is always the one being used.</p>
 *
 * <p>This replaces an earlier MOVEMENT_SPEED-attribute approach, which distorted
 * the FOV (vanilla scales FOV by movement speed). Touching only the input
 * impulses leaves speed, and thus FOV: untouched.</p>
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerAgilityMixin {

    @ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 0.2F), require = 0)
    private float fletcherstrestle$agilityNoDrawSlowdown(float original) {
        ItemStack use = ((LocalPlayer) (Object) this).getUseItem();
        if (use.getItem() instanceof ModularBowItem) {
            BowAssembly assembly = use.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (assembly != null
                    && Materials.bowLimb(assembly.limbMaterial()).stats().agility()) {
                return 1.0F;
            }
        }
        return original;
    }
}
