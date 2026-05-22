package net.frostytrix.fletcherstrestle.entity.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

import static net.frostytrix.fletcherstrestle.FletcherTrestle.MOD_ID;

// Eagle model — 64×64 texture sheet, body / head+beak / neck / tail /
// two wings (upper + lower segment each) / two legs+talons. Same
// geometry as the 1.21.1 implementation. Animation hook moved to the
// 26.1 setupAnim(EagleRenderState) signature: the model reads boolean
// flying + horizontal speed off the render state instead of touching
// the entity directly.
//
// 26.1 EntityModel<S extends EntityRenderState>:
//   * Single-arg setupAnim takes the render state.
//   * renderToBuffer is final on Model; we don't override.
public class EagleModel extends EntityModel<EagleRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MOD_ID, "eagle"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart neck;
    private final ModelPart tail;
    private final ModelPart leftWingUpper;
    private final ModelPart leftWingLower;
    private final ModelPart rightWingUpper;
    private final ModelPart rightWingLower;
    private final ModelPart leftLeg;
    private final ModelPart leftTalon;
    private final ModelPart rightLeg;
    private final ModelPart rightTalon;

    public EagleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.beak = head.getChild("beak");
        this.neck = body.getChild("neck");
        this.tail = body.getChild("tail");

        ModelPart leftWing = body.getChild("left_wing");
        this.leftWingUpper = leftWing;
        this.leftWingLower = leftWing.getChild("left_wing_lower");

        ModelPart rightWing = body.getChild("right_wing");
        this.rightWingUpper = rightWing;
        this.rightWingLower = rightWing.getChild("right_wing_lower");

        this.leftLeg = body.getChild("left_leg");
        this.leftTalon = leftLeg.getChild("left_talon");

        this.rightLeg = body.getChild("right_leg");
        this.rightTalon = rightLeg.getChild("right_talon");
    }

    /** Single-call cube layout — registered with EntityModelSet at client
     *  init. Same geometry as the 1.21.1 sheet; see the original commit
     *  history for per-part documentation. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4f, -3f, -5f, 8, 6, 10),
                PartPose.offset(0f, 14f, 0f));

        body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(36, 0)
                        .addBox(-1.5f, -4f, -1.5f, 3, 4, 3),
                PartPose.offsetAndRotation(0f, -3f, -4f, -0.2f, 0f, 0f));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2f, -4f, -2f, 4, 4, 4),
                PartPose.offset(0f, -7f, -4f));
        head.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-1f, -1f, -3f, 2, 2, 3),
                PartPose.offsetAndRotation(0f, -2f, -2f, -0.15f, 0f, 0f));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-3f, 0f, 0f, 6, 1, 6),
                PartPose.offsetAndRotation(0f, -1f, 5f, 0.3f, 0f, 0f));

        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-7f, -1f, -4f, 7, 2, 8),
                PartPose.offsetAndRotation(-4f, -2f, -2f, 0f, 0f, 1.0f));
        leftWing.addOrReplaceChild("left_wing_lower",
                CubeListBuilder.create().texOffs(32, 32)
                        .addBox(-8f, 0f, -3f, 8, 1, 6),
                PartPose.offsetAndRotation(-7f, 0f, 0f, 0f, 0f, 0.3f));

        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(0f, -1f, -4f, 7, 2, 8),
                PartPose.offsetAndRotation(4f, -2f, -2f, 0f, 0f, -1.0f));
        rightWing.addOrReplaceChild("right_wing_lower",
                CubeListBuilder.create().texOffs(32, 40)
                        .addBox(0f, 0f, -3f, 8, 1, 6),
                PartPose.offsetAndRotation(7f, 0f, 0f, 0f, 0f, -0.3f));

        PartDefinition leftLeg = body.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(20, 16)
                        .addBox(-1f, 0f, -1f, 2, 5, 2),
                PartPose.offset(-2f, 3f, 0f));
        leftLeg.addOrReplaceChild("left_talon",
                CubeListBuilder.create().texOffs(28, 16)
                        .addBox(-2f, 0f, -1.5f, 4, 1, 3),
                PartPose.offsetAndRotation(0f, 5f, 0f, -0.4f, 0f, 0f));

        PartDefinition rightLeg = body.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(20, 24)
                        .addBox(-1f, 0f, -1f, 2, 5, 2),
                PartPose.offset(2f, 3f, 0f));
        rightLeg.addOrReplaceChild("right_talon",
                CubeListBuilder.create().texOffs(28, 24)
                        .addBox(-2f, 0f, -1.5f, 4, 1, 3),
                PartPose.offsetAndRotation(0f, 5f, 0f, -0.4f, 0f, 0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EagleRenderState state) {
        // resetPose() (from Model) restores every part to its PartPose
        // default, so we don't accumulate rotations across frames.
        super.setupAnim(state);

        if (state.flying) {
            animateFlight(state.ageInTicks, state.horizontalSpeed);
        } else {
            animateIdle(state.ageInTicks, state.walkAnimationSpeed, state.walkAnimationPos);
        }

        head.yRot = state.yRot * ((float) Math.PI / 180f);
        head.xRot = state.xRot * ((float) Math.PI / 180f);
    }

    private void animateFlight(float ageInTicks, float speed) {
        float flapAmplitude = 0.6f + speed * 0.4f;
        float flapCycle = (float) Math.sin(ageInTicks * 0.25f) * flapAmplitude;

        leftWingUpper.zRot = -flapCycle - 0.2f;
        rightWingUpper.zRot = flapCycle + 0.2f;

        float lowerExtend = Math.max(0f, -flapCycle) * 0.3f;
        leftWingLower.zRot = -(0.3f + lowerExtend);
        rightWingLower.zRot = 0.3f + lowerExtend;

        tail.xRot = 0.1f + (float) Math.sin(ageInTicks * 0.25f) * 0.05f;

        leftLeg.xRot = -0.6f;
        rightLeg.xRot = -0.6f;
    }

    private void animateIdle(float ageInTicks, float speed, float walkPos) {
        neck.xRot = -0.05f + (float) Math.sin(ageInTicks * 0.05f) * 0.04f;
        head.yRot = (float) Math.sin(ageInTicks * 0.03f) * 0.15f;

        leftWingUpper.zRot = -1.4f;
        rightWingUpper.zRot = 1.4f;
        leftWingLower.zRot = -0.3f;
        rightWingLower.zRot = 0.3f;

        tail.xRot = 0.15f;
        leftLeg.xRot = 0f;
        rightLeg.xRot = 0f;
        leftTalon.xRot = 0f;
        rightTalon.xRot = 0f;

        if (speed > 0.01f) {
            leftLeg.xRot = (float) Math.sin(walkPos * 0.6662f) * 0.5f * speed;
            rightLeg.xRot = -(float) Math.sin(walkPos * 0.6662f + Math.PI) * 0.5f * speed;
        }
    }
}
