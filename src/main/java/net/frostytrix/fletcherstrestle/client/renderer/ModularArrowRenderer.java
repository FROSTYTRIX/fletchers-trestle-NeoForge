package net.frostytrix.fletcherstrestle.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

// 26.1 in-flight renderer for modular arrows. Paints the assembled head,
// shaft, and fletching by drawing the vanilla ArrowModel three times,
// once per layer, with getTextureLocation switching between the three
// sub-textures. The PNGs are designed so only the relevant region is
// opaque, so the layers compose into a complete arrow without Z-fighting
// artifacts.
public class ModularArrowRenderer extends ArrowRenderer<ModularArrowEntity, ModularArrowRenderState> {

    public ModularArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ModularArrowRenderState createRenderState() {
        return new ModularArrowRenderState();
    }

    @Override
    public void extractRenderState(ModularArrowEntity arrow, ModularArrowRenderState state, float partialTick) {
        super.extractRenderState(arrow, state, partialTick);
        ArrowAssembly assembly = arrow.getAssembly();
        if (assembly != null) {
            state.shaft = assembly.shaft();
            state.head = assembly.head();
            state.fletching = assembly.fletching();
        }
    }

    @Override
    public void submit(ModularArrowRenderState state,
                       PoseStack pose,
                       SubmitNodeCollector coll,
                       CameraRenderState camera) {
        // Three passes of the same arrow geometry with the texture rotated
        // through shaft → head → fletching. super.submit reapplies its own
        // push/pop and rotations each time, so we only need to flip the
        // layer flag that getTextureLocation reads.
        state.currentLayer = ModularArrowRenderState.Layer.SHAFT;
        super.submit(state, pose, coll, camera);
        state.currentLayer = ModularArrowRenderState.Layer.HEAD;
        super.submit(state, pose, coll, camera);
        state.currentLayer = ModularArrowRenderState.Layer.FLETCHING;
        super.submit(state, pose, coll, camera);
    }

    @Override
    protected Identifier getTextureLocation(ModularArrowRenderState state) {
        String folder;
        String file;
        switch (state.currentLayer) {
            case HEAD -> {
                folder = "head";
                // The item folder uses '<id>_head.png' but the entity textures
                // are organised differently — entity/projectiles/head/<id>.png.
                file = state.head == null ? "flint" : state.head;
            }
            case FLETCHING -> {
                folder = "fletching";
                file = state.fletching == null ? "feather" : state.fletching;
            }
            case SHAFT -> {
                folder = "shaft";
                file = state.shaft == null ? "oak" : state.shaft;
            }
            default -> { folder = "shaft"; file = "oak"; }
        }
        return Identifier.fromNamespaceAndPath(
                FletcherTrestle.MOD_ID,
                "textures/entity/projectiles/" + folder + "/" + file + ".png");
    }
}
