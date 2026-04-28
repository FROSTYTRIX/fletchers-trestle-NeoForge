package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ModularBowItem extends BowItem {

    public ModularBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly != null) {
            tooltipComponents.add(Component.literal("Assembly Parts:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltipComponents.add(Component.literal("- Limbs: " + assembly.limbMaterial()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("- Riser: " + assembly.riserMaterial()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("- String: " + assembly.stringMaterial()).withStyle(ChatFormatting.GRAY));

            int tuningPercent = (int) (assembly.tuning() * 100);
            tooltipComponents.add(Component.literal("Tuning: " + tuningPercent + "%").withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.add(Component.literal("Unfinished Bow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        int chargeTicks = this.getUseDuration(stack, entityLiving) - timeLeft;

        float customDrawTime = getDrawTime(stack);

        int scaledCharge = (int) ((chargeTicks / customDrawTime) * 20.0f);

        int fakeTimeLeft = this.getUseDuration(stack, entityLiving) - scaledCharge;
        super.releaseUsing(stack, level, entityLiving, fakeTimeLeft);
        if (assembly != null && entityLiving instanceof Player player) {

            if (assembly.limbMaterial().equals("Acacia")) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
            }

            StringStats string = StringStats.fromString(assembly.stringMaterial());
            if (string.getDurabilityCost() > 1) {
                stack.hurtAndBreak((int) (string.getDurabilityCost() - 1), player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
        }
    }

    public float getDrawTime(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly != null) {
            float baseTime = LimbStats.fromString(assembly.limbMaterial()).getDrawTime();

            float tuning = Math.max(0.2f, assembly.tuning());

            return baseTime / tuning;
        }

        return 20.0f; // Vanilla standard fallback
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            return (int) RiserStats.fromString(assembly.riserMaterial()).getMaxDurability();
        }
        return super.getMaxDamage(stack);
    }


    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        super.onUseTick(level, livingEntity, stack, count);

        if (livingEntity instanceof Player player) {
            BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (assembly != null) {
                LimbStats limb = LimbStats.fromString(assembly.limbMaterial());
                StringStats string = StringStats.fromString(assembly.stringMaterial());

                if (limb.isGivesSlowFalling() && !player.onGround()) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2, 2, false, false, true));
                }

                int ticksDrawn = this.getUseDuration(stack, player) - count;
                float maxDrawTime = getDrawTime(stack);

                if (string == StringStats.FLAX && ticksDrawn > (maxDrawTime + 40)) {
                    player.setYRot(player.getYRot() + (level.random.nextFloat() - 0.5F) * 3.0F);
                    player.setXRot(player.getXRot() + (level.random.nextFloat() - 0.5F) * 3.0F);
                }
            }
        }
    }

    public enum LimbStats {
        OAK("oak", 20.0f, 1.0f,false,false),
        SPRUCE("spruce", 22.0f, 1.0f,false,false),
        BIRCH("birch", 10.0f, 0.7f,false,false),
        JUNGLE("jungle", 18.0f, 0.9f,false,false),
        ACACIA("acacia", 20.0f, 1f,false,false),
        DARK_OAK("dark oak", 35.0f, 1.6f,false,false),
        MANGROVE("mangrove", 22.0f, 1f,true,false),
        CHERRY("cherry", 20.0f, 0.85f,false,true),
        PALE_OAK("pale oak", 26.0f, 1f,false,false),
        CRIMSON("crimson", 24.0f, 1.1f,false,false),
        WARPED("warped", 20.0f, 1.0f,false,false);

        private final String name;
        private final float drawTime;
        private final float damageMult;
        private final boolean isAmphibious;
        private final boolean givesSlowFalling;

        LimbStats(String name, float drawTime, float damageMult, boolean isAmphibious, boolean givesSlowFalling) {
            this.name = name;
            this.drawTime = drawTime;
            this.damageMult = damageMult;
            this.isAmphibious = isAmphibious;
            this.givesSlowFalling = givesSlowFalling;
        }

        public static LimbStats fromString(String materialName) {
            for (LimbStats stat : values()) {
                if (stat.name.equalsIgnoreCase(materialName)) return stat;
            }
            return OAK; // Default fallback
        }

        public float getDrawTime() { return drawTime; }
        public float getDamageMult() { return damageMult; }
        public boolean isGivesSlowFalling() { return givesSlowFalling; }
        public boolean isAmphibian() { return isAmphibious; }

        public Object getMaterialName() { return name;}
    }

    public enum RiserStats {
        WOOD("wood", 250, 1.0f),
        IRON("iron", 750, 0.2f),   // 0.2 inaccuracy = Laser precision
        COPPER("copper", 400, 1.0f);

        private final String name;
        private final int maxDurability;
        private final float inaccuracyMult;

        RiserStats(String name, int maxDurability, float inaccuracyMult) {
            this.name = name;
            this.maxDurability = maxDurability;
            this.inaccuracyMult = inaccuracyMult;
        }

        public static RiserStats fromString(String name) {
            for (RiserStats stat : values()) {
                if (stat.name.equalsIgnoreCase(name)) return stat;
            }
            return WOOD;
        }

        public float getMaxDurability() { return maxDurability; }
        public float getInnacuracyMult() { return inaccuracyMult; }
        public String getMaterialName() {return name;}
    }

    public enum StringStats {
        SPIDER("spider", 1.0f, 1),
        FLAX("flax", 1.3f, 1),
        HIGH_TENSION("high tension", 1.8f, 2); // 1.8x speed, but costs 2 durability per shot

        private final String name;
        private final float velocityMult;
        private final int durabilityCost;

        StringStats(String name, float velocityMult, int durabilityCost) {
            this.name = name;
            this.velocityMult = velocityMult;
            this.durabilityCost = durabilityCost;
        }

        public static StringStats fromString(String name) {
            for (StringStats stat : values()) {
                if (stat.name.equalsIgnoreCase(name)) return stat;
            }
            return SPIDER;
        }
        public float getVelocityMult() { return velocityMult; }
        public float getDurabilityCost() { return durabilityCost; }
    }
}
