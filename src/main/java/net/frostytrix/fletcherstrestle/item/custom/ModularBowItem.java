package net.frostytrix.fletcherstrestle.item.custom;

import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.enchantment.ModEnchantments;
import net.frostytrix.fletcherstrestle.material.BowLimbDef;
import net.frostytrix.fletcherstrestle.material.BowRiserDef;
import net.frostytrix.fletcherstrestle.material.BowStringDef;
import net.frostytrix.fletcherstrestle.material.Materials;
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
        // Photosynthesis only allows a wooden riser and non-Nether-fungi limbs.
        if (enchantment.is(ModEnchantments.PHOTOSYNTHESIS)) {
            BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (assembly != null) {
                String riser = assembly.riserMaterial().toLowerCase();
                String limbs = assembly.limbMaterial().toLowerCase();
                boolean isWoodRiser = !riser.contains("copper") && !riser.contains("iron");
                boolean isValidLimbs = !limbs.contains("crimson") && !limbs.contains("warped");
                return isWoodRiser && isValidLimbs;
            }
        }
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);

        if (weapon != null) {
            BowAssembly assembly = weapon.get(ModDataComponents.BOW_ASSEMBLY.get());

            if (assembly != null && projectile instanceof AbstractArrow arrow) {
                BowLimbDef limb = Materials.bowLimb(assembly.limbMaterial());
                BowRiserDef riser = Materials.bowRiser(assembly.riserMaterial());
                BowStringDef string = Materials.bowString(assembly.stringMaterial());

                // --- DAMAGE MODIFIER ---
                arrow.setBaseDamage(arrow.getBaseDamage() * limb.stats().damageMultiplier());

                // Archery skill: crit chance.
                if (shooter instanceof net.minecraft.world.entity.player.Player p) {
                    net.frostytrix.fletcherstrestle.progression.ArcheryProgression.rollCrit(p, arrow);
                }

                // Amphibious lives on stats, not effects.
                if (limb.stats().amphibious()) {
                    arrow.getPersistentData().putBoolean("fletcherstrestle:amphibious", true);
                }

                // --- ON-FIRE EFFECTS: ignite (crimson), no-gravity (warped),
                //     flag-set (spruce punch, copper conductive), …
                LivingEntity finalShooter = shooter;
                limb.effects().forEach(e -> e.onProjectileFired(finalShooter, weapon, arrow));
                riser.effects().forEach(e -> e.onProjectileFired(finalShooter, weapon, arrow));
                string.effects().forEach(e -> e.onProjectileFired(finalShooter, weapon, arrow));
            }
        }
        return projectile;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        ItemStack bowStack = shooter.getUseItem();
        if (bowStack.isEmpty()) bowStack = shooter.getMainHandItem(); // Fallback for edge cases

        BowAssembly assembly = bowStack.get(ModDataComponents.BOW_ASSEMBLY.get());
        // getPickResult() is null for a plain vanilla Arrow (only our
        // ModularArrowEntity overrides it), so a vanilla or creative-default
        // arrow would NPE here and cancel the whole shot. Guard it.
        ItemStack pickResult = projectile.getPickResult();
        ArrowAssembly Assembly = pickResult != null
                ? pickResult.get(ModDataComponents.ARROW_ASSEMBLY.get())
                : null;
        float finalVelocity = velocity;
        float finalInaccuracy = inaccuracy;

        if (assembly != null) {
            BowStringDef string = Materials.bowString(assembly.stringMaterial());
            BowRiserDef riser = Materials.bowRiser(assembly.riserMaterial());
            // Fletching can be null if the ammo isn't a modular arrow.
            float fletchInacc = Assembly != null
                    ? Materials.arrowFletching(Assembly.fletching()).stats().inaccuracyMultiplier()
                    : 1.0f;

            // --- APPLY VELOCITY & INACCURACY ---
            finalVelocity = velocity * string.stats().velocityMultiplier();
            finalInaccuracy = inaccuracy * riser.stats().inaccuracyMultiplier() * fletchInacc;
        }

        // Archery skill: steadier aim with level.
        if (shooter instanceof net.minecraft.world.entity.player.Player p) {
            finalInaccuracy *= net.frostytrix.fletcherstrestle.progression.ArcheryProgression.inaccuracyMultiplier(p);
        }

        super.shootProjectile(shooter, projectile, index, finalVelocity, finalInaccuracy, angle, target);
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly == null) {
            tooltipComponents.add(Component.translatable("gui.fletcherstrestle.unfinished_bow").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
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

                // Take from the selected slot, or the next one that still has
                // arrows. Without this fallback a quiver goes dead the moment
                // the selected slot empties, even with arrows in other slots.
                int usable = firstUsableSlot(list, selected);
                if (usable >= 0) {
                    if (usable != selected) {
                        invStack.set(ModDataComponents.QUIVER_SELECTED_SLOT.get(), usable);
                    }
                    quiverInvSlot = i;
                    quiverStack = invStack;
                    quiverSelectedIdx = usable;
                    extractedArrow = list.get(usable).copy();
                    break;
                }
            }
        }

        // SWAP: Temporarily place the selected arrow directly into the player's inventory
        if (quiverInvSlot != -1) {
            player.getInventory().setItem(quiverInvSlot, extractedArrow);
        }

        // --- 2. VANILLA FIRING LOGIC ---
        // Vanilla finds the arrow we just placed, shoots it, and consumes it.
        int chargeTicks = this.getUseDuration(stack, entityLiving) - timeLeft;
        // Archery skill: faster draw shrinks the effective draw time.
        float customDrawTime = getDrawTime(stack)
                * net.frostytrix.fletcherstrestle.progression.ArcheryProgression.drawMultiplier(player);
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

            // Quick Nock: move to the next loaded arrow type after every shot,
            // so an enchanted bow cycles through the quiver as you fire.
            if (hasEnchant(level, stack, net.frostytrix.fletcherstrestle.enchantment.ModEnchantments.QUICK_NOCK)) {
                int next = firstUsableSlot(list, quiverSelectedIdx + 1);
                if (next >= 0) {
                    quiverStack.set(ModDataComponents.QUIVER_SELECTED_SLOT.get(), next);
                }
            }

            // SWAP: Put the Quiver back into the player's inventory
            player.getInventory().setItem(quiverInvSlot, quiverStack);
        }

        // --- 4. SHOOTER-SIDE RELEASE EFFECTS ---
        if (assembly != null) {
            BowLimbDef limb = Materials.bowLimb(assembly.limbMaterial());
            BowRiserDef riser = Materials.bowRiser(assembly.riserMaterial());
            BowStringDef string = Materials.bowString(assembly.stringMaterial());

            // Effects that target the shooter (acacia speed buff) or
            // anything else attached to a limb/riser/string def with an
            // onBowRelease handler.
            limb.effects().forEach(e -> e.onBowRelease(player, stack));
            riser.effects().forEach(e -> e.onBowRelease(player, stack));
            string.effects().forEach(e -> e.onBowRelease(player, stack));

            int durCost = string.stats().durabilityCost();
            if (durCost > 1) {
                stack.hurtAndBreak(durCost - 1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
        }
    }

    /**
     * The first slot at or after {@code from} (wrapping) that holds arrows, or
     * -1 when the quiver is empty.
     */
    private static int firstUsableSlot(List<ItemStack> contents, int from) {
        if (contents.isEmpty()) {
            return -1;
        }
        int size = contents.size();
        int start = Math.floorMod(from, size);
        for (int step = 0; step < size; step++) {
            int idx = (start + step) % size;
            ItemStack candidate = contents.get(idx);
            if (!candidate.isEmpty() && candidate.getItem() instanceof net.minecraft.world.item.ArrowItem) {
                return idx;
            }
        }
        return -1;
    }

    /** True when the stack carries the given enchantment. */
    private static boolean hasEnchant(Level level, ItemStack stack,
                                      net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return enchantLevel(level, stack, key) > 0;
    }

    /** Level of the given enchantment on the stack, or 0. */
    public static int enchantLevel(Level level, ItemStack stack,
                                   net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        var registry = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        return registry.getHolder(key)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
    }

    public float getDrawTime(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());

        if (assembly != null) {
            float baseTime = Materials.bowLimb(assembly.limbMaterial()).stats().drawTimeTicks();

            float tuning = Math.max(0.2f, assembly.tuning());

            return baseTime / tuning;
        }

        return 20.0f; // Vanilla standard fallback
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
        if (assembly != null) {
            return Materials.bowRiser(assembly.riserMaterial()).stats().maxDurability();
        }
        return super.getMaxDamage(stack);
    }


    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        super.onUseTick(level, livingEntity, stack, count);

        if (livingEntity instanceof Player player) {
            BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (assembly != null) {
                BowLimbDef limb = Materials.bowLimb(assembly.limbMaterial());
                String stringId = Materials.normaliseId(assembly.stringMaterial());

                if (limb.stats().givesSlowFalling() && !player.onGround()) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2, 2, false, false, true));
                }

                int ticksDrawn = this.getUseDuration(stack, player) - count;
                float maxDrawTime = getDrawTime(stack)
                        * net.frostytrix.fletcherstrestle.progression.ArcheryProgression.drawMultiplier(player);

                // Flax string: jitters the aim if the player overdraws. The AIM
                // skill elongates the grace period before the shake kicks in.
                int flaxGrace = net.frostytrix.fletcherstrestle.progression.ArcheryProgression.flaxGraceTicks(player);
                if ("flax".equals(stringId) && ticksDrawn > (maxDrawTime + flaxGrace)) {
                    player.setYRot(player.getYRot() + (level.random.nextFloat() - 0.5F) * 3.0F);
                    player.setXRot(player.getXRot() + (level.random.nextFloat() - 0.5F) * 3.0F);
                }
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            // Keeps the baked Tinker's Mark bonus in step with the enchantment,
            // including when it is removed at a grindstone.
            net.frostytrix.fletcherstrestle.enchantment.TinkersMark.reconcile(level, stack);
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
}
