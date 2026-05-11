package net.frostytrix.fletcherstrestle.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.ItemLayerModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ModularBakedModel implements BakedModel {
    private final Function<Material, TextureAtlasSprite> spriteGetter;
    private final IGeometryBakingContext context;
    private final ModelBaker baker;
    private final ModelState modelState;
    private final String basePath;
    private final ItemOverrides overrides;
    private final ItemTransforms transforms;

    private final Cache<String, BakedModel> cache = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public ModularBakedModel(ModelBaker baker, ModelState modelState, Function<Material, TextureAtlasSprite> spriteGetter,
                             IGeometryBakingContext context, String basePath) {
        this.baker = baker;
        this.modelState = modelState;
        this.spriteGetter = spriteGetter;
        this.context = context;
        this.basePath = basePath;
        this.overrides = new ModularItemOverrides();
        this.transforms = createCustomTransforms();
    }

    private ItemTransforms createCustomTransforms() {
        float f = 1 / 16.0f;
        ItemTransform thirdPersonRight = new ItemTransform(new Vector3f(-80, 260, -40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
        ItemTransform thirdPersonLeft = new ItemTransform(new Vector3f(-80, -280, 40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
        ItemTransform firstPersonRight = new ItemTransform(new Vector3f(0, -90, 25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));
        ItemTransform firstPersonLeft = new ItemTransform(new Vector3f(0, 90, -25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));

        return new ItemTransforms(thirdPersonLeft, thirdPersonRight, firstPersonLeft, firstPersonRight,
                ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM);
    }

    private class ModularItemOverrides extends ItemOverrides {
        @Override
        public @Nullable BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            List<String> textures = new ArrayList<>();
            String cacheKey;

            // --- 1. MODULAR BOW (Matches basePath: item/modular_bow) ---
            BowAssembly bow = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (bow != null && basePath.contains("bow")) {
                String pull = getPullSuffix(stack, entity, 20.0f);
                String limbMat = bow.limbMaterial().toLowerCase().replace(" ", "_");
                String riserMat = bow.riserMaterial().toLowerCase().replace(" ", "_");
                String stringMat = bow.stringMaterial().toLowerCase().replace(" ", "_");

                textures.add(basePath + "/limbs/" + limbMat + "_limb" + pull);
                textures.add(basePath + "/risers/" + riserMat + "_riser");
                textures.add(basePath + "/strings/" + stringMat + "_string" + pull);
                if (!pull.isEmpty()) textures.add(basePath + "/extras/arrow" + pull);

                cacheKey = "bow_" + limbMat + "_" + riserMat + "_" + stringMat + pull;
            }
            // --- 2. MODULAR CROSSBOW (Matches basePath: item/modular_crossbow) ---
            else if (bow != null && basePath.contains("crossbow")) {
                String state = getCrossbowStateSuffix(stack, entity);
                String limbMat = bow.limbMaterial().toLowerCase().replace(" ", "_");
                String riserMat = bow.riserMaterial().toLowerCase().replace(" ", "_");
                String stringMat = bow.stringMaterial().toLowerCase().replace(" ", "_");

                textures.add(basePath + "/limbs/" + limbMat + "_limb" + state);
                textures.add(basePath + "/risers/" + riserMat + "_riser");
                textures.add(basePath + "/strings/" + stringMat + "_string" + state);

                if (state.equals("_charged")) {
                    ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
                    if (projectiles != null && !projectiles.isEmpty()) {
                        // Projectiles are in the root folder, not extras, per your ModItemModelProvider
                        textures.add(basePath + "/" + (projectiles.contains(Items.FIREWORK_ROCKET) ? "firework" : "arrow"));
                    }
                }
                cacheKey = "xbow_" + limbMat + "_" + riserMat + "_" + stringMat + state;
            }
            // --- 3. MODULAR ARROW (Matches basePath: item/arrow) ---
            else {
                ArrowAssembly arrow = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
                if (arrow != null) {
                    String headMat = arrow.head().toLowerCase().replace(" ", "_");
                    String shaftMat = arrow.shaft().toLowerCase().replace(" ", "_");
                    String fletchMat = arrow.fletching().toLowerCase().replace(" ", "_");

                    // Ordering based on your ModItemModelProvider layers 0, 1, 2
                    textures.add(basePath + "/shafts/" + shaftMat + "_shaft");
                    textures.add(basePath + "/fletchings/" + fletchMat + "_fletching");
                    textures.add(basePath + "/heads/" + headMat + "_head");

                    cacheKey = "arrow_" + headMat + "_" + shaftMat + "_" + fletchMat;
                } else {
                    return originalModel;
                }
            }

            BakedModel cached = cache.getIfPresent(cacheKey);
            if (cached == null) {
                cached = new WrappedBakedModel(bakeLayeredModel(textures), transforms);
                cache.put(cacheKey, cached);
            }
            return cached;
        }

        private String getPullSuffix(ItemStack stack, @Nullable LivingEntity entity, float maxPull) {
            if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
                float pull = (float)(stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / maxPull;
                if (pull >= 0.9f) return "_pulling_2";
                if (pull >= 0.65f) return "_pulling_1";
                return "_pulling_0";
            }
            return "";
        }

        private String getCrossbowStateSuffix(ItemStack stack, @Nullable LivingEntity entity) {
            if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
                return getPullSuffix(stack, entity, 25.0f);
            }
            ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (projectiles != null && !projectiles.isEmpty()) {
                return "_charged";
            }
            return "";
        }
    }

    private BakedModel bakeLayeredModel(List<String> texturePaths) {
        List<Material> materials = texturePaths.stream()
                .map(path -> new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, path)))
                .toList();
        try {
            java.lang.reflect.Constructor<ItemLayerModel> ctor = ItemLayerModel.class.getDeclaredConstructor(
                    ImmutableList.class, it.unimi.dsi.fastutil.ints.Int2ObjectMap.class, it.unimi.dsi.fastutil.ints.Int2ObjectMap.class);
            ctor.setAccessible(true);
            return ctor.newInstance(ImmutableList.copyOf(materials), Int2ObjectMaps.emptyMap(), Int2ObjectMaps.emptyMap())
                    .bake(context, baker, spriteGetter, modelState, ItemOverrides.EMPTY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to bake dynamic modular model", e);
        }
    }

    private static class WrappedBakedModel implements BakedModel {
        private final BakedModel original;
        private final ItemTransforms transforms;
        public WrappedBakedModel(BakedModel original, ItemTransforms transforms) { this.original = original; this.transforms = transforms; }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction d, RandomSource r) { return original.getQuads(s, d, r); }
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return false; }
        @Override public boolean usesBlockLight() { return false; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
        @Override public ItemTransforms getTransforms() { return transforms; }
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction d, RandomSource r) { return Collections.emptyList(); }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean usesBlockLight() { return false; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, context.getRenderTypeHint())); }
    @Override public ItemOverrides getOverrides() { return this.overrides; }
    @Override public ItemTransforms getTransforms() { return this.transforms; }
}