package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.block.entity.renderer.ShavingHorseRenderer;
import net.frostytrix.fletcherstrestle.client.ClientKeybinds;
import net.frostytrix.fletcherstrestle.client.ClientState;
import net.frostytrix.fletcherstrestle.client.model.ModularModelLoader;
import net.frostytrix.fletcherstrestle.client.renderer.ModularArrowRenderer;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.frostytrix.fletcherstrestle.network.FletchingTabPayload;
import net.frostytrix.fletcherstrestle.network.QuiverSlotPacket;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

import static net.frostytrix.fletcherstrestle.FletcherTrestle.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SHAVING_HORSE_BE.get(), ShavingHorseRenderer::new);
        event.registerEntityRenderer(ModEntities.MODULAR_ARROW.get(), ModularArrowRenderer::new);
    }

    @SubscribeEvent
    public static void registerPayloads(final net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        final net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar(MOD_ID);
        registrar.playToServer(FletchingTabPayload.TYPE, FletchingTabPayload.STREAM_CODEC, FletchingTabPayload::handleData);
        registrar.playToServer(QuiverSlotPacket.TYPE, QuiverSlotPacket.CODEC, QuiverSlotPacket::handle);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "modular_loader"), ModularModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientState.isFreeLooking = ClientKeybinds.FREE_LOOK_KEY.isDown();

        while (ClientKeybinds.GALLOP_LOCK_KEY.consumeClick()) {
            ClientState.isGallopLocked = !ClientState.isGallopLocked;
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();

        // 1. Safety feature: Auto-disable if you jump off the horse
        if (!player.isPassenger() && ClientState.isGallopLocked) {
            ClientState.isGallopLocked = false;
        }

        // 2. The Cruise Control Logic
        if (ClientState.isGallopLocked && player.getVehicle() instanceof AbstractHorse) {

            // Force the game to think 'W' is being held down
            event.getInput().up = true;
            event.getInput().forwardImpulse = 1.0F;

            // Optional quality of life: If you manually press 'S' (brake), turn off cruise control
            if (event.getInput().down) {
                ClientState.isGallopLocked = false;
            }
        }
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

        if (player.isUsingItem() && stack.getItem() instanceof ModularCrossbowItem crossbow) {
            float f = (float) (stack.getUseDuration(player) - player.getUseItemRemainingTicks());
            float maxDrawTime = crossbow.getUseDuration(stack, player) - 3;
            float progress = f / maxDrawTime;

            if (progress > 1.0F) {
                progress = 1.0F;
            } else {
                progress = progress * progress;
            }

            // Standard Vanilla Zoom Math (reduces FOV by up to 15%)
            event.setNewFovModifier(event.getFovModifier() * (1.0F - (progress * 0.15F)));
        }

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
