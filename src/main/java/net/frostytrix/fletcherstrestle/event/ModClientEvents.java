package net.frostytrix.fletcherstrestle.event;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.block.ModBlocks;
import net.frostytrix.fletcherstrestle.block.entity.ModBlockEntities;
import net.frostytrix.fletcherstrestle.block.entity.renderer.DippingVatRenderer;
import net.frostytrix.fletcherstrestle.block.entity.renderer.ShavingHorseRenderer;
import net.frostytrix.fletcherstrestle.block.entity.renderer.SteamBoxRenderer;
import net.frostytrix.fletcherstrestle.client.ClientKeybinds;
import net.frostytrix.fletcherstrestle.client.ClientState;
import net.frostytrix.fletcherstrestle.client.QuiverHudOverlay;
import net.frostytrix.fletcherstrestle.client.model.ModularModelLoader;
import net.frostytrix.fletcherstrestle.client.renderer.ModularArrowRenderer;
import net.frostytrix.fletcherstrestle.attachment.ModCrossbowAttachments;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.entity.ModEntities;
import net.frostytrix.fletcherstrestle.entity.client.EagleModel;
import net.frostytrix.fletcherstrestle.entity.client.EagleRenderer;
import net.frostytrix.fletcherstrestle.item.ModItems;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.frostytrix.fletcherstrestle.network.QuiverSlotPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;

import static net.frostytrix.fletcherstrestle.FletcherTrestle.MOD_ID;

@EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SHAVING_HORSE_BE.get(), ShavingHorseRenderer::new);
        event.registerEntityRenderer(ModEntities.MODULAR_ARROW.get(), ModularArrowRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DIPPING_VAT_BE.get(), DippingVatRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STEAM_BOX_BE.get(), SteamBoxRenderer::new);
        event.registerEntityRenderer(ModEntities.EAGLE.get(), EagleRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WEAPON_RACK_BE.get(),
                net.frostytrix.fletcherstrestle.block.entity.renderer.WeaponRackRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EagleModel.LAYER_LOCATION, EagleModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "modular_loader"), ModularModelLoader.INSTANCE);
    }

    private static final ResourceLocation ARROW_SLIT_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "arrow_slit");

    // Wrap every baked arrow_slit variant with the dynamic disguise model.
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        java.util.Map<net.minecraft.client.resources.model.ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> models = event.getModels();
        for (var entry : models.entrySet()) {
            if (entry.getKey().id().equals(ARROW_SLIT_ID)) {
                entry.setValue(new net.frostytrix.fletcherstrestle.client.model.ArrowSlitBakedModel(entry.getValue()));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Horse Logic
        ClientState.isFreeLooking = ClientKeybinds.FREE_LOOK_KEY.isDown();

        while (ClientKeybinds.GALLOP_LOCK_KEY.consumeClick()) {
            ClientState.isGallopLocked = !ClientState.isGallopLocked;
        }

        //Quiver Logic
        if (ClientKeybinds.QUIVER_MODIFIER.isDown()) {
            QuiverHudOverlay.displayTicks = Math.max(QuiverHudOverlay.displayTicks, 10);
        }

        QuiverHudOverlay.slideProgressO = QuiverHudOverlay.slideProgress;

        if (QuiverHudOverlay.displayTicks > 0) {
            QuiverHudOverlay.displayTicks--;
            if (QuiverHudOverlay.slideProgress < 10.0f) {
                QuiverHudOverlay.slideProgress += 1.5f;
            }
        } else {
            if (QuiverHudOverlay.slideProgress > 0.0f) {
                QuiverHudOverlay.slideProgress -= 1.0f;
            }
        }
        QuiverHudOverlay.slideProgress = Mth.clamp(QuiverHudOverlay.slideProgress, 0.0f, 10.0f);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // 1. Check if our modifier key is actively being held down
        if (ClientKeybinds.QUIVER_MODIFIER.isDown()) {

            // 2. Verify the player actually has a quiver in their inventory
            boolean hasQuiver = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).getItem() instanceof ModularQuiverItem) {
                    hasQuiver = true;
                    break;
                }
            }

            if (hasQuiver) {
                // 3. CANCEL the vanilla event so the hotbar stops scrolling
                event.setCanceled(true);

                // 4. Calculate direction and fire the packet!
                double delta = event.getScrollDeltaY();
                if (delta != 0) {
                    // Scroll down cycles right, scroll up cycles left.
                    boolean cycleRight = delta < 0;
                    PacketDistributor.sendToServer(new QuiverSlotPacket(cycleRight));
                    // Pop the Quiver HUD open so the change is visible.
                    QuiverHudOverlay.displayTicks = 60; // 3 seconds
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();

        // Auto-disable if the player dismounts.
        if (!player.isPassenger() && ClientState.isGallopLocked) {
            ClientState.isGallopLocked = false;
        }

        // Cruise control: hold the mount moving forward.
        if (ClientState.isGallopLocked && player.getVehicle() instanceof AbstractHorse) {
            event.getInput().up = true;
            event.getInput().forwardImpulse = 1.0F;

            // Pressing 'S' (brake) cancels cruise control.
            if (event.getInput().down) {
                ClientState.isGallopLocked = false;
            }
        }
    }

    // Tints layer 3 of the modular arrow item model (the liquid overlay
    // added by ModularBakedModel) with the potion's color. Layers 0-2 are
    // the shaft / fletching / head: untouched.
    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 3) return 0xFFFFFFFF;
            net.minecraft.world.item.alchemy.PotionContents pc =
                    stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            if (pc == null) return 0xFFFFFFFF;
            // PotionContents.getColor() returns RGB; combine with full alpha.
            return 0xFF000000 | (pc.getColor() & 0xFFFFFF);
        }, ModItems.MODULAR_ARROW.get());
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0 && level != null && pos != null) {
                return BiomeColors.getAverageWaterColor(level, pos);
            }

            return 0x3F76E4;

        }, ModBlocks.STEAM_BOX.get());

        // Arrow slit: delegate tinting to whatever block it's disguised as, so
        // grass/leaves/etc. get their proper biome colour.
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null
                    && level.getBlockEntity(pos) instanceof net.frostytrix.fletcherstrestle.block.entity.ArrowSlitBlockEntity slit
                    && slit.hasMimic()) {
                return Minecraft.getInstance().getBlockColors().getColor(slit.getMimic(), level, pos, tintIndex);
            }
            return -1;
        }, ModBlocks.ARROW_SLIT.get());
    }

    /** FOV multiplier from a fitted scope (optic) attachment, or 1.0 if none. */
    private static float scopeZoomFor(Player player, ItemStack stack) {
        ResourceLocation attId = stack.get(ModDataComponents.CROSSBOW_ATTACHMENT.get());
        if (attId == null) {
            return 1.0F;
        }
        var registry = player.level().registryAccess()
                .registryOrThrow(ModCrossbowAttachments.CROSSBOW_ATTACHMENT);
        var def = registry.get(attId);
        return def != null ? def.stats().zoom() : 1.0F;
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

            // Standard vanilla zoom (up to 15%) while charging.
            event.setNewFovModifier(event.getFovModifier() * (1.0F - (progress * 0.15F)));
        }

        // Scope ADS: a loaded crossbow with a scope fitted zooms while the
        // scope toggle (keybind) is on. This is the "aim to shoot" zoom.
        ItemStack held = player.getMainHandItem();
        if (ClientState.scopeActive
                && held.getItem() instanceof ModularCrossbowItem
                && net.minecraft.world.item.CrossbowItem.isCharged(held)) {
            float scopeZoom = scopeZoomFor(player, held);
            if (scopeZoom < 1.0F) {
                event.setNewFovModifier(event.getFovModifier() * scopeZoom);
            }
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
