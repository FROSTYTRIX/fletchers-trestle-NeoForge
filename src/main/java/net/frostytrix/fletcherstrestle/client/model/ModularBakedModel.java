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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
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
            .maximumSize(1000)
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
        // CRITICAL FIX: Pixels to Blocks conversion (divide by 16)
        float f = 1 / 16.0f;

        // Third Person: Rot[-80, 260, -40], Pos[-1, -2, 2.5], Scale[0.9]
        ItemTransform thirdPersonRight = new ItemTransform(new Vector3f(-80, 260, -40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
        ItemTransform thirdPersonLeft = new ItemTransform(new Vector3f(-80, -280, 40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));

        // First Person: Rot[0, -90, 25], Pos[1.13, 3.2, 1.13], Scale[0.68]
        ItemTransform firstPersonRight = new ItemTransform(new Vector3f(0, -90, 25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));
        ItemTransform firstPersonLeft = new ItemTransform(new Vector3f(0, 90, -25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));

        return new ItemTransforms(
                thirdPersonLeft, thirdPersonRight,
                firstPersonLeft, firstPersonRight,
                ItemTransform.NO_TRANSFORM, // Head
                ItemTransform.NO_TRANSFORM, // GUI
                ItemTransform.NO_TRANSFORM, // Ground
                ItemTransform.NO_TRANSFORM  // Fixed
        );
    }

    private class ModularItemOverrides extends ItemOverrides {
        @Override
        public @Nullable BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            String cacheKey = "";
            List<String> textures = new ArrayList<>();

            BowAssembly bow = stack.get(ModDataComponents.BOW_ASSEMBLY.get());
            if (bow != null) {
                String pullSuffix = getPullSuffix(stack, entity);

                String limbMat = bow.limbMaterial().toLowerCase().replace(" ", "_");
                String riserMat = bow.riserMaterial().toLowerCase().replace(" ", "_");
                String stringMat = bow.stringMaterial().toLowerCase().replace(" ", "_");

                // Layer 0: Limbs
                textures.add(basePath + "/limbs/" + limbMat + "_limb" + pullSuffix);
                // Layer 1: Riser (Static)
                textures.add(basePath + "/risers/" + riserMat + "_riser");
                // Layer 2: String
                textures.add(basePath + "/strings/" + stringMat + "_string" + pullSuffix);

                // --- THE ARROW FIX ---
                // If the player is pulling the bow, add the arrow texture as Layer 3
                if (!pullSuffix.isEmpty()) {
                    // This matches your: "item/modular_bow/extras/arrow_pulling_0"
                    textures.add(basePath + "/extras/arrow" + pullSuffix);
                }

                cacheKey = "bow_" + limbMat + "_" + riserMat + "_" + stringMat + pullSuffix;
            }

            ArrowAssembly arrow = stack.get(ModDataComponents.ARROW_ASSEMBLY.get());
            if (arrow != null) {
                String headMat = arrow.head().toLowerCase().replace(" ", "_");
                String shaftMat = arrow.shaft().toLowerCase().replace(" ", "_");
                String fletchMat = arrow.fletching().toLowerCase().replace(" ", "_");

                textures.add(basePath + "/shafts/" + shaftMat + "_shaft");
                textures.add(basePath + "/fletchings/" + fletchMat + "_fletching");
                textures.add(basePath + "/heads/" + headMat + "_head");

                cacheKey = "arrow_" + headMat + "_" + shaftMat + "_" + fletchMat;
            }

            if (textures.isEmpty()) return originalModel;

            BakedModel cached = cache.getIfPresent(cacheKey);
            if (cached == null) {
                cached = new WrappedBakedModel(bakeLayeredModel(textures), transforms);
                cache.put(cacheKey, cached);
            }
            return cached;
        }

        private String getPullSuffix(ItemStack stack, @Nullable LivingEntity entity) {
            if (entity != null && entity.isUsingItem() && entity.getUseItem() == stack) {
                float pull = (float)(stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0f;
                if (pull >= 0.9f) return "_pulling_2";
                if (pull >= 0.65f) return "_pulling_1";
                return "_pulling_0";
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
                    com.google.common.collect.ImmutableList.class,
                    it.unimi.dsi.fastutil.ints.Int2ObjectMap.class,
                    it.unimi.dsi.fastutil.ints.Int2ObjectMap.class
            );
            ctor.setAccessible(true);

            ItemLayerModel layerModel = ctor.newInstance(
                    com.google.common.collect.ImmutableList.copyOf(materials),
                    Int2ObjectMaps.emptyMap(),
                    Int2ObjectMaps.emptyMap()
            );

            // Using the 5-parameter bake call as you requested
            return layerModel.bake(context, baker, spriteGetter, modelState, ItemOverrides.EMPTY);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate dynamic Modular Item Model", e);
        }
    }

    private static class WrappedBakedModel implements BakedModel {
        private final BakedModel original;
        private final ItemTransforms transforms;

        public WrappedBakedModel(BakedModel original, ItemTransforms transforms) {
            this.original = original;
            this.transforms = transforms;
        }

        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return original.getQuads(state, side, rand);
        }

        // KEEPING THE BRIGHTNESS FIX
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return false; }
        @Override public boolean usesBlockLight() { return false; }

        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return original.getParticleIcon(); }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
        @Override public ItemTransforms getTransforms() { return transforms; }
    }

    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) { return Collections.emptyList(); }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean usesBlockLight() { return false; }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() {
        return spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, context.getRenderTypeHint()));
    }
    @Override public ItemOverrides getOverrides() { return this.overrides; }
    @Override public ItemTransforms getTransforms() { return this.transforms; }
}