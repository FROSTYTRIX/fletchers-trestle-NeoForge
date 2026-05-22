package net.frostytrix.fletcherstrestle.enchantment;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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

// Bow-enchantment behaviours that don't fit on the items themselves:
//   * Bioluminescence — first arrow fired consumes a torch and tags the
//     arrow; on impact the torch is placed (or dropped) where it hit.
//   * Gale Force — arrow impact triggers a small wind explosion.
//   * Photosynthesis — slow daytime durability repair (limited to
//     wood / non-nether modular bow parts).
//
// 26.1 changes from 1.21.1:
//   * Registry<T>#getHolder(ResourceKey) → registryAccess().lookupOrThrow
//     (Registries.ENCHANTMENT).get(ResourceKey) returning Optional<Holder>.
//   * ItemStack#getEnchantmentLevel(Holder) was removed; the equivalent
//     is EnchantmentHelper.getItemEnchantmentLevel(Holder, ItemStack).
//   * Inventory.items is private — iterate via getContainerSize/getItem.
//   * Entity#getTags() renamed to Entity#entityTags().
//   * Level#explode no longer accepts the wide (entity, dmgSource,
//     calc, x, y, z, radius, fire, interaction, particle, particleLarge,
//     sound) overload with loose particle+sound args; the basic
//     (entity, x, y, z, radius, interaction) form is the easiest path
//     here. The gust visual is replaced with a manual playSound call —
//     gust particles are a follow-up.
@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModEvents {

    private static final String TORCH_TAG = "fletcherstrestle:carries_torch";

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;

        ItemStack bow = arrow.getWeaponItem();
        if (bow == null || bow.isEmpty()) return;

        lookupEnchantment(event.getLevel(), ModEnchantments.BIOLUMINESCENCE).ifPresent(holder -> {
            if (EnchantmentHelper.getItemEnchantmentLevel(holder, bow) > 0 && consumeTorch(player)) {
                arrow.addTag(TORCH_TAG);
            }
        });
    }

    /** Offhand first, then anywhere in the player's inventory. Creative
     *  mode pays no torch cost. */
    private static boolean consumeTorch(Player player) {
        if (player.isCreative()) return true;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.TORCH)) {
            offhand.shrink(1);
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TORCH)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onArrowImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) return;
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        Level level = arrow.level();

        ItemStack weapon = arrow.getWeaponItem();
        if (weapon != null && !weapon.isEmpty()) {
            lookupEnchantment(level, ModEnchantments.GALE_FORCE).ifPresent(holder -> {
                int galeLevel = EnchantmentHelper.getItemEnchantmentLevel(holder, weapon);
                if (galeLevel <= 0) return;

                double hitX, hitY, hitZ;
                HitResult hit = event.getRayTraceResult();
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    Direction face = blockHit.getDirection();
                    hitX = blockHit.getLocation().x() + face.getStepX() * 0.1;
                    hitY = blockHit.getLocation().y() + face.getStepY() * 0.1;
                    hitZ = blockHit.getLocation().z() + face.getStepZ() * 0.1;
                } else if (hit.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) hit;
                    hitX = entityHit.getLocation().x();
                    hitY = entityHit.getLocation().y();
                    hitZ = entityHit.getLocation().z();
                } else {
                    hitX = arrow.getX();
                    hitY = arrow.getY();
                    hitZ = arrow.getZ();
                }

                float blastRadius = 0.5f + (galeLevel * 0.55f);
                level.explode(arrow, hitX, hitY, hitZ, blastRadius, Level.ExplosionInteraction.TRIGGER);
                level.playSound(null, hitX, hitY, hitZ,
                        SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
                arrow.discard();
            });
        }

        // Bioluminescence torch placement.
        if (arrow.entityTags().contains(TORCH_TAG)
                && event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) event.getRayTraceResult();
            BlockPos hitPos = blockHit.getBlockPos();
            Direction hitFace = blockHit.getDirection();
            BlockPos placePos = hitPos.relative(hitFace);

            arrow.removeTag(TORCH_TAG);

            BlockState torchState = null;
            if (hitFace == Direction.UP) {
                torchState = Blocks.TORCH.defaultBlockState();
            } else if (hitFace != Direction.DOWN) {
                torchState = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, hitFace);
            }

            boolean placed = false;
            if (torchState != null && level.getBlockState(placePos).canBeReplaced()
                    && torchState.canSurvive(level, placePos)) {
                level.setBlock(placePos, torchState, 3);
                level.playSound(null, placePos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                placed = true;
            }
            if (!placed) {
                level.addFreshEntity(new ItemEntity(level,
                        placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5,
                        new ItemStack(Items.TORCH)));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;
        if (player.tickCount % 200 != 0) return;
        // 26.1: Level.isDay() renamed to isBrightOutside().
        if (!level.isBrightOutside() || !level.canSeeSky(player.blockPosition())) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamaged()) return;

        lookupEnchantment(level, ModEnchantments.PHOTOSYNTHESIS).ifPresent(holder -> {
            int enchLevel = EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
            if (enchLevel <= 0) return;

            // Modular bow gate: skip the repair if the assembly uses copper
            // (metal riser) or crimson/warped (nether wood) — those aren't
            // photosynthetic.
            if (stack.has(ModDataComponents.BOW_ASSEMBLY.get())) {
                BowAssembly assembly = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
                if (assembly != null) {
                    String riser = assembly.riserMaterial().toLowerCase();
                    String limbs = assembly.limbMaterial().toLowerCase();
                    boolean isWoodRiser = !riser.contains("copper");
                    boolean isValidLimb = !limbs.contains("crimson") && !limbs.contains("warped");
                    if (!isWoodRiser || !isValidLimb) return;
                }
            }

            stack.setDamageValue(Math.max(0, stack.getDamageValue() - enchLevel));
        });
    }

    /** Look up an enchantment Holder via the level's registry access. */
    private static java.util.Optional<Holder.Reference<Enchantment>> lookupEnchantment(
            Level level, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
    }
}
