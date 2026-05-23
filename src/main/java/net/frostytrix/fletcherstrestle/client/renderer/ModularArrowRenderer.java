package net.frostytrix.fletcherstrestle.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.component.ArrowAssembly;
import net.frostytrix.fletcherstrestle.entity.custom.ModularArrowEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.PotionContents;

// 26.1 in-flight renderer for modular arrows. Paints the assembled head,
// shaft, fletching, and (for glass-vial arrows with a potion dipped in)
// a tinted liquid overlay, by drawing the vanilla ArrowModel up to four
// times per frame. super.submit reapplies its own push/pop and
// rotations each call, so we only need to flip the layer flag that
// getTextureLocation reads and (for the liquid pass) the outlineColor
// field that ArrowRenderer threads into submitModel as the model tint.
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

        // Liquid overlay only fires for glass-vial heads carrying a potion.
        state.hasLiquid = false;
        if ("glass_vial".equals(state.head)) {
            PotionContents contents = arrow.getPickupItemStackOrigin().get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                // PotionContents.getColor() returns RGB; OR full alpha so
                // the liquid layer doesn't render transparent.
                state.liquidColor = 0xFF000000 | (contents.getColor() & 0xFFFFFF);
                state.hasLiquid = true;
            }
        }
    }

    @Override
    public void submit(ModularArrowRenderState state,
                       PoseStack pose,
                       SubmitNodeCollector coll,
                       CameraRenderState camera) {
        // Cache outline so we can restore it after the (potentially tinted)
        // liquid pass. ArrowRenderer.submit threads state.outlineColor in
        // as the model tint argument to submitModel.
        int savedOutline = state.outlineColor;

        state.outlineColor = savedOutline; // untinted (-1 / 0 depending on glow)
        state.currentLayer = ModularArrowRenderState.Layer.SHAFT;
        super.submit(state, pose, coll, camera);
        state.currentLayer = ModularArrowRenderState.Layer.HEAD;
        super.submit(state, pose, coll, camera);
        state.currentLayer = ModularArrowRenderState.Layer.FLETCHING;
        super.submit(state, pose, coll, camera);

        if (state.hasLiquid) {
            // Tint THIS pass only with the potion color.
            state.outlineColor = state.liquidColor;
            state.currentLayer = ModularArrowRenderState.Layer.LIQUID;
            super.submit(state, pose, coll, camera);
            state.outlineColor = savedOutline; // restore for next frame
        }
    }

    @Override
    protected Identifier getTextureLocation(ModularArrowRenderState state) {
        String folder;
        String file;
        switch (state.currentLayer) {
            case HEAD -> {
                folder = "head";
                file = state.head == null ? "flint" : state.head;
            }
            case FLETCHING -> {
                folder = "fletching";
                file = state.fletching == null ? "feather" : state.fletching;
            }
            case LIQUID -> {
                // Single shared liquid texture; tint applied via state.outlineColor.
                folder = "head";
                file = "glass_vial_liquid";
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
