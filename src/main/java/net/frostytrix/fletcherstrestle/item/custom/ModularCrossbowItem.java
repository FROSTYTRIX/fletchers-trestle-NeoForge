package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.frostytrix.fletcherstrestle.material.BowLimbDef;
import net.frostytrix.fletcherstrestle.material.BowRiserDef;
import net.frostytrix.fletcherstrestle.material.BowStringDef;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModularCrossbowItem extends CrossbowItem {
    public ModularCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly == null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.unfinished_crossbow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return;
        }

        if (!Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        tooltipComponents.add(Component.translatable("gui.fletcherstrestle.assembly_parts").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.limbs")).append(": ")
                .append(Materials.bowLimbName(assembly.limbMaterial())).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.riser")).append(": ")
                .append(Materials.bowRiserName(assembly.riserMaterial())).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("- ").append(Component.translatable("gui.fletcherstrestle.string")).append(": ")
                .append(Materials.bowStringName(assembly.stringMaterial())).withStyle(ChatFormatting.GRAY));

        int tuningPercent = (int) (assembly.tuning() * 100);
        tooltipComponents.add(Component.translatable("gui.fletcherstrestle.tuning").append(": " + tuningPercent + "%").withStyle(ChatFormatting.GREEN));

        ResourceLocation attId = stack.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
        if (attId != null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.attachment").append(": ")
                    .append(ModCrossbowAttachments.displayName(attId)).withStyle(ChatFormatting.AQUA));
        }
    }

    // --- 1. CHARGING TIME LOGIC ---
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        // A magazine makes the draw take reload_multiplier times as long; the
        // hold window has to match so the player can hold until it loads.
        if (magazineSize(stack, entity) > 1) {
            return magazineChargeTicks(stack, entity) + 3;
        }
        return (int) getDrawTime(stack) + 3;
    }

    /** Ticks a magazine crossbow must be drawn before it loads (slower reload). */
    private int magazineChargeTicks(ItemStack stack, LivingEntity entity) {
        return Math.round(getChargeDuration(stack, entity) * reloadMultiplier(stack, entity));
    }

    /**
     * Ticks the crossbow must be drawn before it loads: a magazine stretches this
     * by its reload multiplier. Public so the client "pull" animation can divide by
     * the same value and stay in sync with the real draw time.
     */
    public int requiredChargeTicks(ItemStack stack, LivingEntity entity) {
        return magazineSize(stack, entity) > 1
                ? magazineChargeTicks(stack, entity)
                : getChargeDuration(stack, entity);
    }

    public float getDrawTime(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            float baseTime = Materials.bowLimb(assembly.limbMaterial()).stats().drawTimeTicks();
            float tuning = Math.max(0.2f, assembly.tuning());
            return baseTime / tuning;
        }
        return 25.0f; // Vanilla crossbow default
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            return Materials.bowRiser(assembly.riserMaterial()).stats().maxDurability();
        }
        return super.getMaxDamage(stack);
    }

    // --- 2. QUIVER SWAP & LOADING LOGIC ---
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (!(entityLiving instanceof Player player)) return;

        // Ensure we actually pulled it back far enough to load. A magazine
        // crossbow requires the longer (reload_multiplier) draw; until then we
        // must NOT fall through to super, which would load at the normal time.
        boolean magazine = magazineSize(stack, entityLiving) > 1;
        int required = requiredChargeTicks(stack, entityLiving);
        int chargeTicks = this.getUseDuration(stack, entityLiving) - timeLeft;
        if (chargeTicks < required) {
            if (!magazine) {
                super.releaseUsing(stack, level, entityLiving, timeLeft);
            }
            return;
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
        super.releaseUsing(stack, level, entityLiving, timeLeft);

        // 3. RESTORE THE QUIVER
        if (quiverInvSlot != -1) {
            ItemStack modifiedArrow = player.getInventory().getItem(quiverInvSlot);
            List<ItemStack> list = ModularQuiverItem.getQuiverContents(quiverStack);
            list.set(quiverSelectedIdx, modifiedArrow.isEmpty() ? ItemStack.EMPTY : modifiedArrow);
            ModularQuiverItem.saveQuiverContents(quiverStack, list);
            player.getInventory().setItem(quiverInvSlot, quiverStack);
        }

        // 4. MAGAZINE: top up the charge to magazine_size, consuming one extra
        // arrow per added bolt (creative doesn't consume). Only magazine
        // crossbows enter this; normal crossbows are unaffected.
        int mag = magazineSize(stack, entityLiving);
        if (mag > 1 && isCharged(stack)) {
            ChargedProjectiles cp = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (cp != null && !cp.isEmpty()) {
                List<ItemStack> bolts = new ArrayList<>(cp.getItems());
                while (bolts.size() < mag) {
                    if (player.getAbilities().instabuild) {
                        bolts.add(bolts.get(0).copy());
                        continue;
                    }
                    ItemStack arrow = tryConsumeOneArrow(player);
                    if (arrow.isEmpty()) break;
                    bolts.add(arrow);
                }
                stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(bolts));
            }
        }
    }

    // ---------------- Bayonet attachment ----------------

    /** Bonus melee damage from the installed attachment def, or 0 if none. */
    private static float meleeDamage(ItemStack stack, LivingEntity entity) {
        ResourceLocation id = stack.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
        if (id == null) return 0.0f;
        var def = entity.level().registryAccess()
                .registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT).get(id);
        return def != null ? def.stats().meleeDamage() : 0.0f;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // A fitted bayonet makes the crossbow a real melee weapon (attack damage is
        // applied via the bench-set attribute modifiers); stabbing wears it down.
        if (meleeDamage(stack, attacker) > 0.0f) {
            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
            return true;
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    // ---------------- Magazine attachment ----------------

    /** Magazine capacity from the installed attachment def, or 1 if none. */
    private static int magazineSize(ItemStack stack, LivingEntity entity) {
        ResourceLocation id = stack.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
        if (id == null) return 1;
        var def = entity.level().registryAccess()
                .registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT).get(id);
        return def != null ? Math.max(1, def.stats().magazineSize()) : 1;
    }

    /** Charge-time multiplier from the installed attachment def, or 1.0 if none. */
    private static float reloadMultiplier(ItemStack stack, LivingEntity entity) {
        ResourceLocation id = stack.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
        if (id == null) return 1.0f;
        var def = entity.level().registryAccess()
                .registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT).get(id);
        return def != null ? Math.max(1.0f, def.stats().reloadMultiplier()) : 1.0f;
    }

    /** Pulls a single arrow from the selected quiver slot, else plain inventory. */
    private static ItemStack tryConsumeOneArrow(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (inv.getItem() instanceof ModularQuiverItem) {
                int sel = inv.getOrDefault(ModDataComponents.QUIVER_SELECTED_SLOT.get(), 0);
                List<ItemStack> list = ModularQuiverItem.getQuiverContents(inv);
                if (sel >= 0 && sel < list.size() && !list.get(sel).isEmpty()
                        && list.get(sel).getItem() instanceof ArrowItem) {
                    ItemStack one = list.get(sel).copy();
                    one.setCount(1);
                    ItemStack remaining = list.get(sel).copy();
                    remaining.shrink(1);
                    list.set(sel, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
                    ModularQuiverItem.saveQuiverContents(inv, list);
                    return one;
                }
            }
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inv = player.getInventory().getItem(i);
            if (!inv.isEmpty() && inv.getItem() instanceof ArrowItem) {
                ItemStack one = inv.copy();
                one.setCount(1);
                inv.shrink(1);
                return one;
            }
        }
        return ItemStack.EMPTY;
    }

    // Repeating fire: a magazine crossbow shoots ONE bolt per click and keeps
    // the rest charged, instead of vanilla's fire-all-at-once. Normal crossbows
    // fall through to super.
    @Override
    public void performShooting(Level level, LivingEntity shooter, InteractionHand hand,
                                ItemStack weapon, float velocity, float inaccuracy, @Nullable LivingEntity target) {
        if (magazineSize(weapon, shooter) <= 1) {
            super.performShooting(level, shooter, hand, weapon, velocity, inaccuracy, target);
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ChargedProjectiles cp = weapon.get(DataComponents.CHARGED_PROJECTILES);
        if (cp == null || cp.isEmpty()) {
            return;
        }
        List<ItemStack> items = new ArrayList<>(cp.getItems());
        ItemStack first = items.remove(0);
        weapon.set(DataComponents.CHARGED_PROJECTILES,
                items.isEmpty() ? ChargedProjectiles.EMPTY : ChargedProjectiles.of(items));
        this.shoot(serverLevel, shooter, hand, weapon, List.of(first), velocity, inaccuracy,
                shooter instanceof Player, target);
        if (shooter instanceof ServerPlayer sp) {
            CriteriaTriggers.SHOT_CROSSBOW.trigger(sp, weapon);
            sp.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
        }
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);
        BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly != null && projectile instanceof AbstractArrow arrow) {
            BowLimbDef limb = Materials.bowLimb(assembly.limbMaterial());
            BowRiserDef riser = Materials.bowRiser(assembly.riserMaterial());
            BowStringDef string = Materials.bowString(assembly.stringMaterial());

            arrow.setBaseDamage(arrow.getBaseDamage() * limb.stats().damageMultiplier());

            // Archery skill: crit chance.
            if (shooter instanceof Player p) {
                net.frostytrix.fletcherstrestle.progression.ArcheryProgression.rollCrit(p, arrow);
            }

            // Amphibious lives on stats, not effects.
            if (limb.stats().amphibious()) {
                arrow.getPersistentData().putBoolean("fletcherstrestle:amphibious", true);
            }

            // On-fire effects: ignite, no-gravity, flag-set, etc.
            limb.effects().forEach(e -> e.onProjectileFired(shooter, weapon, arrow));
            riser.effects().forEach(e -> e.onProjectileFired(shooter, weapon, arrow));
            string.effects().forEach(e -> e.onProjectileFired(shooter, weapon, arrow));
        }

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
            BowLimbDef limb = Materials.bowLimb(assembly.limbMaterial());
            BowRiserDef riser = Materials.bowRiser(assembly.riserMaterial());
            BowStringDef string = Materials.bowString(assembly.stringMaterial());

            // Crossbow damage inherently scales with velocity in vanilla.
            // We multiply by both String Speed AND Limb Damage.
            finalVelocity = velocity * string.stats().velocityMultiplier();
            finalInaccuracy = inaccuracy * riser.stats().inaccuracyMultiplier();

            // Shooter-targeting effects (acacia speed buff, …) fire here.
            limb.effects().forEach(e -> e.onBowRelease(shooter, crossbowStack));
            riser.effects().forEach(e -> e.onBowRelease(shooter, crossbowStack));
            string.effects().forEach(e -> e.onBowRelease(shooter, crossbowStack));

            // Apply Durability Cost
            int durCost = string.stats().durabilityCost();
            if (durCost > 1 && shooter instanceof Player player) {
                crossbowStack.hurtAndBreak(durCost - 1, player, LivingEntity.getSlotForHand(shooter.getUsedItemHand()));
            }
        }

        // Archery skill: steadier aim with level.
        if (shooter instanceof Player p) {
            finalInaccuracy *= net.frostytrix.fletcherstrestle.progression.ArcheryProgression.inaccuracyMultiplier(p);
        }

        super.shootProjectile(shooter, projectile, index, finalVelocity, finalInaccuracy, angle, target);
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
                BowLimbDef limb = Materials.bowLimb(assembly.limbMaterial());

                // Slow-falling-while-aiming is stats-driven (cherry limb sets it).
                if (limb.stats().givesSlowFalling() && !player.onGround()) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 2, false, false, false));
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
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
            return InteractionResultHolder.fail(stack);
        } else {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
    }

}