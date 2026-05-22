package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

// 26.1 MobRenderer<T extends Mob, S extends LivingEntityRenderState,
//                  M extends EntityModel<? super S>>.
// Three type params now — entity type, render state, model. The renderer
// snapshots per-frame fields onto EagleRenderState in extractRenderState
// and the model reads them in setupAnim without touching the entity.
public class EagleRenderer extends MobRenderer<EagleEntity, EagleRenderState, EagleModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FletcherTrestle.MOD_ID,
                    "textures/entity/eagle/eagle.png");

    public EagleRenderer(EntityRendererProvider.Context context) {
        super(context, new EagleModel(context.bakeLayer(EagleModel.LAYER_LOCATION)), 0.4f);
    }

    @Override
    public EagleRenderState createRenderState() {
        return new EagleRenderState();
    }

    @Override
    public void extractRenderState(EagleEntity entity, EagleRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.flying = entity.isFlying();
        Vec3 motion = entity.getDeltaMovement();
        state.horizontalSpeed = (float) motion.horizontalDistance();
    }

    @Override
    public Identifier getTextureLocation(EagleRenderState state) {
        return TEXTURE;
    }

    // 26.1: setupRotations takes the render state, not the entity.
    @Override
    protected void setupRotations(EagleRenderState state, PoseStack pose, float bodyYaw, float scale) {
        super.setupRotations(state, pose, bodyYaw, scale);
        if (state.flying) {
            // Pitch forward up to 30° based on horizontal speed.
            float pitchAngle = Math.min(state.horizontalSpeed * 40f, 30f);
            pose.mulPose(Axis.XP.rotationDegrees(pitchAngle));
        }
    }
}
