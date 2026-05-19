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

        // Update this line to pass the basePath!
        this.transforms = createCustomTransforms(basePath);
    }

    private ItemTransforms createCustomTransforms(String basePath) {
        float f = 1 / 16.0f;

        // NEW: Create a standard transform for when the item is dropped on the ground
        // Rotation: 0, Translation: slightly up so it doesn't clip, Scale: 50% (0.5f)
        ItemTransform groundTransform = new ItemTransform(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 2 * f, 0),
                new Vector3f(0.5f, 0.5f, 0.5f)
        );

        // If it's a crossbow, use the specific rotations and translations you provided
        if (basePath != null && basePath.contains("crossbow")) {
            ItemTransform thirdPersonRight = new ItemTransform(new Vector3f(-90, 0, -60), new Vector3f(2 * f, 0.1f * f, -3 * f), new Vector3f(0.9f, 0.9f, 0.9f));
            ItemTransform thirdPersonLeft = new ItemTransform(new Vector3f(-90, 0, 30), new Vector3f(2 * f, 0.1f * f, -3 * f), new Vector3f(0.9f, 0.9f, 0.9f));
            ItemTransform firstPersonRight = new ItemTransform(new Vector3f(-90, 0, -55), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));
            ItemTransform firstPersonLeft = new ItemTransform(new Vector3f(-90, 0, 35), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));

            return new ItemTransforms(thirdPersonLeft, thirdPersonRight, firstPersonLeft, firstPersonRight,
                    ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, groundTransform, ItemTransform.NO_TRANSFORM);
        }
        // Otherwise, fall back to the standard Bow transforms you already had
        else {
            ItemTransform thirdPersonRight = new ItemTransform(new Vector3f(-80, 260, -40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
            ItemTransform thirdPersonLeft = new ItemTransform(new Vector3f(-80, -280, 40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
            ItemTransform firstPersonRight = new ItemTransform(new Vector3f(0, -90, 25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));
            ItemTransform firstPersonLeft = new ItemTransform(new Vector3f(0, 90, -25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));

            return new ItemTransforms(thirdPersonLeft, thirdPersonRight, firstPersonLeft, firstPersonRight,
                    ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, groundTransform, ItemTransform.NO_TRANSFORM); // <-- Replaced ground argument
        }
    }

    private class ModularItemOverrides extends ItemOverrides {
        @Override
        public @Nullable BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            List<String> textures = new ArrayList<>();
            String cacheKey;

            // Grab components (they will be null if it's an unfinished/raw item)
            BowAssembly bow = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            ArrowAssembly arrow = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());

            // --- 1. MODULAR BOW ---
            if (basePath.contains("bow") && !basePath.contains("crossbow")) {
                String pull = getPullSuffix(stack, entity, 20.0f);

                // FALLBACK LOGIC: If bow is null, default to oak/spider.
                String limbMat = bow != null ? bow.limbMaterial().toLowerCase().replace(" ", "_") : "oak";
                String riserMat = bow != null ? bow.riserMaterial().toLowerCase().replace(" ", "_") : "wood";
                String stringMat = bow != null ? bow.stringMaterial().toLowerCase().replace(" ", "_") : "spider";

                // Properly using basePath and your subfolders!
                textures.add(basePath + "/limbs/" + limbMat + "_limb" + pull);
                textures.add(basePath + "/risers/" + riserMat + "_riser");
                textures.add(basePath + "/strings/" + stringMat + "_string" + pull);
                if (!pull.isEmpty()) textures.add(basePath + "/extras/arrow" + pull);

                cacheKey = "bow_" + limbMat + "_" + riserMat + "_" + stringMat + pull;
            }
            // --- 2. MODULAR CROSSBOW ---
            else if (basePath.contains("crossbow")) {
                String state = getCrossbowStateSuffix(stack, entity);

                String limbMat = bow != null ? bow.limbMaterial().toLowerCase().replace(" ", "_") : "oak";
                String riserMat = bow != null ? bow.riserMaterial().toLowerCase().replace(" ", "_") : "wood";
                String stringMat = bow != null ? bow.stringMaterial().toLowerCase().replace(" ", "_") : "spider";

                textures.add(basePath + "/limbs/" + limbMat + "_limb");
                textures.add(basePath + "/risers/" + riserMat + "_riser");

                String stringState = state.equals("_charged") ? "_pulling_2" : state;
                textures.add(basePath + "/strings/" + stringMat + "_string" + stringState);

                // We declare this outside the if-statement so the cacheKey can see it
                String loadedProjectile = "";

                // 3. Fix the Firework vs Arrow logic
                if (state.equals("_charged")) {
                    ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
                    if (projectiles != null && !projectiles.isEmpty()) {

                        boolean hasFirework = false;
                        for (ItemStack projectile : projectiles.getItems()) {
                            if (projectile.getItem() instanceof net.minecraft.world.item.FireworkRocketItem) {
                                hasFirework = true;
                                break;
                            }
                        }

                        loadedProjectile = hasFirework ? "firework" : "arrow";
                        textures.add(basePath + "/extras/" + loadedProjectile);
                    }
                }

                // CACHE FIX: We append the loadedProjectile to the key!
                cacheKey = "xbow_" + limbMat + "_" + riserMat + "_" + stringMat + state + "_" + loadedProjectile;
            }
            // --- 3. MODULAR ARROW ---
            else if (basePath.contains("arrow")) {
                String headMat = arrow != null ? arrow.head().toLowerCase().replace(" ", "_") : "flint";
                String shaftMat = arrow != null ? arrow.shaft().toLowerCase().replace(" ", "_") : "oak";
                String fletchMat = arrow != null ? arrow.fletching().toLowerCase().replace(" ", "_") : "feather";

                textures.add(basePath + "/shafts/" + shaftMat + "_shaft");
                textures.add(basePath + "/fletchings/" + fletchMat + "_fletching");
                textures.add(basePath + "/heads/" + headMat + "_head");

                cacheKey = "arrow_" + headMat + "_" + shaftMat + "_" + fletchMat;
            }
            // --- CATCH ALL ---
            else {
                return originalModel;
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