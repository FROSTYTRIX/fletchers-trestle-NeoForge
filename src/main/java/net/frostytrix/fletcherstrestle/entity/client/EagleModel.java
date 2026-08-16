package net.frostytrix.fletcherstrestle.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.frostytrix.fletcherstrestle.entity.custom.EagleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import static net.frostytrix.fletcherstrestle.FletcherTrestle.MOD_ID;

/**
 * Eagle Model: commissioned Blockbench rig (64x64 texture sheet).
 * <p>
 * Geometry is the artist's export ({@link #createBodyLayer()}); the part
 * hierarchy is:
 * <pre>
 *   commeagle (root group)
 *     body
 *       leftleg → leftfoot
 *       rightleg → rightfoot
 *       tailbase → tailfeathers
 *       neck → head
 *       leftwingbase → leftwingfeathers
 *       rightwingbase → rightwingfeathers
 * </pre>
 * The animatable group parts all default to zero rotation (the resting pose
 * the artist modelled lives inside the baked {@code _r1} sub-cubes), so
 * {@link #resetParts()} just zeroes them each frame.
 * <p>
 * NOTE: wing-flap axis/amplitude and leg-tuck are a first pass: they need
 * tuning from an in-game screenshot (the usual build→run→tune loop).
 */
public class EagleModel extends EntityModel<EagleEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MOD_ID, "eagle"), "main");

    // Top render group (everything hangs off this).
    private final ModelPart commeagle;
    // Animatable group parts.
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart tailbase;
    private final ModelPart tailfeathers;
    private final ModelPart leftWingBase;
    private final ModelPart leftWingFeathers;
    private final ModelPart rightWingBase;
    private final ModelPart rightWingFeathers;
    private final ModelPart leftLeg;
    private final ModelPart leftFoot;
    private final ModelPart rightLeg;
    private final ModelPart rightFoot;

    public EagleModel(ModelPart root) {
        this.commeagle = root.getChild("commeagle");
        this.body = commeagle.getChild("body");

        this.leftLeg = body.getChild("leftleg");
        this.leftFoot = leftLeg.getChild("leftfoot");
        this.rightLeg = body.getChild("rightleg");
        this.rightFoot = rightLeg.getChild("rightfoot");

        this.tailbase = body.getChild("tailbase");
        this.tailfeathers = tailbase.getChild("tailfeathers");

        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");

        this.leftWingBase = body.getChild("leftwingbase");
        this.leftWingFeathers = leftWingBase.getChild("leftwingfeathers");
        this.rightWingBase = body.getChild("rightwingbase");
        this.rightWingFeathers = rightWingBase.getChild("rightwingfeathers");
    }

    /**
     * createBodyLayer(): geometry exported from Blockbench (Mojang mappings).
     * Texture sheet: 64 x 64.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition commeagle = partdefinition.addOrReplaceChild("commeagle", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = commeagle.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

        body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.0F, -7.0F, 8.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition leftleg = body.addOrReplaceChild("leftleg", CubeListBuilder.create(), PartPose.offset(2.5F, 5.0F, -1.0F));

        leftleg.addOrReplaceChild("leftleg_r1", CubeListBuilder.create().texOffs(44, 32).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, 0.3491F, 0.0F, 0.0F));

        leftleg.addOrReplaceChild("leftfoot", CubeListBuilder.create().texOffs(32, 48).addBox(-0.5F, 1.0F, 0.25F, 1.0F, 5.0F, 0.0F, new CubeDeformation(0.01F))
        .texOffs(44, 28).addBox(-1.5F, 6.0F, -2.5F, 3.0F, 0.0F, 4.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 1.0F, 1.0F));

        PartDefinition rightleg = body.addOrReplaceChild("rightleg", CubeListBuilder.create(), PartPose.offset(-2.5F, 4.25F, -0.5F));

        rightleg.addOrReplaceChild("rightleg_r1", CubeListBuilder.create().texOffs(44, 37).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.75F, 0.5F, 0.3491F, 0.0F, 0.0F));

        rightleg.addOrReplaceChild("rightfoot", CubeListBuilder.create().texOffs(34, 48).addBox(-0.5F, 0.0F, 0.25F, 1.0F, 5.0F, 0.0F, new CubeDeformation(0.01F))
        .texOffs(32, 44).addBox(-1.5F, 5.0F, -2.5F, 3.0F, 0.0F, 4.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 2.75F, 0.5F));

        PartDefinition tailbase = body.addOrReplaceChild("tailbase", CubeListBuilder.create().texOffs(38, 11).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

        PartDefinition tailfeathers = tailbase.addOrReplaceChild("tailfeathers", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 3.0F));

        tailfeathers.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, 0.0F, -1.0F, 7.0F, 0.0F, 10.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.5F, 0.25F, 0.5F, -0.3927F, 0.0F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -6.0F));

        neck.addOrReplaceChild("neck_r1", CubeListBuilder.create().texOffs(38, 0).addBox(-3.0F, -2.0F, -2.0F, 5.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -2.0F, -1.0F, -0.1309F, 0.0F, 0.0F));

        neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(34, 18).addBox(-2.5F, -2.9829F, -3.739F, 4.0F, 4.0F, 6.0F, new CubeDeformation(0.0F))
        .texOffs(46, 42).addBox(-1.5F, -1.9829F, -5.739F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(46, 46).addBox(-1.5F, -1.9829F, -6.739F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -4.0F, -1.0F, 0.1309F, 0.0F, 0.0F));

        PartDefinition leftwingbase = body.addOrReplaceChild("leftwingbase", CubeListBuilder.create(), PartPose.offset(3.5F, -2.0F, -4.0F));

        leftwingbase.addOrReplaceChild("lwbase_r1", CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, -6.0F, -1.0F, 1.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, 4.5F, -2.5F, -0.3927F, 0.0F, 0.0F));

        leftwingbase.addOrReplaceChild("leftwingfeathers", CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F, -2.0F, -1.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.01F)), PartPose.offset(1.5F, 4.5F, 6.5F));

        PartDefinition rightwingbase = body.addOrReplaceChild("rightwingbase", CubeListBuilder.create(), PartPose.offset(-3.5F, -2.0F, -4.0F));

        rightwingbase.addOrReplaceChild("rwbase_r1", CubeListBuilder.create().texOffs(22, 28).addBox(0.0F, -6.0F, -1.0F, 1.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 4.5F, -2.5F, -0.3927F, 0.0F, 0.0F));

        rightwingbase.addOrReplaceChild("rightwingfeathers", CubeListBuilder.create().texOffs(16, 44).addBox(0.5F, -2.0F, -1.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.01F)), PartPose.offset(-1.5F, 4.5F, 6.5F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    // ---------------------------------------------------------------
    // setupAnim(): called every frame by the renderer.
    // ---------------------------------------------------------------
    @Override
    public void setupAnim(EagleEntity entity, float animationPosition,
                          float animationSpeed, float ageInTicks,
                          float netHeadYaw, float headPitch) {

        resetParts();

        // Slight forward lean, ONLY in the guidebook's static render. Patchouli
        // creates a display entity and never ticks it, so tickCount stays 0;
        // in-world eagles tick past 0 immediately and stand normally.
        if (entity.tickCount == 0) {
            body.xRot = 0.12f;
        }

        if (entity.isFlying()) {
            // Flap phase & amplitude both come from the entity (accumulated /
            // smoothed there): amplitude scales glide↔flap with speed, and the
            // accumulated phase lets the frequency scale with speed too without
            // the jitter that frequency-modulating a shared sine would cause.
            animateFlight(entity.getFlapPhase(), entity.getFlapAmount());
        } else {
            animateIdle(ageInTicks, animationSpeed, animationPosition);
        }

        // Head tracks the look direction. Head is a child of neck; the small
        // baked head pitch (0.1309) is its rest pose, so we add on top of 0.
        head.yRot = netHeadYaw * ((float) Math.PI / 180f);
        head.xRot = headPitch * ((float) Math.PI / 180f);
    }

    private void animateFlight(float flapPhase, float horizSpeed) {
        // The wings are modelled FOLDED (hanging down along the body at rest,
        // zRot 0). Flight has two parts: (1) a static "open" angle that swings
        // each folded wing out to horizontal at the shoulder, and (2) a flap
        // oscillating around that open pose. The flap stays smaller than the
        // open angle so the wing never rotates back into the body.
        // Left wing sits on +X (opens with negative zRot), right on -X (positive).
        // ~83° open: wings sit nearly horizontal (90° = 1.5708) so a gliding
        // eagle reads as wings-out, not half-folded.
        float openAngle = 1.45f;

        // move: 0 when hovering/soaring, →1 when powering forward. Blends a
        // near-still eagle into a wings-out GLIDE (tiny waver) and a moving one
        // into a full, faster flap, so a stationary airborne eagle looks like
        // it's soaring, not frozen mid-air.
        // Amplitude scales glide↔flap with speed; the phase is accumulated on
        // the entity (so its frequency can scale with speed jitter-free).
        float move = Math.min(1f, horizSpeed / 0.25f);
        float flapAmp = 0.06f + move * 0.4f;
        float flap = (float) Math.sin(flapPhase) * flapAmp;

        leftWingBase.zRot = -openAngle + flap;
        rightWingBase.zRot = openAngle - flap;

        // Primary feathers trail the flap a little (they're children of the
        // wing base, so this bends the wingtip relative to the opened wing).
        leftWingFeathers.zRot = flap * 0.4f;
        rightWingFeathers.zRot = -flap * 0.4f;

        // Tail fans / dips gently with the flap.
        tailbase.xRot = (float) Math.sin(flapPhase) * 0.06f;

        // Legs tuck BACK under the tail in flight (positive xRot swings the
        // downward-hanging legs rearward).
        leftLeg.xRot = 0.8f;
        rightLeg.xRot = 0.8f;
    }

    private void animateIdle(float ageInTicks, float speed, float walkPos) {
        // Gentle breathing/sway on the neck.
        neck.xRot = (float) Math.sin(ageInTicks * 0.05f) * 0.04f;
        // Occasional slow head turn.
        head.yRot = (float) Math.sin(ageInTicks * 0.03f) * 0.15f;

        // Tail relaxed.
        tailbase.xRot = 0.05f;

        if (speed > 0.01f) {
            // Walking waddle.
            leftLeg.xRot = (float) Math.sin(walkPos * 0.6662f) * 0.5f * speed;
            rightLeg.xRot = -(float) Math.sin(walkPos * 0.6662f + Math.PI) * 0.5f * speed;
        }
    }

    /**
     * Zeroes every animated group part each frame so rotations don't stack
     * (same accumulation bug we fixed in ModularArrowRenderer). The artist's
     * resting pose lives in the baked _r1 cubes, so zero IS the rest pose.
     */
    private void resetParts() {
        neck.xRot = neck.yRot = neck.zRot = 0f;
        head.xRot = head.yRot = head.zRot = 0f;
        tailbase.xRot = tailbase.yRot = tailbase.zRot = 0f;
        tailfeathers.xRot = tailfeathers.yRot = tailfeathers.zRot = 0f;
        leftWingBase.xRot = leftWingBase.yRot = leftWingBase.zRot = 0f;
        leftWingFeathers.xRot = leftWingFeathers.yRot = leftWingFeathers.zRot = 0f;
        rightWingBase.xRot = rightWingBase.yRot = rightWingBase.zRot = 0f;
        rightWingFeathers.xRot = rightWingFeathers.yRot = rightWingFeathers.zRot = 0f;
        leftLeg.xRot = leftLeg.yRot = leftLeg.zRot = 0f;
        leftFoot.xRot = leftFoot.yRot = leftFoot.zRot = 0f;
        rightLeg.xRot = rightLeg.yRot = rightLeg.zRot = 0f;
        rightFoot.xRot = rightFoot.yRot = rightFoot.zRot = 0f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        commeagle.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
