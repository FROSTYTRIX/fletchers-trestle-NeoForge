package net.frostytrix.fletcherstrestle.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.frostytrix.fletcherstrestle.item.custom.ModularQuiverItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The quiver's item model, drawn like an open bundle: the quiver's back, then the
 * selected arrow turned end for end so its fletching sticks out of the mouth,
 * then the quiver's front over its shaft.
 *
 * <p>The quiver's own model is plain {@code item/generated} with two layers,
 * {@code layer0} the back and {@code layer1} the front, and the game bakes each
 * layer with its index as the tint index: that is how the two are told apart
 * here. The arrow is whatever model the game resolves for the arrow itself, so a
 * modular arrow shows its real head, shaft and coloured fletching.</p>
 *
 * <p>Because it is the item model, the arrow shows everywhere the quiver does:
 * inventory, hand, item frames and the player's back.</p>
 */
public class QuiverBakedModel implements BakedModel {

    /** One pixel in block units, the unit the sprite is laid out in. */
    private static final float PIXEL = 1 / 16.0f;

    /**
     * Slides the turned arrow across the sprite, in pixels: positive X is right,
     * positive Y is up. At zero it sits exactly where the arrow sprite draws it,
     * rotated 180 degrees about the centre.
     */
    private static final float ARROW_SHIFT_X = 1.0f;
    private static final float ARROW_SHIFT_Y = 0.0f;

    /**
     * Depth steps that keep the three layers out of each other's plane, in block
     * units, positive toward the viewer: the back stays put, the arrow sits just
     * in front of it, the front just in front of the arrow.
     */
    private static final float ARROW_DEPTH = 0.001f;
    private static final float FRONT_DEPTH = 0.002f;

    /** The quiver layer that is its front; {@code layer0} is the back. */
    private static final int FRONT_LAYER = 1;

    private final BakedModel quiver;
    private final BakedModel empty;
    private final ItemOverrides overrides = new ArrowOverrides();
    // Keyed by the arrow's model instance, so every modular arrow assembly gets
    // its own entry and a resource reload simply lets the old ones go.
    private final Cache<BakedModel, BakedModel> withArrow = CacheBuilder.newBuilder()
            .weakKeys()
            .maximumSize(256)
            .build();

    public QuiverBakedModel(BakedModel quiver) {
        this.quiver = quiver;
        this.empty = new Layered(quiver, null);
    }

    private class ArrowOverrides extends ItemOverrides {
        @Override
        public @Nullable BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                            @Nullable LivingEntity entity, int seed) {
            ItemStack arrow = ModularQuiverItem.selectedArrows(stack);
            if (arrow.isEmpty()) {
                return empty;
            }
            BakedModel arrowModel = Minecraft.getInstance().getItemRenderer().getModel(arrow, level, entity, seed);
            BakedModel layered = withArrow.getIfPresent(arrowModel);
            if (layered == null) {
                layered = new Layered(quiver, arrowModel);
                withArrow.put(arrowModel, layered);
            }
            return layered;
        }
    }

    /** The finished stack of quads, built once per arrow model. */
    private static final class Layered implements BakedModel {
        private final BakedModel quiver;
        /** Quads per side, {@link Direction#values()} order, unculled last. */
        private final List<List<BakedQuad>> quads = new ArrayList<>();

        Layered(BakedModel quiver, @Nullable BakedModel arrow) {
            this.quiver = quiver;
            RandomSource random = RandomSource.create();
            for (int i = 0; i <= Direction.values().length; i++) {
                Direction side = i < Direction.values().length ? Direction.values()[i] : null;
                List<BakedQuad> layer = new ArrayList<>();
                List<BakedQuad> front = new ArrayList<>();
                random.setSeed(42L);
                for (BakedQuad quad : quiver.getQuads(null, side, random)) {
                    // Untinted: the quiver's item colour belongs to the arrow.
                    if (quad.getTintIndex() >= FRONT_LAYER) {
                        front.add(moved(quad, FRONT_DEPTH));
                    } else {
                        layer.add(moved(quad, 0.0f));
                    }
                }
                if (arrow != null) {
                    random.setSeed(42L);
                    for (BakedQuad quad : arrow.getQuads(null, side, random)) {
                        layer.add(turned(quad));
                    }
                }
                layer.addAll(front);
                quads.add(List.copyOf(layer));
            }
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return quads.get(side == null ? Direction.values().length : side.ordinal());
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return quiver.getRenderTypes(stack, fabulous);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return quiver.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return quiver.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return quiver.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return quiver.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return quiver.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }

    /** A quiver quad, pushed forward by {@code dz} and stripped of its tint. */
    private static BakedQuad moved(BakedQuad quad, float dz) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            int at = i * stride + 2;
            vertices[at] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[at]) + dz);
        }
        return new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade(),
                quad.hasAmbientOcclusion());
    }

    /**
     * An arrow quad turned half a turn about the sprite's centre, (x, y) to
     * (1 - x, 1 - y), and set just in front of the quiver's back. A rotation
     * keeps the winding, so only the positions, the normals and the facing move.
     * The tint index is kept: the quiver's item colour hands it to the arrow.
     */
    private static BakedQuad turned(BakedQuad quad) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            int at = i * stride;
            vertices[at] = Float.floatToRawIntBits(1.0f - Float.intBitsToFloat(vertices[at]) + ARROW_SHIFT_X * PIXEL);
            vertices[at + 1] = Float.floatToRawIntBits(1.0f - Float.intBitsToFloat(vertices[at + 1]) + ARROW_SHIFT_Y * PIXEL);
            vertices[at + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[at + 2]) + ARROW_DEPTH);
            // The packed normal is the last int of a vertex in the block format.
            vertices[at + stride - 1] = turnPackedNormal(vertices[at + stride - 1]);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), turn(quad.getDirection()), quad.getSprite(),
                quad.isShade(), quad.hasAmbientOcclusion());
    }

    /** Negates x and y of a normal packed as three signed bytes. */
    private static int turnPackedNormal(int packed) {
        int nx = (byte) (packed & 0xFF);
        int ny = (byte) ((packed >> 8) & 0xFF);
        return (packed & 0xFFFF0000) | ((-ny & 0xFF) << 8) | (-nx & 0xFF);
    }

    private static Direction turn(Direction direction) {
        return switch (direction) {
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case NORTH, SOUTH -> direction;
        };
    }

    // The bare model, before an override picks the arrow: an empty quiver.

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return empty.getQuads(state, side, random);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return quiver.getRenderTypes(stack, fabulous);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return quiver.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return quiver.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return quiver.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return quiver.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return quiver.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
