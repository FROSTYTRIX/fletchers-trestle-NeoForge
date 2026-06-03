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
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HeavyDummyRenderer extends LivingEntityRenderer<HeavyDummyEntity, HeavyDummyModel<HeavyDummyEntity>> {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "textures/entity/heavy_dummy.png");

    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new HashMap<>();
    private static final Map<UUID, Boolean> FETCHING = new HashMap<>();

    public HeavyDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new HeavyDummyModel<>(context.bakeLayer(HeavyDummyModel.LAYER_LOCATION)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(HeavyDummyEntity entity) {
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
                ProfileResult result = sessionService.fetchProfile(uuid, false);

                if (result != null && result.profile() != null) {
                    GameProfile profile = result.profile();
                    // Resolve the skin on the main thread so SkinManager is touched safely.
                    minecraft.execute(() ->
                            minecraft.getSkinManager().getOrLoad(profile).thenAccept(playerSkin ->
                                    SKIN_CACHE.put(uuid, playerSkin.texture())));
                }
            } catch (Exception e) {
                FETCHING.remove(uuid);
            }
        });
    }
}