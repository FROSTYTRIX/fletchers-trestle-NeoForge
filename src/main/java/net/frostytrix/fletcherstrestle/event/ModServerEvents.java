package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.menu.FletchingMenu;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModServerEvents {

    // Apply modifiers to the arrows

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {

        // 1. CRITICAL: Only calculate physics and damage on the Server!
        // If we do this on the client, the arrow will visually glitch.
        if (event.getLevel().isClientSide()) {
            return;
        }

        // 2. Check if the entity is an arrow and has a shooter
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity shooter) {

            // 3. Find the bow (Check main hand, then offhand)
            ItemStack bow = shooter.getMainHandItem();
            if (!(bow.getItem() instanceof ModularBowItem)) {
                bow = shooter.getOffhandItem();
            }

            // 4. If it IS our custom bow, read the parts!
            if (bow.getItem() instanceof ModularBowItem) {
                BowAssembly assembly = bow.get(ModDataComponents.BOW_ASSEMBLY.get());

                if (assembly != null) {
                    ModularBowItem.LimbStats limb = ModularBowItem.LimbStats.fromString(assembly.limbMaterial());
                    ModularBowItem.StringStats string = ModularBowItem.StringStats.fromString(assembly.stringMaterial());
                    ModularBowItem.RiserStats riser = ModularBowItem.RiserStats.fromString(assembly.riserMaterial());
                    // --- APPLY THE MODIFIERS ---

                    // DAMAGE: Multiply the base damage
                    arrow.setBaseDamage(arrow.getBaseDamage() * limb.getDamageMult());

                    // VELOCITY: Scale the movement vector
                    arrow.setDeltaMovement(arrow.getDeltaMovement().scale(string.getVelocityMult()));

                    // INACCURACY (Aiming spread)
                    // We re-shoot the arrow from the player's rotation using our specific inaccuracy multiplier.
                    // Vanilla uses 1.0F. Iron Riser uses 0.2F (Laser).
                    float currentSpeed = (float) arrow.getDeltaMovement().length();
                    arrow.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, currentSpeed, riser.getInnacuracyMult());

                    // MAGIC / SPECIALS
                    if (limb == ModularBowItem.LimbStats.CRIMSON) {
                        arrow.igniteForSeconds(100);
                    }
                    if (limb == ModularBowItem.LimbStats.WARPED) {
                        arrow.setNoGravity(true);
                    }

                    if (limb.isAmphibian()) {
                        // Stamp the arrow with a custom NBT tag
                        arrow.getPersistentData().putBoolean("fletcherstrestle:amphibious", true);
                    }

                    if (limb.getMaterialName().equals("Spruce")) {
                        // Stamp the arrow so we know it came from a Spruce bow
                        arrow.getPersistentData().putBoolean("fletcherstrestle:punch", true);
                    }

                    if (riser.getMaterialName().equalsIgnoreCase("Copper")) {
                        // Stamp the arrow with a custom "Conductive" tag
                        arrow.getPersistentData().putBoolean("fletcherstrestle:conductive", true);
                    }
                }
            }
        }
    }

    //Conteract water drag if amhibious

    @SubscribeEvent
    public static void onArrowTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof AbstractArrow arrow) {

            if (arrow.isInWater() && arrow.getPersistentData().getBoolean("fletcherstrestle:amphibious")) {

                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.65D));

                if (arrow.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP,
                            arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onArrowKnockback(LivingKnockBackEvent event) {
        var damageSource = event.getEntity().getLastDamageSource();

        if (damageSource != null && damageSource.getDirectEntity() instanceof AbstractArrow arrow) {

            if (arrow.getPersistentData().getBoolean("fletcherstrestle:punch")) {

                event.setStrength(event.getStrength() + 0.6F);
            }
        }
    }

    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof AbstractArrow arrow) {

            if (arrow.getPersistentData().getBoolean("fletcherstrestle:conductive")) {

                if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY && arrow.level().isThundering()) {

                    Entity hitEntity = ((EntityHitResult) event.getRayTraceResult()).getEntity();

                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(arrow.level());
                    if (lightning != null) {
                        lightning.moveTo(hitEntity.position());
                        arrow.level().addFreshEntity(lightning);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
        if (event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)) {
            event.setCanceled(true);
            if (event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)) {

                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

                if (!event.getLevel().isClientSide()) {
                    event.getEntity().openMenu(new SimpleMenuProvider(
                            (id, inv, player) -> new FletchingMenu(id, inv),
                            Component.translatable("block.minecraft.fletching_table")
                    ));
                }
            }
        }
    }
}
