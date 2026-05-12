package net.frostytrix.fletcherstrestle.enchantment;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        // Only run on the server, and only if the joining entity is an arrow
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) return;

        // Check if the shooter was a player
        if (arrow.getOwner() instanceof Player player) {

            // In 1.21.1, arrows natively keep track of the exact weapon ItemStack that fired them!
            ItemStack bow = arrow.getWeaponItem();

            if (bow != null && !bow.isEmpty()) {
                Registry<Enchantment> registry = event.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

                registry.getHolder(ModEnchantments.BIOLUMINESCENCE).ifPresent(enchantHolder -> {
                    // Does the bow have Bioluminescence?
                    if (bow.getEnchantmentLevel(enchantHolder) > 0) {

                        // If the player successfully consumed a torch, tag the flying arrow!
                        if (consumeTorch(player)) {
                            arrow.addTag("fletcherstrestle:carries_torch");
                        }
                    }
                });
            }
        }
    }

    // Helper method to find and delete exactly 1 torch
    private static boolean consumeTorch(Player player) {
        // Free torches for creative mode
        if (player.isCreative()) return true;

        // Check offhand first (common for spelunking)
        if (player.getOffhandItem().is(Items.TORCH)) {
            player.getOffhandItem().shrink(1);
            return true;
        }

        // Scan main inventory
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(Items.TORCH)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // --- 2. THE IMPACT LOGIC ---
    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) return;
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        Level level = arrow.level();

        ItemStack weapon = arrow.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

            registry.getHolder(ModEnchantments.GALE_FORCE).ifPresent(enchantHolder -> {
                int galeLevel = weapon.getEnchantmentLevel(enchantHolder);
                if (galeLevel > 0) {

                    double hitX, hitY, hitZ;

                    // 1. THE BULLETPROOF FIX: Use the exact RayTrace hit location, not the arrow's body!
                    if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) event.getRayTraceResult();
                        Direction face = blockHit.getDirection();

                        // getLocation() gets the exact surface pixel of the block
                        hitX = blockHit.getLocation().x() + (face.getStepX() * 0.1);
                        hitY = blockHit.getLocation().y() + (face.getStepY() * 0.1);
                        hitZ = blockHit.getLocation().z() + (face.getStepZ() * 0.1);

                    } else if (event.getRayTraceResult().getType() == HitResult.Type.ENTITY) {
                        // If we hit a zombie, just explode exactly where it hit them
                        EntityHitResult entityHit = (EntityHitResult) event.getRayTraceResult();
                        hitX = entityHit.getLocation().x();
                        hitY = entityHit.getLocation().y();
                        hitZ = entityHit.getLocation().z();

                    } else {
                        // Fallback (should rarely happen, but prevents crashes)
                        hitX = arrow.getX();
                        hitY = arrow.getY();
                        hitZ = arrow.getZ();
                    }

                    // 2. THE POWER TUNING
                    float blastRadius = 0.5f + (galeLevel * 0.55f);

                    // Trigger the burst at the exact surface coordinate
                    level.explode(
                            arrow,
                            null,
                            null,
                            hitX,
                            hitY,
                            hitZ,
                            blastRadius,
                            false,
                            Level.ExplosionInteraction.TRIGGER,
                            ParticleTypes.GUST_EMITTER_SMALL,
                            ParticleTypes.GUST_EMITTER_LARGE,
                            SoundEvents.WIND_CHARGE_BURST
                    );

                    arrow.discard();
                }
            });
        }

        // Does this arrow carry a torch?
        if (arrow.getTags().contains("fletcherstrestle:carries_torch")) {

            // Did it hit a block? (Not an entity or the sky)
            if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) event.getRayTraceResult();

                BlockPos hitPos = blockHit.getBlockPos();
                Direction hitFace = blockHit.getDirection();
                BlockPos placePos = hitPos.relative(hitFace); // The empty air block where the torch should go

                // Strip the tag immediately so it doesn't trigger again if the arrow falls
                arrow.removeTag("fletcherstrestle:carries_torch");

                // Determine orientation
                BlockState torchState = null;
                if (hitFace == Direction.UP) {
                    torchState = Blocks.TORCH.defaultBlockState(); // Floor torch
                } else if (hitFace != Direction.DOWN) {
                    // Wall torch. The "FACING" property matches the block face we hit
                    torchState = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, hitFace);
                }

                boolean placedSuccessfully = false;

                // Validate placement: Does a torch state exist (not a ceiling) AND is the space empty (canBeReplaced)?
                if (torchState != null && level.getBlockState(placePos).canBeReplaced()) {

                    // Validate physics: Can the torch actually survive on the surface it hit? (e.g. not a glass block)
                    if (torchState.canSurvive(level, placePos)) {
                        level.setBlock(placePos, torchState, 3);
                        level.playSound(null, placePos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        placedSuccessfully = true;
                    }
                }

                // If it was a ceiling, a non-solid block, or occupied space, drop the torch on the ground
                if (!placedSuccessfully) {
                    ItemEntity droppedTorch = new ItemEntity(
                            level,
                            placePos.getX() + 0.5,
                            placePos.getY() + 0.5,
                            placePos.getZ() + 0.5,
                            new ItemStack(Items.TORCH)
                    );
                    level.addFreshEntity(droppedTorch);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        // 1. Only run this logic on the server side
        if (level.isClientSide()) return;

        // 2. Optimization: Only execute exactly every 40 ticks (2 seconds)
        if (player.tickCount % 200 != 0) return;

        // 3. Environment Check: Is it daytime and can the player see the sky?
        if (!level.isDay() || !level.canSeeSky(player.blockPosition())) return;

        // 4. Check the item in the main hand
        ItemStack stack = player.getMainHandItem();

        // We only care if the item is actually damaged
        if (!stack.isEmpty() && stack.isDamaged()) {

            // 1.21.1 requires querying the dynamic registry to get the Enchantment Holder
            Registry<net.minecraft.world.item.enchantment.Enchantment> registry =
                    level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

            registry.getHolder(ModEnchantments.PHOTOSYNTHESIS).ifPresent(enchantmentHolder -> {
                int enchLevel = stack.getEnchantmentLevel(enchantmentHolder);

                if (enchLevel > 0) {
                    // --- NEW: Component Validation ---
                    // If it's a modular bow, check its parts before repairing
                    if (stack.has(ModDataComponents.BOW_ASSEMBLY.get())) {
                        BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
                        String riser = assembly.riserMaterial().toLowerCase();
                        String limbs = assembly.limbMaterial().toLowerCase();

                        boolean isWoodRiser = !riser.contains("copper");
                        boolean isValidLimb = !limbs.contains("crimson") && !limbs.contains("warped");

                        // If it has metal parts or nether wood, abort the repair immediately!
                        if (!isWoodRiser || !isValidLimb) {
                            return;
                        }
                    }
                    // --- END NEW ---

                    // Proceed with normal healing
                    int repairAmount = enchLevel;
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - repairAmount));
                }
            });
        }
    }
}