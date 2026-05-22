package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModularCrossbowItem extends CrossbowItem {
    public ModularCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);

        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly == null) {
            tooltipComponents.accept(Component.literal("Unfinished Crossbow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return;
        }

        // 26.1: tooltipFlag.hasShiftDown() replaces Screen.hasShiftDown().
        if (!tooltipFlag.hasShiftDown()) {
            tooltipComponents.accept(Component.literal("Hold Shift for details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        tooltipComponents.accept(Component.literal("Assembly Parts:").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltipComponents.accept(Component.literal("- Limbs: " + assembly.limbMaterial()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.literal("- Riser: " + assembly.riserMaterial()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.literal("- String: " + assembly.stringMaterial()).withStyle(ChatFormatting.GRAY));

        int tuningPercent = (int) (assembly.tuning() * 100);
        tooltipComponents.accept(Component.literal("Tuning: " + tuningPercent + "%").withStyle(ChatFormatting.GREEN));
    }

    // --- 1. CHARGING TIME LOGIC ---
    // 26.1: CrossbowItem.releaseUsing now refuses to load unless
    // useDuration - timeLeft >= getChargeDuration (~25 ticks at base).
    // Our materials let draw time drop to ~12 ticks at high tuning, which
    // used to be fine in 1.21.1 but now silently fails the load check
    // (crossbow goes back to empty after release — "doesn't keep the arrow").
    // Floor against the static charge duration so the load always passes.
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        int draw = (int) getDrawTime(stack);
        int floor = CrossbowItem.getChargeDuration(stack, entity);
        return Math.max(draw, floor) + 3;
    }

    public float getDrawTime(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            float baseTime = LimbStats.fromString(assembly.limbMaterial()).getDrawTime();
            float tuning = Math.max(0.2f, assembly.tuning());
            return baseTime / tuning;
        }
        return 25.0f; // Vanilla crossbow default
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            return (int) RiserStats.fromString(assembly.riserMaterial()).getMaxDurability();
        }
        return super.getMaxDamage(stack);
    }



    // --- 2. QUIVER SWAP & LOADING LOGIC ---
    // 26.1: CrossbowItem.releaseUsing now returns boolean (true = consumed).
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) return false;

        // Ensure we actually pulled it back far enough to load!
        int chargeTicks = this.getUseDuration(stack, entityLiving) - timeLeft;
        if (chargeTicks < this.getChargeDuration(stack, entityLiving)) {
            return super.releaseUsing(stack, level, entityLiving, timeLeft);
        }

        // 1. THE QUIVER SWAP TRICK
        int quiverInvSlot = -1;
        ItemStack quiverStack = ItemStack.EMPTY;
        int quiverSelectedIdx = -1;
        ItemStack extractedArrow = ItemStack.EMPTY;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof ModularQuiverItem) {
                int selected = invStack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
                List<ItemStack> list = ModularQuiverItem.getQuiverContents(invStack);

                if (selected >= 0 && selected < list.size() && !list.get(selected).isEmpty() && list.get(selected).getItem() instanceof net.minecraft.world.item.ArrowItem) {
                    quiverInvSlot = i;
                    quiverStack = invStack;
                    quiverSelectedIdx = selected;
                    extractedArrow = list.get(selected).copy();
                    break;
                }
            }
        }

        // SWAP IN
        if (quiverInvSlot != -1) {
            player.getInventory().setItem(quiverInvSlot, extractedArrow);
        }

        // 2. VANILLA LOADING
        boolean consumed = super.releaseUsing(stack, level, entityLiving, timeLeft);

        // 3. RESTORE THE QUIVER
        if (quiverInvSlot != -1) {
            ItemStack modifiedArrow = player.getInventory().getItem(quiverInvSlot);
            List<ItemStack> list = ModularQuiverItem.getQuiverContents(quiverStack);
            list.set(quiverSelectedIdx, modifiedArrow.isEmpty() ? ItemStack.EMPTY : modifiedArrow);
            ModularQuiverItem.saveQuiverContents(quiverStack, list);
            player.getInventory().setItem(quiverInvSlot, quiverStack);
        }
        return consumed;
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        // 1. Let vanilla create the base projectile first
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);

        // 2. Fetch our custom modular stats
        BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());

        // 3. If it's a valid assembly and the projectile is an arrow, apply traits!
        if (assembly != null && projectile instanceof AbstractArrow arrow) {
            LimbStats limb = LimbStats.fromString(assembly.limbMaterial());

            // --- DAMAGE MODIFIER ---
            // Vanilla arrows calculate final damage as (velocity * baseDamage).
            // We apply the Limb's damage multiplier directly to the base damage here.
            arrow.setBaseDamage(arrow.baseDamage * limb.getDamageMult());

            // --- WARPED: No Gravity ---
            if (assembly.limbMaterial().equalsIgnoreCase("Warped")) {
                arrow.setNoGravity(true);
            }

            // --- CRIMSON: Flaming Arrows ---
            if (assembly.limbMaterial().equalsIgnoreCase("Crimson")) {
                arrow.setRemainingFireTicks(100);
            }

            // --- SPRUCE: Built-in Punch I (read by ModularArrowEntity.doKnockback) ---
            if (assembly.limbMaterial().equalsIgnoreCase("Spruce")) {
                arrow.getPersistentData().putBoolean("fletcherstrestle:punch", true);
            }


            // --- MANGROVE: Amphibious ---
            // If you have a custom arrow entity that handles water physics,
            // you can cast it here and trigger your amphibious logic!
            if (limb.isAmphibian() && arrow instanceof ModularArrowEntity modArrow) {
                // modArrow.setAmphibious(true); // Uncomment if you have this method in ModularArrowEntity
            }
        }

        // 4. Return the fully modified arrow to the firing sequence
        return projectile;
    }

    // --- 3. FIRING LOGIC (Apply Stats) ---
    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        ItemStack crossbowStack = shooter.getMainHandItem();
        BowAssembly assembly = crossbowStack.get(ModDataComponents.BOW_ASSEMBLY.get());
        float finalVelocity = velocity;
        float finalInaccuracy = inaccuracy;



        if (assembly != null) {
            StringStats string = StringStats.fromString(assembly.stringMaterial());
            RiserStats riser = RiserStats.fromString(assembly.riserMaterial());
            LimbStats limb = LimbStats.fromString(assembly.limbMaterial());

            // Crossbow damage inherently scales with velocity in vanilla.
            // We multiply by both String Speed AND Limb Damage.
            finalVelocity = velocity * string.getVelocityMult();
            finalInaccuracy = inaccuracy * riser.getInnacuracyMult();

            // Apply Durability Cost
            if (string.getDurabilityCost() > 1 && shooter instanceof Player player) {
                // 26.1: LivingEntity.getSlotForHand removed; map InteractionHand directly.
                crossbowStack.hurtAndBreak((int) (string.getDurabilityCost() - 1), player,
                        shooter.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            }
        }

        super.shootProjectile(shooter, projectile, index, velocity, inaccuracy, angle, target);
        if (shooter instanceof Player player) {
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
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        super.onUseTick(level, livingEntity, stack, count);

        if (livingEntity instanceof Player player) {
            BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (assembly != null) {
                LimbStats limb = LimbStats.fromString(assembly.limbMaterial());

                // Acacia Movement Buff WHILE pulling the crossbow
                if (assembly.limbMaterial().equals("Acacia")) {
                    player.addEffect(new MobEffectInstance(MobEffects.SPEED, 5, 0, false, false, false));
                }

                if (limb.isGivesSlowFalling() && !player.onGround()) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 2, false, false, false));
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // If it's already loaded, bypass the ammo check and shoot it!
        if (isCharged(stack)) {
            return super.use(level, player, hand);
        }

        boolean hasNormalArrow = !player.getProjectile(stack).isEmpty();
        boolean hasQuiverArrow = false;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.getItem() instanceof ModularQuiverItem) {
                int selected = invStack.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
                List<ItemStack> list = ModularQuiverItem.getQuiverContents(invStack);

                if (selected >= 0 && selected < list.size() && !list.get(selected).isEmpty() && list.get(selected).getItem() instanceof net.minecraft.world.item.ArrowItem) {
                    hasQuiverArrow = true;
                    break;
                }
            }
        }

        if (!player.getAbilities().instabuild && !hasNormalArrow && !hasQuiverArrow) {
            return InteractionResult.FAIL;
        } else {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
    }

    // --- ENUMS ---
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
            return OAK;
        }

        public float getDrawTime() { return drawTime; }
        public float getDamageMult() { return damageMult; }
        public boolean isGivesSlowFalling() { return givesSlowFalling; }
        public boolean isAmphibian() { return isAmphibious; }
        public Object getMaterialName() { return name;}
    }

    public enum RiserStats {
        WOOD("wood", 250, 1.0f),
        IRON("iron", 750, 0.2f),
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
        HIGH_TENSION("high tension", 1.8f, 2);

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