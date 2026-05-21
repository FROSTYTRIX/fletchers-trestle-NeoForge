package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HeavyDummyRenderer extends LivingEntityRenderer<HeavyDummyEntity, HeavyDummyModel<HeavyDummyEntity>> {
    private static final Identifier DEFAULT_TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/entity/heavy_dummy.png");

    private static final Map<UUID, Identifier> SKIN_CACHE = new HashMap<>();
    private static final Map<UUID, Boolean> FETCHING = new HashMap<>();

    public HeavyDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new HeavyDummyModel<>(context.bakeLayer(HeavyDummyModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public Identifier getTextureLocation(HeavyDummyEntity entity) {
        return entity.getSkinUUID()
                .map(uuid -> {
                    if (SKIN_CACHE.containsKey(uuid)) {
                        return SKIN_CACHE.get(uuid);
                    }

                    if (!FETCHING.getOrDefault(uuid, false)) {
                        FETCHING.put(uuid, true);
                        fetchSkinAsync(uuid);
                    }

                    return DefaultPlayerSkin.get(uuid).texture();
                })
                .orElse(DEFAULT_TEXTURE);
    }

    private void fetchSkinAsync(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            MinecraftSessionService sessionService = minecraft.getMinecraftSessionService();

            try {
                // Use 'false' for signed data to avoid strict IDE security errors
                ProfileResult result = sessionService.fetchProfile(uuid, false);

                if (result != null && result.profile() != null) {
                    GameProfile profile = result.profile();

                    // 1.21 uses a specific internal method to register the skin texture
                    // We run it on the main thread to interact with the SkinManager safely
                    minecraft.execute(() -> {
                        minecraft.getSkinManager().getOrLoad(profile).thenAccept(playerSkin -> {
                            // This code only runs once the PNG is actually downloaded
                            SKIN_CACHE.put(uuid, playerSkin.texture());
                            System.out.println("DEBUG: Texture fully loaded for " + profile.getName());
                        });
                    });
                }
            } catch (Exception e) {
                FETCHING.remove(uuid);
            }
        });
    }
}