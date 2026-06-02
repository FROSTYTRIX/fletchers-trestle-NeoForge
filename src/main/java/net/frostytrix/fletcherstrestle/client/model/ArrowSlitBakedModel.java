package net.frostytrix.fletcherstrestle.client.model;

import net.frostytrix.fletcherstrestle.block.entity.ArrowSlitBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the baked {@code arrow_slit} model. When the block entity reports a
 * mimic block, the slit geometry is re-skinned to look like that block:
 * <ul>
 *   <li>each face takes the mimic's texture(s) for that direction — all of them,
 *       so layered blocks like grass (base + tinted overlay) look right;</li>
 *   <li>UVs are projected from world position (box mapping), so a face samples
 *       the part of the texture that matches where it sits in the cube (the
 *       bottom of the slit shows the bottom of the texture, not the top);</li>
 *   <li>tint index is carried so biome colours work, and the model reports the
 *       mimic's render type(s) so transparency (glass, leaves) renders right.</li>
 * </ul>
 */
public class ArrowSlitBakedModel extends BakedModelWrapper<BakedModel> {

    public ArrowSlitBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState mimic = data.get(ArrowSlitBlockEntity.MIMIC);
        if (mimic != null && !mimic.isAir()) {
            return mimicModel(mimic).getRenderTypes(mimic, rand, ModelData.EMPTY);
        }
        return super.getRenderTypes(state, rand, data);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        BlockState mimic = data.get(ArrowSlitBlockEntity.MIMIC);
        List<BakedQuad> slitQuads = super.getQuads(state, side, rand, data, renderType);
        if (mimic == null || mimic.isAir()) {
            return slitQuads;
        }
        BakedModel mimicModel = mimicModel(mimic);
        // Only emit on render layers the mimic actually uses (avoid duplicating
        // the geometry across layers for multi-layer mimics).
        if (renderType != null && !mimicModel.getRenderTypes(mimic, rand, ModelData.EMPTY).contains(renderType)) {
            return List.of();
        }

        List<BakedQuad> out = new ArrayList<>(slitQuads.size());
        for (BakedQuad slitQuad : slitQuads) {
            List<BakedQuad> refs = mimicFaceQuads(mimicModel, mimic, slitQuad.getDirection(), rand);
            if (refs.isEmpty()) {
                out.add(reskin(slitQuad, mimicModel.getParticleIcon(ModelData.EMPTY), -1));
            } else {
                for (BakedQuad ref : refs) {
                    out.add(reskin(slitQuad, ref.getSprite(), ref.getTintIndex()));
                }
            }
        }
        return out;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState mimic = data.get(ArrowSlitBlockEntity.MIMIC);
        if (mimic != null && !mimic.isAir()) {
            return mimicModel(mimic).getParticleIcon(ModelData.EMPTY);
        }
        return super.getParticleIcon(data);
    }

    private static BakedModel mimicModel(BlockState mimic) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(mimic);
    }

    /** All of the mimic's quads for one face (e.g. grass side = base + overlay). */
    private static List<BakedQuad> mimicFaceQuads(BakedModel model, BlockState mimic, @Nullable Direction dir, RandomSource rand) {
        if (dir != null) {
            List<BakedQuad> culled = model.getQuads(mimic, dir, rand, ModelData.EMPTY, null);
            if (!culled.isEmpty()) return culled;
        }
        List<BakedQuad> general = model.getQuads(mimic, null, rand, ModelData.EMPTY, null);
        if (dir != null) {
            List<BakedQuad> matching = new ArrayList<>();
            for (BakedQuad q : general) {
                if (q.getDirection() == dir) matching.add(q);
            }
            if (!matching.isEmpty()) return matching;
        }
        return general;
    }

    /**
     * Copies the slit quad's geometry but swaps its sprite/tint to the mimic's,
     * recomputing UVs from each vertex's position so the texture lines up like a
     * full block would (box / triplanar mapping, vanilla face convention).
     */
    private static BakedQuad reskin(BakedQuad slitQuad, TextureAtlasSprite target, int tintIndex) {
        int[] verts = slitQuad.getVertices().clone();
        int stride = verts.length / 4; // BLOCK format = 8 ints / vertex
        Direction dir = slitQuad.getDirection();
        float uSpan = target.getU1() - target.getU0();
        float vSpan = target.getV1() - target.getV0();
        for (int i = 0; i < 4; i++) {
            int o = i * stride;
            float x = Float.intBitsToFloat(verts[o]) * 16f;
            float y = Float.intBitsToFloat(verts[o + 1]) * 16f;
            float z = Float.intBitsToFloat(verts[o + 2]) * 16f;
            float u16;
            float v16;
            switch (dir) {
                case DOWN -> { u16 = x;        v16 = 16f - z; }
                case UP -> { u16 = x;          v16 = z; }
                case NORTH -> { u16 = 16f - x; v16 = 16f - y; }
                case SOUTH -> { u16 = x;       v16 = 16f - y; }
                case WEST -> { u16 = z;        v16 = 16f - y; }
                case EAST -> { u16 = 16f - z;  v16 = 16f - y; }
                case null, default -> { u16 = x; v16 = 16f - y; }
            }
            verts[o + 4] = Float.floatToRawIntBits(target.getU0() + (u16 / 16f) * uSpan);
            verts[o + 5] = Float.floatToRawIntBits(target.getV0() + (v16 / 16f) * vSpan);
        }
        return new BakedQuad(verts, tintIndex, dir, target, slitQuad.isShade());
    }
}
