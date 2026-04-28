package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.block.entity.renderer.ShavingHorseRenderer;
import net.frostytrix.fletcherstrestle.client.renderer.ModularArrowRenderer;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = FletcherTrestle.MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SHAVING_HORSE_BE.get(), ShavingHorseRenderer::new);
        event.registerEntityRenderer(ModEntities.MODULAR_ARROW.get(), ModularArrowRenderer::new);
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0 && level != null && pos != null) {
                return BiomeColors.getAverageWaterColor(level, pos);
            }

            return 0x3F76E4;

        }, ModBlocks.STEAM_BOX.get());
    }

    @SubscribeEvent
    public static void onFovModify(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = player.getUseItem();


        if (player.isUsingItem() && stack.getItem() instanceof ModularBowItem bow) {
            int ticksUsed = stack.getUseDuration(player) - player.getUseItemRemainingTicks();

            float drawTime = bow.getDrawTime(stack);

            float progress = (float) ticksUsed / drawTime;
            if (progress > 1.0F) {
                progress = 1.0F;
            } else {
                progress = progress * progress;
            }

            float fovModifier = 1.0F - (progress * 0.15F);

            event.setNewFovModifier(fovModifier);
        }
    }

    @SubscribeEvent
    public static void onMovementUpdate(MovementInputUpdateEvent event) {
        Player player = event.getEntity();

        if (player.isUsingItem() && player.getUseItem().getItem() == ModItems.MODULAR_BOW.get()) {

            var assembly = player.getUseItem().get(ModDataComponents.BOW_ASSEMBLY.get());

            if (assembly != null && assembly.limbMaterial().equals("Jungle")) {

                event.getInput().forwardImpulse *= 5.0F;
                event.getInput().leftImpulse *= 5.0F;
            }
        }
    }
}
