package net.frostytrix.fletcherstrestle.enchantment;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tinker's Mark bakes a little extra tuning into a bow: 2% per level, up to 10%
 * at level five, and never past 100%.
 *
 * <p>Because the bonus is written into the assembly rather than applied on the
 * fly, it has to be undone if the enchantment ever goes away. The original
 * tuning is kept in its own component, so the state is reconciled from scratch
 * every tick: add the bonus when the enchantment is present, restore the
 * original when it is not. That handles anvils, grindstones and commands alike
 * without needing to hook any of them.</p>
 */
public final class TinkersMark {
    private TinkersMark() {
    }

    private static final float PER_LEVEL = 0.02f;

    /**
     * Brings a bow's tuning in line with its current Tinker's Mark level.
     * Safe to call every tick: it only writes when something actually differs.
     */
    public static void reconcile(Level level, ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly == null) {
            return;
        }
        int marks = ModularBowItem.enchantLevel(level, stack, ModEnchantments.TINKERS_MARK);
        Float storedBase = stack.get(ModDataComponents.TUNING_BEFORE_MARK.get());

        if (marks <= 0) {
            // Enchantment gone: put the original tuning back.
            if (storedBase != null) {
                stack.set(ModDataComponents.BOW_ASSEMBLY.get(), withTuning(assembly, storedBase));
                stack.remove(ModDataComponents.TUNING_BEFORE_MARK.get());
            }
            return;
        }

        float base = storedBase != null ? storedBase : assembly.tuning();
        float marked = Math.min(1.0f, base + PER_LEVEL * marks);
        if (storedBase == null) {
            stack.set(ModDataComponents.TUNING_BEFORE_MARK.get(), base);
        }
        if (Math.abs(assembly.tuning() - marked) > 1.0E-5f) {
            stack.set(ModDataComponents.BOW_ASSEMBLY.get(), withTuning(assembly, marked));
        }
    }

    private static BowAssembly withTuning(BowAssembly assembly, float tuning) {
        return assembly.withTuning(tuning);
    }
}
