package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModularArrowItem extends ArrowItem {


    public ModularArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        ArrowAssembly assembly = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);

        if (assembly == null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.unfinished_arrow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return;
        }

        if (!Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        // Assembly parts
        tooltipComponents.add(Component.translatable("gui.fletcherstrestle.arrow_parts").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.head")).append(": ")
                .append(net.frostytrix.fletcherstrestle.material.Materials.arrowHeadName(assembly.head())).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.shaft")).append(": ")
                .append(net.frostytrix.fletcherstrestle.material.Materials.arrowShaftName(assembly.shaft())).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.fletching")).append(": ")
                .append(net.frostytrix.fletcherstrestle.material.Materials.arrowFletchingName(assembly.fletching())).withStyle(ChatFormatting.GRAY));

        // Potion contents (when present — for glass-vial arrows after dipping)
        if (potion != null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.effects").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            // 1.0 durationFactor — full potion duration at impact center.
            // Effects fall off by distance at hit-time (see applyGlassVialEffect).
            potion.addPotionTooltip(tooltipComponents::add, 1.0F, 20.0F);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;

        // 1. Replace underscores with spaces so "dark_oak" becomes "dark oak"
        String spaced = str.replace('_', ' ');

        // 2. Split the string into individual words
        String[] words = spaced.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                // 3. Capitalize the first letter of each word
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        // 4. Trim the extra space at the end
        return result.toString().trim();
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        // Pass the 'ammo' ItemStack directly into your entity!
        // This 'ammo' contains your ArrowAssembly component.
        return new ModularArrowEntity(level, shooter, ammo.copy(), weapon);
    }


    /**
     * @deprecated see {@link ModularBowItem.LimbStats}.
     */
    @Deprecated(forRemoval = true)
    public enum HeadStats {
        FLINT("flint", 1.0f, false, false),
        BROADHEAD("broadhead", 1.15f, true, false), // Applies Bleed
        BODKIN_POINT("bodkin_point", 1.0f, false, true),        // Armor Piercing
        RESONANCE_TIP("resonance_tip", 1.0f, false, false),
        BARBED_TIP("barbed_tip", 1.0f, false, false),  // Pulls the target toward the shooter on hit
        WEIGHTED_BLUNT("weighted_blunt", 1.05f, false, false),
        WEIGHTED_HOOK("weighted_hook", 0.5f, false, false),
        TRAILING_ROPE("trailing_rope", 0.3f, false, false),
        GLASS_VIAL("glass_vial", 0.4f, false, false);  // Fragile delivery — payload is in the potion

        private final String name;
        private final float damageMult;
        private final boolean causesBleed;
        private final boolean armorPiercing;

        HeadStats(String name, float damageMult, boolean causesBleed, boolean armorPiercing) {
            this.name = name;
            this.damageMult = damageMult;
            this.causesBleed = causesBleed;
            this.armorPiercing = armorPiercing;
        }

        public static HeadStats fromString(String materialName) {
            for (HeadStats stat : values()) {
                if (stat.name.equalsIgnoreCase(materialName)) return stat;
            }
            return FLINT;
        }

        public float getDamageMult() {
            return damageMult;
        }

        public boolean causesBleed() {
            return causesBleed;
        }

        public boolean isArmorPiercing() {
            return armorPiercing;
        }
    }

    /**
     * @deprecated see {@link ModularBowItem.LimbStats}.
     */
    @Deprecated(forRemoval = true)
    public enum ShaftStats {
        OAK("oak", 1.0f, 1.0f),
        SPRUCE("spruce", 1.0f, 1.1f),   // Drops slightly faster (heavy)
        BIRCH("birch", 1f, 0.9f),
        JUNGLE("jungle", 1f, 1.0f),
        DARK_OAK("dark_oak", 1f, 1.0f),
        ACACIA("acacia", 1f, 1.0f),
        MANGROVE("mangrove", 1f, 1.0f),
        CHERRY("cherry", 1f, 1.0f),
        PALE_OAK("pale_oak", 1f, 1.0f),
        CRIMSON("crimson", 1f, 1.0f),
        WARPED("warped", 1f, 1.0f);// Flies faster, drops less

        private final String name;
        private final float velocityMult;
        private final float gravityMult;

        ShaftStats(String name, float velocityMult, float gravityMult) {
            this.name = name;
            this.velocityMult = velocityMult;
            this.gravityMult = gravityMult;
        }

        public static ShaftStats fromString(String name) {
            for (ShaftStats stat : values()) {
                if (stat.name.equalsIgnoreCase(name)) return stat;
            }
            return OAK;
        }

        public float getVelocityMult() {
            return velocityMult;
        }

        public float getGravityMult() {
            return gravityMult;
        }
    }

    /**
     * @deprecated see {@link ModularBowItem.LimbStats}.
     */
    @Deprecated(forRemoval = true)
    public enum FletchingStats {
        FEATHER("feather", 1.0f),
        RIGID("rigid", 0.84f),
        TRAILING("trailing", 0.75f),
        SERRATED("serrated", 1f),
        BOUND("bound", 1f),
        VEX("vex", 1f);

        private final String name;
        private final float inaccuracyMult;

        FletchingStats(String name, float accuracyMult) {
            this.name = name;
            this.inaccuracyMult = accuracyMult;
        }

        public static FletchingStats fromString(String name) {
            for (FletchingStats stat : values()) {
                if (stat.name.equalsIgnoreCase(name)) return stat;
            }
            return FEATHER;
        }

        public float getInaccuracyMult() {
            return inaccuracyMult;
        }
    }
}