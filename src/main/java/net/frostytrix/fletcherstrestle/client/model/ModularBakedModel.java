package net.frostytrix.fletcherstrestle.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.component.BowAssembly;
import net.frostytrix.fletcherstrestle.component.ModDataComponents;
import net.frostytrix.fletcherstrestle.item.custom.ModularBowItem;
import net.frostytrix.fletcherstrestle.material.Materials;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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

        this.transforms = createCustomTransforms(basePath);
    }

    private ItemTransforms createCustomTransforms(String basePath) {
        float f = 1 / 16.0f;

        // Ground transform: nudged up so it doesn't clip, scaled to 50%.
        ItemTransform groundTransform = new ItemTransform(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 2 * f, 0),
                new Vector3f(0.5f, 0.5f, 0.5f)
        );

        // Crossbows use their own hand transforms.
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
            List<ResourceLocation> textures = new ArrayList<>();
            String cacheKey;

            // Grab components (they will be null if it's an unfinished/raw item)
            BowAssembly bow = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            ArrowAssembly arrow = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());

            // --- 1. MODULAR BOW ---
            if (basePath.contains("bow") && !basePath.contains("crossbow")) {
                // Pull-stage thresholds need to match the per-limb draw time
                // so the bow visually finishes drawing at the same instant
                // gameplay finishes drawing (and the FOV zoom completes).
                // Fall back to vanilla 20 ticks only when the stack isn't a
                // ModularBowItem (e.g. during the inventory icon pass).
                float maxPull = 20.0f;
                if (stack.getItem() instanceof ModularBowItem bowItem) {
                    maxPull = bowItem.getDrawTime(stack);
                }
                String pull = getPullSuffix(stack, entity, maxPull);

                // FALLBACK LOGIC: if no assembly, render oak/wood/spider.
                String limbMat = bow != null ? bow.limbMaterial() : "oak";
                String riserMat = bow != null ? bow.riserMaterial() : "wood";
                String stringMat = bow != null ? bow.stringMaterial() : "spider";

                // Each texture lookup honors a def's optional "texture"
                // override and otherwise falls back to
                // <materialNamespace>:<basePath>/<folder>/<id><suffix>.
                textures.add(Materials.bowLimbTexture(limbMat, basePath + "/limbs", "_limb" + pull));
                textures.add(Materials.bowRiserTexture(riserMat, basePath + "/risers", "_riser"));
                textures.add(Materials.bowStringTexture(stringMat, basePath + "/strings", "_string" + pull));
                if (!pull.isEmpty()) {
                    // The "arrow on the bow" silhouette stays in our namespace
                    // — no def to override it.
                    textures.add(ResourceLocation.fromNamespaceAndPath(
                            FletcherTrestle.MOD_ID, basePath + "/extras/arrow" + pull));
                }

                cacheKey = "bow_" + Materials.normaliseId(limbMat) + "_"
                        + Materials.normaliseId(riserMat) + "_"
                        + Materials.normaliseId(stringMat) + pull;
            }
            // --- 2. MODULAR CROSSBOW ---
            else if (basePath.contains("crossbow")) {
                String state = getCrossbowStateSuffix(stack, entity);

                String limbMat = bow != null ? bow.limbMaterial() : "oak";
                String riserMat = bow != null ? bow.riserMaterial() : "wood";
                String stringMat = bow != null ? bow.stringMaterial() : "spider";

                textures.add(Materials.bowLimbTexture(limbMat, basePath + "/limbs", "_limb"));
                textures.add(Materials.bowRiserTexture(riserMat, basePath + "/risers", "_riser"));

                String stringState = state.equals("_charged") ? "_pulling_2" : state;
                textures.add(Materials.bowStringTexture(stringMat, basePath + "/strings", "_string" + stringState));

                String loadedProjectile = "";
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
                        textures.add(ResourceLocation.fromNamespaceAndPath(
                                FletcherTrestle.MOD_ID, basePath + "/extras/" + loadedProjectile));
                    }
                }

                cacheKey = "xbow_" + Materials.normaliseId(limbMat) + "_"
                        + Materials.normaliseId(riserMat) + "_"
                        + Materials.normaliseId(stringMat) + state + "_" + loadedProjectile;
            }
            // --- 3. MODULAR ARROW ---
            else if (basePath.contains("arrow")) {
                String headMat = arrow != null ? arrow.head() : "flint";
                String shaftMat = arrow != null ? arrow.shaft() : "oak";
                String fletchMat = arrow != null ? arrow.fletching() : "feather";

                textures.add(Materials.arrowShaftTexture(shaftMat, basePath + "/shafts", "_shaft"));     // tint idx 0
                textures.add(Materials.arrowFletchingTexture(fletchMat, basePath + "/fletchings", "_fletching")); // tint idx 1
                textures.add(Materials.arrowHeadTexture(headMat, basePath + "/heads", "_head"));      // tint idx 2

                // Glass-vial arrows that have been dipped get a fourth layer:
                // the "liquid" silhouette tinted to the potion's color via the
                // ItemColor handler registered in ModClientEvents.
                String headIdNormalised = Materials.normaliseId(headMat);
                boolean hasLiquid = "glass_vial".equals(headIdNormalised)
                        && stack.get(DataComponents.POTION_CONTENTS) != null;
                if (hasLiquid) {
                    textures.add(ResourceLocation.fromNamespaceAndPath(
                            FletcherTrestle.MOD_ID, basePath + "/heads/glass_vial_liquid")); // tint idx 3
                }

                cacheKey = "arrow_" + headIdNormalised + "_"
                        + Materials.normaliseId(shaftMat) + "_"
                        + Materials.normaliseId(fletchMat)
                        + (hasLiquid ? "_potion" : "");
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
                float pull = (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / maxPull;
                if (pull >= 0.9f) return "_pulling_2";
                if (pull >= 0.65f) return "_pulling_1";
                return "_pulling_0";
            }
            return "";
        }

        private String getCrossbowStateSuffix(ItemStack stack, @Nullable LivingEntity entity) {
            if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
                // Use the crossbow's actual draw length (use duration minus the
                // 3-tick hold buffer) so the load animation matches gameplay charge
                // time — including a magazine's slower (reload_multiplier) draw.
                float maxPull = 25.0f;
                if (stack.getItem() instanceof net.frostytrix.fletcherstrestle.item.custom.ModularCrossbowItem xbow) {
                    maxPull = Math.max(1.0f, xbow.getUseDuration(stack, entity) - 3);
                }
                return getPullSuffix(stack, entity, maxPull);
            }
            ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (projectiles != null && !projectiles.isEmpty()) {
                return "_charged";
            }
            return "";
        }
    }

    private BakedModel bakeLayeredModel(List<ResourceLocation> texturePaths) {
        List<Material> materials = texturePaths.stream()
                .map(loc -> new Material(InventoryMenu.BLOCK_ATLAS, loc))
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

    private record WrappedBakedModel(BakedModel original, ItemTransforms transforms) implements BakedModel {

        @Override
            public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction d, RandomSource r) {
                return original.getQuads(s, d, r);
            }

            @Override
            public boolean useAmbientOcclusion() {
                return false;
            }

            @Override
            public boolean isGui3d() {
                return false;
            }

            @Override
            public boolean usesBlockLight() {
                return false;
            }

            @Override
            public boolean isCustomRenderer() {
                return false;
            }

            @Override
            public TextureAtlasSprite getParticleIcon() {
                return original.getParticleIcon();
            }

            @Override
            public ItemOverrides getOverrides() {
                return ItemOverrides.EMPTY;
            }

            @Override
            public ItemTransforms getTransforms() {
                return transforms;
            }
        }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState s, @Nullable Direction d, RandomSource r) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, context.getRenderTypeHint()));
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }
}