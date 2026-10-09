package net.frostytrix.fletcherstrestle.fluid;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The liquid potion in a Dipping Vat carries the poured potion's components: its
 * full {@code potion_contents}, and anything else a mod put on the bottle. Storing
 * all of it, not just a potion id, keeps custom effects intact: a potion mixed in
 * Potion Blender, or one from any mod that builds potions from custom effects,
 * goes onto arrows unchanged and comes back out into bottles under its own name.
 */
public final class PotionFluid {
    private PotionFluid() {
    }

    /** Older saves stored only the potion's id, under this custom-data key. */
    private static final String LEGACY_KEY = "potion";

    public static FluidStack of(PotionContents contents, int amount) {
        FluidStack stack = new FluidStack(ModFluids.LIQUID_POTION_SOURCE.get(), amount);
        stack.set(DataComponents.POTION_CONTENTS, contents);
        return stack;
    }

    /** A potion bottle poured out: the liquid keeps every component the bottle had. */
    public static FluidStack fromBottle(ItemStack potion, int amount) {
        FluidStack stack = new FluidStack(ModFluids.LIQUID_POTION_SOURCE.get(), amount);
        stack.applyComponents(potion.getComponentsPatch());
        return stack;
    }

    /** A bottle filled from the liquid, with the components it was poured in with. */
    public static ItemStack toBottle(FluidStack stack) {
        ItemStack bottle = new ItemStack(Items.POTION);
        if (stack.has(DataComponents.POTION_CONTENTS)) {
            bottle.applyComponents(stack.getComponentsPatch());
        } else {
            bottle.set(DataComponents.POTION_CONTENTS, contents(stack));
        }
        return bottle;
    }

    public static boolean isPotion(FluidStack stack) {
        return stack.getFluid() == ModFluids.LIQUID_POTION_SOURCE.get();
    }

    /** The potion this liquid holds, or {@link PotionContents#EMPTY} for water or nothing. */
    public static PotionContents contents(FluidStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            return contents;
        }
        CustomData legacy = stack.get(DataComponents.CUSTOM_DATA);
        if (legacy != null && legacy.contains(LEGACY_KEY)) {
            ResourceLocation id = ResourceLocation.tryParse(legacy.copyTag().getString(LEGACY_KEY));
            if (id != null) {
                var potion = BuiltInRegistries.POTION.getHolder(id);
                if (potion.isPresent()) {
                    return new PotionContents(potion.get());
                }
            }
        }
        return PotionContents.EMPTY;
    }

    /** An older id-only stack rewritten with full contents, so fresh pours merge into it. */
    public static FluidStack upgrade(FluidStack stack) {
        if (isPotion(stack) && !stack.has(DataComponents.POTION_CONTENTS) && stack.has(DataComponents.CUSTOM_DATA)) {
            return of(contents(stack), stack.getAmount());
        }
        return stack;
    }

    /** The registry id of the stored potion, if it is a single registered potion. */
    public static String potionId(FluidStack stack) {
        return contents(stack).potion()
                .flatMap(holder -> holder.unwrapKey())
                .map(key -> key.location().toString())
                .orElse("");
    }
}
