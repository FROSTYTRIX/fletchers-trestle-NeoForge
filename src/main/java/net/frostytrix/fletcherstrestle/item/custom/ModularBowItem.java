package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ModularBowItem extends BowItem {

    public ModularBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        // If the game is asking about Photosynthesis, check our custom rules
        if (enchantment.is(ModEnchantments.PHOTOSYNTHESIS)) {
            BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

            if (assembly != null) {
                // Convert to lowercase to make checking easier (e.g., "Dark Oak" -> "dark oak")
                String riser = assembly.riserMaterial().toLowerCase();
                String limbs = assembly.limbMaterial().toLowerCase();

                // 1. Riser MUST be wood (Update this list if you add Iron/Gold risers later)
                // Assuming currently only "copper" is your non-wood riser.
                boolean isWoodRiser = !riser.contains("copper") &&  !riser.contains("iron");

                // 2. Limbs MUST NOT be Nether fungi
                boolean isValidLimbs = !limbs.contains("crimson") && !limbs.contains("warped");

                // It can only receive the enchantment if both are true
                return isWoodRiser && isValidLimbs;
            }
        }

        // For all other standard enchantments (Power, Punch, etc.), use default behavior
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        // 1. Let vanilla create the base projectile first
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);

        // FIX: Check if the weapon is physically null before calling .get()
        if (weapon != null) {
            BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());

            if (assembly != null && projectile instanceof AbstractArrow arrow) {
                LimbStats limb = LimbStats.fromString(assembly.limbMaterial());
                RiserStats riser = RiserStats.fromString(assembly.riserMaterial());

                // --- DAMAGE MODIFIER ---
                arrow.setBaseDamage(arrow.getBaseDamage() * limb.getDamageMult());

                // --- SPECIAL TRAITS ---
                if (limb == LimbStats.CRIMSON) {
                    arrow.igniteForSeconds(100);
                }
                if (limb == LimbStats.WARPED) {
                    arrow.setNoGravity(true);
                }

                // --- PERSISTENT DATA TAGS ---
                if (limb.isAmphibian()) {
                    arrow.getPersistentData().putBoolean("fletcherstrestle:amphibious", true);
                }
                // Spruce limb: built-in Punch I. The arrow entity reads this
                // flag in doKnockback() to apply an extra knockback impulse,
                // since AbstractArrow.setKnockback no longer exists in 1.21.
                if (limb.getMaterialName().equals("Spruce")) {
                    arrow.getPersistentData().putBoolean("fletcherstrestle:punch", true);
                }
                if (riser.getMaterialName().equalsIgnoreCase("Copper")) {
                    arrow.getPersistentData().putBoolean("fletcherstrestle:conductive", true);
                }
            }
        }
        return projectile;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        ItemStack bowStack = shooter.getUseItem();
        if (bowStack.isEmpty()) bowStack = shooter.getMainHandItem(); // Fallback for edge cases

        BowAssembly assembly = bowStack.get(ModDataComponents.BOW_ASSEMBLY.get());
        ArrowAssembly Assembly = projectile.getPickResult().get(ModDataComponents.ARROW_ASSEMBLY.get());
        float finalVelocity = velocity;
        float finalInaccuracy = inaccuracy;

        if (assembly != null) {
            StringStats string = StringStats.fromString(assembly.stringMaterial());
            RiserStats riser = RiserStats.fromString(assembly.riserMaterial());
            ModularArrowItem.FletchingStats fletching = ModularArrowItem.FletchingStats.fromString(Assembly.fletching());

            // --- APPLY VELOCITY & INACCURACY ---
            finalVelocity = velocity * string.getVelocityMult();
            finalInaccuracy = inaccuracy * riser.getInnacuracyMult() * fletching.getInaccuracyMult();
        }

        // Call super with our modified values
        super.shootProjectile(shooter, projectile, index, finalVelocity, finalInaccuracy, angle, target);
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly == null) {
            tooltipComponents.add(Component.literal("Unfinished Bow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return;
        }

        if (!Screen.hasShiftDown()) {
            tooltipComponents.add(Component.literal("Hold Shift for details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        tooltipComponents.add(Component.literal("Assembly Parts:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("- Limbs: " + assembly.limbMaterial()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- Riser: " + assembly.riserMaterial()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- String: " + assembly.stringMaterial()).withStyle(ChatFormatting.GRAY));

        int tuningPercent = (int) (assembly.tuning() * 100);
        tooltipComponents.add(Component.literal("Tuning: " + tuningPercent + "%").withStyle(ChatFormatting.GREEN));
    }


    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) return;
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        // --- 1. THE QUIVER SWAP TRICK ---
        int quiverInvSlot = -1;
        ItemStack quiverStack = ItemStack.EMPTY;
        int quiverSelectedIdx = -1;
        ItemStack extractedArrow = ItemStack.EMPTY;

        // Search the inventory for a quiver
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof ModularQuiverItem) {
                int selected = invStack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
                List<ItemStack> list = ModularQuiverItem.getQuiverContents(invStack);

                // If the selected slot has an arrow, pull it out!
                if (selected >= 0 && selected < list.size() && !list.get(selected).isEmpty() && list.get(selected).getItem() instanceof net.minecraft.world.item.ArrowItem) {
                    quiverInvSlot = i;
                    quiverStack = invStack;
                    quiverSelectedIdx = selected;
                    extractedArrow = list.get(selected).copy();
                    break;
                }
            }
        }

        // SWAP: Temporarily place the selected arrow directly into the player's inventory
        if (quiverInvSlot != -1) {
            player.getInventory().setItem(quiverInvSlot, extractedArrow);
        }

        // --- 2. VANILLA FIRING LOGIC ---
        // Vanilla will find the arrow we just placed, shoot it, and consume it naturally!
        int chargeTicks = this.getUseDuration(stack, entityLiving) - timeLeft;
        float customDrawTime = getDrawTime(stack);
        int scaledCharge = (int) ((chargeTicks / customDrawTime) * 20.0f);
        int fakeTimeLeft = this.getUseDuration(stack, entityLiving) - scaledCharge;

        super.releaseUsing(stack, level, entityLiving, fakeTimeLeft);

        // --- 3. RESTORE THE QUIVER ---
        if (quiverInvSlot != -1) {
            // Get the arrow back (it might be shrunken by 1 now)
            ItemStack modifiedArrow = player.getInventory().getItem(quiverInvSlot);

            // Put it back inside the Quiver's container
            List<ItemStack> list = ModularQuiverItem.getQuiverContents(quiverStack);
            list.set(quiverSelectedIdx, modifiedArrow.isEmpty() ? ItemStack.EMPTY : modifiedArrow);
            ModularQuiverItem.saveQuiverContents(quiverStack, list);

            // SWAP: Put the Quiver back into the player's inventory
            player.getInventory().setItem(quiverInvSlot, quiverStack);
        }

        // --- 4. YOUR EXISTING CUSTOM EFFECTS ---
        if (assembly != null) {
            if (assembly.limbMaterial().equals("Acacia")) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false, true));
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1. Check if the player has a normal arrow in their open inventory
        boolean hasNormalArrow = !player.getProjectile(stack).isEmpty();

        // 2. Check if the player has a Quiver with an arrow selected
        boolean hasQuiverArrow = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof ModularQuiverItem) {
                int selected = invStack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
                List<ItemStack> list = ModularQuiverItem.getQuiverContents(invStack);

                // If the selected slot has an arrow, we are good to go!
                if (selected >= 0 && selected < list.size() && !list.get(selected).isEmpty() && list.get(selected).getItem() instanceof net.minecraft.world.item.ArrowItem) {
                    hasQuiverArrow = true;
                    break;
                }
            }
        }

        // 3. If they are in Survival and have NO ammo anywhere, block the shot.
        if (!player.getAbilities().instabuild && !hasNormalArrow && !hasQuiverArrow) {
            return InteractionResultHolder.fail(stack);
        } else {
            // Otherwise, allow them to start pulling the bow!
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
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
