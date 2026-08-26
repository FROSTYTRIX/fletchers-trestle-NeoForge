package net.frostytrix.fletcherstrestle.material;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.material.stats.BowLimbStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out the limb a bow actually behaves like.
 *
 * <p>An ordinary bow simply uses its one wood. A <em>composite</em>, laminated
 * from two woods, gets the average of their numbers and the traits of both.
 * Averaging the numbers means a composite sits between its parents rather than
 * beating them, but inheriting both sets of traits is genuinely powerful, which
 * is why composites are behind a server config.</p>
 *
 * <p>Every consumer should read limb stats through here rather than resolving
 * {@code assembly.limbMaterial()} directly, or composites would silently behave
 * like their top limb alone.</p>
 */
public final class CompositeLimb {
    private CompositeLimb() {
    }

    /** The limb definition a bow behaves like, blended when it is a composite. */
    public static BowLimbDef effective(BowAssembly assembly) {
        BowLimbDef top = Materials.bowLimb(assembly.limbMaterial());
        if (!assembly.isComposite()) {
            return top;
        }
        BowLimbDef bottom = Materials.bowLimb(assembly.secondLimb().orElse(assembly.limbMaterial()));
        return blend(top, bottom);
    }

    /** Convenience for the common case of only wanting the numbers. */
    public static BowLimbStats stats(BowAssembly assembly) {
        return effective(assembly).stats();
    }

    private static BowLimbDef blend(BowLimbDef a, BowLimbDef b) {
        BowLimbStats sa = a.stats();
        BowLimbStats sb = b.stats();

        BowLimbStats blended = new BowLimbStats(
                (sa.drawTimeTicks() + sb.drawTimeTicks()) / 2f,
                (sa.damageMultiplier() + sb.damageMultiplier()) / 2f,
                // A composite keeps every trait either wood has. That is the
                // point of laminating two woods, and it is why composites are
                // config-gated rather than watered down.
                sa.amphibious() || sb.amphibious(),
                sa.givesSlowFalling() || sb.givesSlowFalling(),
                sa.agility() || sb.agility()
        );

        List<MaterialEffect> effects = new ArrayList<>(a.effects());
        effects.addAll(b.effects());

        // Keeps the top limb's ingredient and texture: the composite's look is
        // handled by the model layering both woods, not by this def.
        return new BowLimbDef(a.ingredient(), blended, a.texture(), effects, a.crossbowOverrides());
    }
}
