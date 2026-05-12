package net.frostytrix.fletcherstrestle.enchantment;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        // 1. Only run this logic on the server side
        if (level.isClientSide()) return;

        // 2. Optimization: Only execute exactly every 40 ticks (2 seconds)
        if (player.tickCount % 40 != 0) return;

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
                    int repairAmount = enchLevel * 5;
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - repairAmount));
                }
            });
        }
    }
}