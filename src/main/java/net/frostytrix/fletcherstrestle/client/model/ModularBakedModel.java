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

    /**
     * Vanilla's item/generated gives every item a 180 degree Y flip in the FIXED
     * context (item frames, display blocks). Without it our modular weapons sat
     * backwards relative to vanilla bows wherever FIXED is used.
     */
    private static final ItemTransform FIXED_TRANSFORM = new ItemTransform(
            new Vector3f(0, 180, 0), new Vector3f(0, 0, 0), new Vector3f(1, 1, 1));

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
                    ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, groundTransform, FIXED_TRANSFORM);
        }
        // Otherwise, fall back to the standard Bow transforms you already had
        else {
            ItemTransform thirdPersonRight = new ItemTransform(new Vector3f(-80, 260, -40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
            ItemTransform thirdPersonLeft = new ItemTransform(new Vector3f(-80, -280, 40), new Vector3f(-1 * f, -2 * f, 2.5f * f), new Vector3f(0.9f, 0.9f, 0.9f));
            ItemTransform firstPersonRight = new ItemTransform(new Vector3f(0, -90, 25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));
            ItemTransform firstPersonLeft = new ItemTransform(new Vector3f(0, 90, -25), new Vector3f(1.13f * f, 3.2f * f, 1.13f * f), new Vector3f(0.68f, 0.68f, 0.68f));

            return new ItemTransforms(thirdPersonLeft, thirdPersonRight, firstPersonLeft, firstPersonRight,
                    ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, groundTransform, FIXED_TRANSFORM); // <-- Replaced ground argument
        }
    }

    private class ModularItemOverrides extends ItemOverrides {
        @Override
        public @Nullable BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            List<ResourceLocation> textures = new ArrayList<>();
            // Layers flipped about the diagonal, to draw a half-limb as both limbs.
            List<ResourceLocation> mirroredTextures = new ArrayList<>();
            // Layers shifted bodily across the sprite: the crossbow's stock.
            List<ResourceLocation> stockTextures = new ArrayList<>();
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
                // Limb textures cover only one half of the bow. The other half
                // is the same texture mirrored about its diagonal, which is why
                // a composite can show two woods without any extra art: the
                // lower limb is simply the second wood's half, flipped.
                String lowerLimbMat = bow != null ? bow.secondLimb().orElse(limbMat) : limbMat;
                mirroredTextures.add(Materials.bowLimbTexture(lowerLimbMat, basePath + "/limbs", "_limb" + pull));

                textures.add(Materials.bowLimbTexture(limbMat, basePath + "/limbs", "_limb" + pull));
                textures.add(Materials.bowRiserTexture(riserMat, basePath + "/risers", "_riser"));
                textures.add(Materials.bowStringTexture(stringMat, basePath + "/strings", "_string" + pull));
                if (!pull.isEmpty()) {
                    // The "arrow on the bow" silhouette stays in our namespace
                    //: no def to override it.
                    textures.add(ResourceLocation.fromNamespaceAndPath(
                            FletcherTrestle.MOD_ID, basePath + "/extras/arrow" + pull));
                }

                cacheKey = "bow_" + Materials.normaliseId(limbMat) + "_"
                        + Materials.normaliseId(lowerLimbMat) + "_"
                        + Materials.normaliseId(riserMat) + "_"
                        + Materials.normaliseId(stringMat) + pull;
            }
            // --- 2. MODULAR CROSSBOW ---
            else if (basePath.contains("crossbow")) {
                String state = getCrossbowStateSuffix(stack, entity);

                String limbMat = bow != null ? bow.limbMaterial() : "oak";
                String riserMat = bow != null ? bow.riserMaterial() : "wood";
                String stringMat = bow != null ? bow.stringMaterial() : "spider";

                // The stock is no longer painted into every limb texture: it is
                // the mechanical trigger, shifted so its grip sits behind the
                // prod. One texture instead of eleven copies of the same body.
                stockTextures.add(ResourceLocation.fromNamespaceAndPath(
                        FletcherTrestle.MOD_ID, "item/mechanical_trigger"));

                // Like the bow, the limb texture is half a prod drawn twice:
                // once as-is, once flipped about the diagonal.
                String lowerLimbMat = bow != null ? bow.secondLimb().orElse(limbMat) : limbMat;
                textures.add(Materials.bowLimbTexture(limbMat, basePath + "/limbs", "_limb"));
                mirroredTextures.add(Materials.bowLimbTexture(lowerLimbMat, basePath + "/limbs", "_limb"));

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
                        + Materials.normaliseId(lowerLimbMat) + "_"
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
                BakedModel base = bakeLayeredModel(textures, modelState);
                List<ExtraLayer> extras = new ArrayList<>();
                if (!stockTextures.isEmpty()) {
                    extras.add(new ExtraLayer(bakeLayeredModel(stockTextures, modelState),
                            translated(STOCK_OFFSET_X, STOCK_OFFSET_Y, STOCK_DEPTH_OFFSET)));
                }
                if (!mirroredTextures.isEmpty()) {
                    extras.add(new ExtraLayer(bakeLayeredModel(mirroredTextures, modelState),
                            ModularBakedModel::flipAboutDiagonal));
                }
                if (!extras.isEmpty()) {
                    base = new StackedBakedModel(base, extras);
                }
                cached = new WrappedBakedModel(base, transforms);
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
                // time, including a magazine's slower (reload_multiplier) draw.
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

    /**
     * One pixel in block units, the unit the sprite is laid out in.
     */
    private static final float PIXEL = 1 / 16.0f;

    /**
     * How far the flipped lower limb is nudged clear of the layer slab the
     * other layers share, in block units: a sixty-fourth of a pixel each way.
     * Far too small to see, big enough that nothing has to break a tie against
     * the riser.
     *
     * <p>One constant per axis, so any of them can be retuned, or negated to
     * send the limb the other way, without disturbing the others. Positive X
     * is right, positive Y is up, positive Z is toward the viewer.</p>
     */
    private static final float X_OFFSET = -0.001f;
    private static final float Y_OFFSET = 0.001f;
    private static final float DEPTH_OFFSET = -0.001f;

    /**
     * Where the crossbow's stock sits. It is the mechanical trigger texture
     * shifted two pixels right and down, so its grip falls behind the prod
     * instead of on top of it, and pushed further back than the lower limb so
     * every other layer paints over it.
     */
    private static final float STOCK_OFFSET_X = 2 * PIXEL;
    private static final float STOCK_OFFSET_Y = -2 * PIXEL;
    private static final float STOCK_DEPTH_OFFSET = -0.002f;

    /** A per-quad edit applied to one layer after it is baked. */
    @FunctionalInterface
    private interface QuadOp {
        BakedQuad apply(BakedQuad quad);
    }

    /** A separately-baked layer and the edit that positions it. */
    private record ExtraLayer(BakedModel model, QuadOp op) {
    }

    /**
     * Draws the main layer stack, then any separately-baked layers on top.
     *
     * <p>Layers that are not simply stacked in place have to be baked on their
     * own, because a bake applies one model state to everything in it. Their
     * edits are applied here, to the finished quads, which keeps the maths off
     * the axes it does not belong on: a flip that only touches x and y cannot
     * disturb a layer's depth, and a translation cannot disturb its winding.
     * Depth order between the layers comes from their own offsets rather than
     * from the order they are emitted in.</p>
     */
    private record StackedBakedModel(BakedModel base, List<ExtraLayer> extras) implements BakedModel {
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            List<BakedQuad> quads = new ArrayList<>(base.getQuads(state, side, random));
            for (ExtraLayer extra : extras) {
                for (BakedQuad quad : extra.model().getQuads(state, side, random)) {
                    quads.add(extra.op().apply(quad));
                }
            }
            return quads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return base.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return base.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return base.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return base.getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }


    /**
     * Reflects one quad about the sprite's anti-diagonal: (x, y) becomes
     * (1 - y, 1 - x) in block space, plus the small nudge clear of the riser's
     * plane.
     *
     * <p>Three things travel together. The positions carry the flip. The
     * vertex order is reversed, because a reflection turns the winding inside
     * out and the item render type culls back faces, which is what made the
     * limb look hollow. The normals are reflected the same way as the
     * positions, because the item shader lights a quad from its normals: leave
     * them and the limb is lit as though its front were its back, which reads
     * as the shading landing on the wrong side.</p>
     */
    private static BakedQuad flipAboutDiagonal(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int[] flipped = new int[vertices.length];
        for (int i = 0; i < 4; i++) {
            int out = i * stride;
            System.arraycopy(vertices, (3 - i) * stride, flipped, out, stride);

            float x = Float.intBitsToFloat(flipped[out]);
            float y = Float.intBitsToFloat(flipped[out + 1]);
            float z = Float.intBitsToFloat(flipped[out + 2]);
            flipped[out] = Float.floatToRawIntBits(1.0f - y + X_OFFSET);
            flipped[out + 1] = Float.floatToRawIntBits(1.0f - x + Y_OFFSET);
            flipped[out + 2] = Float.floatToRawIntBits(z + DEPTH_OFFSET);

            // The packed normal is the last int of a vertex in the block format.
            flipped[out + stride - 1] = reflectPackedNormal(flipped[out + stride - 1]);
        }
        return new BakedQuad(flipped, quad.getTintIndex(), reflect(quad.getDirection()),
                quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    /**
     * Slides a whole layer, leaving its winding, normals and facing alone: a
     * translation changes none of them.
     */
    private static QuadOp translated(float dx, float dy, float dz) {
        return quad -> {
            int[] moved = quad.getVertices().clone();
            int stride = moved.length / 4;
            for (int i = 0; i < 4; i++) {
                int out = i * stride;
                moved[out] = Float.floatToRawIntBits(Float.intBitsToFloat(moved[out]) + dx);
                moved[out + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(moved[out + 1]) + dy);
                moved[out + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(moved[out + 2]) + dz);
            }
            return new BakedQuad(moved, quad.getTintIndex(), quad.getDirection(),
                    quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
        };
    }

    /** Applies (x, y) -> (-y, -x) to a normal packed as three signed bytes. */
    private static int reflectPackedNormal(int packed) {
        int nx = (byte) (packed & 0xFF);
        int ny = (byte) ((packed >> 8) & 0xFF);
        int nz = (packed >> 16) & 0xFF;
        return (packed & 0xFF000000) | (nz << 16) | ((-nx & 0xFF) << 8) | (-ny & 0xFF);
    }

    /** The same reflection applied to a face direction. Front and back are unmoved. */
    private static Direction reflect(Direction direction) {
        return switch (direction) {
            case EAST -> Direction.DOWN;
            case WEST -> Direction.UP;
            case UP -> Direction.WEST;
            case DOWN -> Direction.EAST;
            case NORTH, SOUTH -> direction;
        };
    }

    private BakedModel bakeLayeredModel(List<ResourceLocation> texturePaths) {
        return bakeLayeredModel(texturePaths, modelState);
    }

    private BakedModel bakeLayeredModel(List<ResourceLocation> texturePaths, ModelState state) {
        List<Material> materials = texturePaths.stream()
                .map(loc -> new Material(InventoryMenu.BLOCK_ATLAS, loc))
                .toList();
        try {
            java.lang.reflect.Constructor<ItemLayerModel> ctor = ItemLayerModel.class.getDeclaredConstructor(
                    ImmutableList.class, it.unimi.dsi.fastutil.ints.Int2ObjectMap.class, it.unimi.dsi.fastutil.ints.Int2ObjectMap.class);
            ctor.setAccessible(true);
            return ctor.newInstance(ImmutableList.copyOf(materials), Int2ObjectMaps.emptyMap(), Int2ObjectMaps.emptyMap())
                    .bake(context, baker, spriteGetter, state, ItemOverrides.EMPTY);
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