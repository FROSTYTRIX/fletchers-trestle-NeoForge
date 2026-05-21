package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;

import static net.frostytrix.fletcherstrestle.FletcherTrestle.MOD_ID;

/**
 * Eagle Model — texture sheet is 64x64 px.
 *
 * Pivot point convention used throughout:
 *   X = left/right  (positive = player's right when looking at entity front)
 *   Y = up/down     (positive = DOWN in Minecraft model space)
 *   Z = forward/back (positive = toward the viewer / entity's back)
 *
 * All dimensions in 1/16th of a block (i.e. "model units").
 * The eagle is modelled as if standing upright facing you.
 */
public class EagleModel extends EntityModel<EagleEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MOD_ID, "eagle"), "main");

    // Part references (stored so setupAnim() can rotate them each frame)
    private final ModelPart root;
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
        this.root      = root;
        this.body      = root.getChild("body");
        this.head      = body.getChild("head");
        this.beak      = head.getChild("beak");
        this.neck      = body.getChild("neck");
        this.tail      = body.getChild("tail");

        ModelPart leftWing  = body.getChild("left_wing");
        this.leftWingUpper  = leftWing;
        this.leftWingLower  = leftWing.getChild("left_wing_lower");

        ModelPart rightWing = body.getChild("right_wing");
        this.rightWingUpper = rightWing;
        this.rightWingLower = rightWing.getChild("right_wing_lower");

        this.leftLeg   = body.getChild("left_leg");
        this.leftTalon = leftLeg.getChild("left_talon");

        this.rightLeg  = body.getChild("right_leg");
        this.rightTalon = rightLeg.getChild("right_talon");
    }

    /**
     * createBodyLayer() — defines ALL cube geometry and UV mappings.
     * Called once at startup; the result is registered with EntityModelSet.
     *
     * Texture sheet: 64 x 64 pixels.
     * UV coordinates below are [u, v] = top-left corner of the face strip on the sheet.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---------------------------------------------------------------
        // BODY  — the core trunk, pivot at the bird's center of mass
        //   Size:     8w x 6h x 10d   (wide, shallow, long like a bird torso)
        //   Pivot:    (0, 14, 0)       (14 units down from model origin = roughly mid-air standing)
        //   UV:       (0, 0)
        // ---------------------------------------------------------------
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4f, -3f, -5f,   8, 6, 10),
                PartPose.offset(0f, 14f, 0f));

        // ---------------------------------------------------------------
        // NECK — short connector between body and head
        //   Size:     3w x 4h x 3d
        //   Pivot:    (0, -3, -4)      (attached at front-top of body)
        //   UV:       (36, 0)
        //   Default rotation: tilted slightly forward (-0.2 rad on X)
        // ---------------------------------------------------------------
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(36, 0)
                        .addBox(-1.5f, -4f, -1.5f,   3, 4, 3),
                PartPose.offsetAndRotation(0f, -3f, -4f,   -0.2f, 0f, 0f));

        // ---------------------------------------------------------------
        // HEAD — attached to top of neck
        //   Size:     4w x 4h x 4d
        //   Pivot:    (0, -4, 0)       (top of neck)
        //   UV:       (0, 16)
        // ---------------------------------------------------------------
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2f, -4f, -2f,   4, 4, 4),
                // Pivot is relative to BODY origin, matching the neck top:
                // body pivot (0,14,0) + neck offset (0,-3,-4) + neck height (0,-4,0) = (0,-7,-4)
                PartPose.offset(0f, -7f, -4f));

        // ---------------------------------------------------------------
        // BEAK — child of head, hooked downward tip
        //   Size:     2w x 2h x 3d    (extends forward from head front)
        //   Pivot:    (0, -2, -2)      (front-mid of head)
        //   UV:       (16, 16)
        //   Default rotation: slight downward hook (-0.15 rad on X)
        // ---------------------------------------------------------------
        head.addOrReplaceChild("beak",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-1f, -1f, -3f,   2, 2, 3),
                PartPose.offsetAndRotation(0f, -2f, -2f,   -0.15f, 0f, 0f));

        // ---------------------------------------------------------------
        // TAIL — fans out from the back of the body
        //   Size:     6w x 1h x 6d    (flat fan)
        //   Pivot:    (0, -1, 5)       (rear top of body)
        //   UV:       (0, 32)
        //   Default rotation: tilted upward slightly (0.3 rad on X)
        // ---------------------------------------------------------------
        body.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-3f, 0f, 0f,   6, 1, 6),
                PartPose.offsetAndRotation(0f, -1f, 5f,   0.3f, 0f, 0f));

        // ---------------------------------------------------------------
        // LEFT WING (upper segment) — pivots at the shoulder (left side of body)
        //   Size:     7w x 2h x 8d
        //   Pivot:    (-4, -2, -2)     (left shoulder of body)
        //   UV:       (0, 40)
        //   Default rotation: wings folded against body (Z = +1.0 rad, folds down-inward)
        // ---------------------------------------------------------------
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-7f, -1f, -4f,   7, 2, 8),
                PartPose.offsetAndRotation(-4f, -2f, -2f,   0f, 0f, 1.0f));

        // LEFT WING LOWER (primary feathers) — child of upper wing
        //   Size:     8w x 1h x 6d    (slightly wider, thinner = primary fan)
        //   Pivot:    (-7, 0, 0)       (tip of upper segment)
        //   UV:       (32, 32)
        //   Default rotation: folded further (Z = +0.3 rad relative to upper)
        leftWing.addOrReplaceChild("left_wing_lower",
                CubeListBuilder.create()
                        .texOffs(32, 32)
                        .addBox(-8f, 0f, -3f,   8, 1, 6),
                PartPose.offsetAndRotation(-7f, 0f, 0f,   0f, 0f, 0.3f));

        // ---------------------------------------------------------------
        // RIGHT WING (upper segment) — mirror of left wing
        //   Pivot:    (4, -2, -2)      (right shoulder)
        //   UV:       (0, 48)
        //   Default rotation: Z = -1.0 rad (folds down-inward, opposite side)
        // ---------------------------------------------------------------
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 48)
                        .addBox(0f, -1f, -4f,   7, 2, 8),
                PartPose.offsetAndRotation(4f, -2f, -2f,   0f, 0f, -1.0f));

        // RIGHT WING LOWER (primary feathers)
        //   Pivot:    (7, 0, 0)
        //   UV:       (32, 40)
        //   Default rotation: Z = -0.3 rad
        rightWing.addOrReplaceChild("right_wing_lower",
                CubeListBuilder.create()
                        .texOffs(32, 40)
                        .addBox(0f, 0f, -3f,   8, 1, 6),
                PartPose.offsetAndRotation(7f, 0f, 0f,   0f, 0f, -0.3f));

        // ---------------------------------------------------------------
        // LEFT LEG
        //   Size:     2w x 5h x 2d
        //   Pivot:    (-2, 3, 0)       (left underside of body)
        //   UV:       (20, 16)
        // ---------------------------------------------------------------
        PartDefinition leftLeg = body.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(20, 16)
                        .addBox(-1f, 0f, -1f,   2, 5, 2),
                PartPose.offset(-2f, 3f, 0f));

        // LEFT TALON — 3 small claws, modelled as one flat cube for now
        //   Size:     4w x 1h x 3d
        //   Pivot:    (0, 5, 0)        (bottom of leg)
        //   UV:       (28, 16)
        //   Default rotation: -0.4 rad on X (claws grip forward-down)
        leftLeg.addOrReplaceChild("left_talon",
                CubeListBuilder.create()
                        .texOffs(28, 16)
                        .addBox(-2f, 0f, -1.5f,   4, 1, 3),
                PartPose.offsetAndRotation(0f, 5f, 0f,   -0.4f, 0f, 0f));

        // ---------------------------------------------------------------
        // RIGHT LEG (mirror of left)
        //   Pivot:    (2, 3, 0)
        //   UV:       (20, 24)
        // ---------------------------------------------------------------
        PartDefinition rightLeg = body.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(20, 24)
                        .addBox(-1f, 0f, -1f,   2, 5, 2),
                PartPose.offset(2f, 3f, 0f));

        // RIGHT TALON
        //   UV:       (28, 24)
        rightLeg.addOrReplaceChild("right_talon",
                CubeListBuilder.create()
                        .texOffs(28, 24)
                        .addBox(-2f, 0f, -1.5f,   4, 1, 3),
                PartPose.offsetAndRotation(0f, 5f, 0f,   -0.4f, 0f, 0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ---------------------------------------------------------------
    // setupAnim() — called every frame by the renderer.
    // animationPosition = walk distance accumulator (used for leg/wing swing)
    // animationSpeed    = how fast the entity is moving (0..1)
    // bobbing           = vertical bob offset
    // ageInTicks        = entity age in ticks + partialTick (for idle sway)
    // ---------------------------------------------------------------
    @Override
    public void setupAnim(EagleEntity entity, float animationPosition,
                          float animationSpeed, float ageInTicks,
                          float netHeadYaw, float headPitch) {

        // Reset all parts to their default PartPose rotation every frame
        // so animations don't stack across frames.
        resetParts();

        boolean isFlying = entity.isFlying(); // you'll expose this from EagleEntity

        if (isFlying) {
            animateFlight(ageInTicks, animationSpeed);
        } else {
            animateIdle(ageInTicks, animationSpeed, animationPosition);
        }

        // Head always tracks the look direction (yaw/pitch from EntityRenderer)
        head.yRot = netHeadYaw  * ((float) Math.PI / 180f);
        head.xRot = headPitch   * ((float) Math.PI / 180f);
        // Keep beak attached — no extra rotation needed, it's a child of head
    }

    private void animateFlight(float ageInTicks, float speed) {
        // Wing flap: sinusoidal on Z axis for upper segments
        // Amplitude increases with speed (faster flight = bigger flap)
        float flapAmplitude = 0.6f + speed * 0.4f;
        float flapCycle     = (float) Math.sin(ageInTicks * 0.25f) * flapAmplitude;

        // Upper wings rotate on Z (left = negative to lift, right = positive)
        leftWingUpper.zRot  = -flapCycle - 0.2f;   // -0.2 = slight natural droop offset
        rightWingUpper.zRot =  flapCycle + 0.2f;

        // Lower wing (primary feathers) extends outward slightly on downstroke
        float lowerExtend = Math.max(0f, -flapCycle) * 0.3f;
        leftWingLower.zRot  = -(0.3f + lowerExtend);
        rightWingLower.zRot =   0.3f + lowerExtend;

        // Tail fans out during flight, dips slightly on downstroke
        tail.xRot = 0.1f + (float) Math.sin(ageInTicks * 0.25f) * 0.05f;

        // Body tilts forward during flight
        // (this is done in the renderer via PoseStack, not here — placeholder comment)

        // Legs tuck back during flight
        leftLeg.xRot  = -0.6f;
        rightLeg.xRot = -0.6f;
    }

    private void animateIdle(float ageInTicks, float speed, float walkPos) {
        // Perched bird posture: neck pulled in (head closer to body), slight
        // breathing motion. The default neck xRot is -0.2 (forward tilt);
        // we increase to -0.05 so the head sits more upright over the body.
        neck.xRot = -0.05f + (float) Math.sin(ageInTicks * 0.05f) * 0.04f;

        // Occasional slow head-turn left/right.
        head.yRot = (float) Math.sin(ageInTicks * 0.03f) * 0.15f;

        // Wings folded DOWN along the body. Sign convention: model Y+ is
        // visually DOWN, so positive zRot on the left wing rotates its tip
        // UP (above horizontal). To drape the wing along the bird's side
        // we need NEGATIVE zRot on the left and POSITIVE on the right.
        leftWingUpper.zRot  = -1.4f;
        rightWingUpper.zRot =  1.4f;
        // The lower wing (primary feathers) tucks inward slightly. Same
        // sign rule applies — small inward bend.
        leftWingLower.zRot  = -0.3f;
        rightWingLower.zRot =  0.3f;

        // Tail relaxed downward instead of fanned out.
        tail.xRot = 0.15f;

        // Legs straight under the body — not angled like in flight.
        leftLeg.xRot  = 0f;
        rightLeg.xRot = 0f;

        // Talons flat on the ground (default pose has them tilted forward
        // for the in-flight grip-forward look).
        leftTalon.xRot  = 0f;
        rightTalon.xRot = 0f;

        if (speed > 0.01f) {
            // Walking waddle.
            leftLeg.xRot  =  (float) Math.sin(walkPos * 0.6662f)            * 0.5f * speed;
            rightLeg.xRot = -(float) Math.sin(walkPos * 0.6662f + Math.PI)  * 0.5f * speed;
        }
    }

    /**
     * Resets all animated parts to their PartPose defaults.
     * Must be called at the start of every setupAnim() to prevent
     * rotation accumulation across frames (the same bug we fixed in ModularArrowRenderer!).
     */
    private void resetParts() {
        // Body stays fixed — its PartPose handles position
        neck.xRot = -0.2f; neck.yRot = 0f; neck.zRot = 0f;
        head.xRot = 0f;    head.yRot = 0f; head.zRot = 0f;
        beak.xRot = -0.15f;

        tail.xRot = 0.3f;  tail.yRot = 0f; tail.zRot = 0f;

        leftWingUpper.xRot = 0f; leftWingUpper.yRot = 0f; leftWingUpper.zRot  =  1.0f;
        leftWingLower.xRot = 0f; leftWingLower.yRot = 0f; leftWingLower.zRot  =  0.3f;

        rightWingUpper.xRot = 0f; rightWingUpper.yRot = 0f; rightWingUpper.zRot = -1.0f;
        rightWingLower.xRot = 0f; rightWingLower.yRot = 0f; rightWingLower.zRot = -0.3f;

        leftLeg.xRot  = 0f; leftLeg.yRot  = 0f; leftLeg.zRot  = 0f;
        rightLeg.xRot = 0f; rightLeg.yRot = 0f; rightLeg.zRot = 0f;

        leftTalon.xRot  = -0.4f;
        rightTalon.xRot = -0.4f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}