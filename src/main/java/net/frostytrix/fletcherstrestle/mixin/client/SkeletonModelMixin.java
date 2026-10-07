package net.frostytrix.fletcherstrestle.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets a skeleton hold a modular bow like a bow.
 *
 * <p>{@code SkeletonModel} checks for the vanilla bow by identity
 * ({@code is(Items.BOW)}) twice: once to raise the aiming pose, and once to decide
 * that a skeleton holding anything else should swing its arms out like a zombie.
 * Both checks count a modular bow as a bow here. Strays and bogged use this model
 * too.</p>
 */
@Mixin(SkeletonModel.class)
public abstract class SkeletonModelMixin {

    @WrapOperation(method = {"prepareMobModel", "setupAnim"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean fletcherstrestle$modularBowIsABow(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item)
                || (item == Items.BOW && stack.getItem() instanceof ModularBowItem);
    }
}
