package net.frostytrix.fletcherstrestle.menu;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.config.FletcherConfig;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.frostytrix.fletcherstrestle.material.BowStringDef;
import net.frostytrix.fletcherstrestle.material.MaterialResolver;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Maintenance at the Fletching Table: restringing and retuning a finished weapon.
 *
 * <p>A finished bow or crossbow goes in the <b>riser slot</b>, where its riser
 * would sit, with the limb slots left empty. Then:</p>
 * <ul>
 *   <li>a string in the string slot <b>restrings</b> it: the string is swapped
 *       (the metal-riser rule still applies) and part of its durability comes
 *       back;</li>
 *   <li>playing the minigame <b>retunes</b> it, at a small durability cost,
 *       because tuning works the string. Restringing at the same time waives
 *       that cost.</li>
 * </ul>
 * <p>Everything else is kept: enchantments, name, both woods of a composite,
 * a crossbow's attachment.</p>
 */
public final class BenchWork {
    private BenchWork() {
    }

    public static boolean isFinishedWeapon(ItemStack stack) {
        return (stack.getItem() instanceof ModularBowItem || stack.getItem() instanceof ModularCrossbowItem)
                && stack.has(ModDataComponents.BOW_ASSEMBLY.get());
    }

    /** Bench work happens when a finished weapon sits in the riser slot and both limb slots are empty. */
    public static boolean applies(ItemStack riserSlot, ItemStack topLimb, ItemStack bottomLimb) {
        return isFinishedWeapon(riserSlot) && topLimb.isEmpty() && bottomLimb.isEmpty();
    }

    /**
     * The reworked weapon, or {@link ItemStack#EMPTY} when there's nothing to do
     * (no string, no retune) or it can't be done (the string needs a metal riser,
     * or a retune would break the weapon).
     *
     * @param tuning the minigame score, or a negative value if it hasn't been played
     */
    public static ItemStack result(HolderLookup.Provider registries, ItemStack weapon, ItemStack string, float tuning) {
        boolean restring = !string.isEmpty();
        boolean retune = tuning >= 0f;
        BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly == null || (!restring && !retune)) {
            return ItemStack.EMPTY;
        }

        ItemStack out = weapon.copyWithCount(1);
        int max = out.getMaxDamage();

        if (restring) {
            Optional<Holder.Reference<BowStringDef>> def = MaterialResolver.resolveBowString(registries, string);
            if (def.isEmpty()) {
                return ItemStack.EMPTY;
            }
            boolean needsMetal = def.get().value().stats().requiresMetalRiser();
            if (needsMetal && !Materials.bowRiser(assembly.riserMaterial()).stats().metal()) {
                return ItemStack.EMPTY;
            }
            assembly = assembly.withString(def.get().key().location().toString());
            int repair = (int) Math.round(max * FletcherConfig.RESTRING_REPAIR.get());
            out.setDamageValue(Math.max(0, out.getDamageValue() - repair));
        }

        if (retune) {
            if (!restring) {
                int cost = (int) Math.ceil(max * FletcherConfig.RETUNE_COST.get());
                if (out.getDamageValue() + cost >= max) {
                    return ItemStack.EMPTY;
                }
                out.setDamageValue(out.getDamageValue() + cost);
            }
            assembly = assembly.withTuning(tuning);
            // The new score is the bow's own tuning. Tinker's Mark recalculates its
            // bonus on top of it from scratch the next time the bow ticks.
            out.remove(ModDataComponents.TUNING_BEFORE_MARK.get());
        }

        out.set(ModDataComponents.BOW_ASSEMBLY.get(), assembly);
        return out;
    }
}
