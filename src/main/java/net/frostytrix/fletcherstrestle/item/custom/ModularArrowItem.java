package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.ModularArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

        if (assembly != null) {
            String headName = capitalize(assembly.head());
            String shaftName = capitalize(assembly.shaft());
            String fletchingName = capitalize(assembly.fletching());

            tooltipComponents.add(Component.literal("Arrow Parts:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltipComponents.add(Component.literal("- Head: " + headName).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("- Shaft: " + shaftName).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("- Fletching: " + fletchingName).withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.literal("Unfinished Arrow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
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
        ModularArrowEntity arrow = new ModularArrowEntity(level, shooter, ammo.copyWithCount(1), weapon);
        return arrow;
    }



    public enum HeadStats {
        FLINT("flint", 1.0f, false, false),
        BROADHEAD("broadhead", 1.15f, true, false), // Applies Bleed
        BODKIN_POINT("bodkin_point", 1.0f, false, true),        // Armor Piercing
        RESONANCE_TIP("resonance_tip", 1.0f, false, false),
        BARBED_TIP("barbed_tip", 1.0f, false, false),
        WEIGHTED_BLUNT("weighted_blunt", 1.0f, false, false);

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

        public float getDamageMult() { return damageMult; }
        public boolean causesBleed() { return causesBleed; }
        public boolean isArmorPiercing() { return armorPiercing; }
    }

    public enum ShaftStats {
        OAK("oak", 1.0f, 1.0f),
        SPRUCE("spruce", 1.0f, 1.1f),   // Drops slightly faster (heavy)
        BIRCH("birch", 1f, 0.9f),
        JUNGLE("jungle", 1f, 0.9f),
        DARK_OAK("dark_oak", 1f, 0.9f),
        ACACIA("acacia", 1f, 0.9f)
        ;// Flies faster, drops less

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

        public float getVelocityMult() { return velocityMult; }
        public float getGravityMult() { return gravityMult; }
    }

    public enum FletchingStats {
        FEATHER("feather", 1.0f),
        RIGID("rigid", 1.2f),
        TRAILING("trailing", 1.3f),
        SERRATED("serrated", 1f),
        BOUND("bound", 1f),
        VEX("vex", 1f);

        private final String name;
        private final float accuracyMult;

        FletchingStats(String name, float accuracyMult) {
            this.name = name;
            this.accuracyMult = accuracyMult;
        }

        public static FletchingStats fromString(String name) {
            for (FletchingStats stat : values()) {
                if (stat.name.equalsIgnoreCase(name)) return stat;
            }
            return FEATHER;
        }

        public float getAccuracyMult() { return accuracyMult; }
    }
}