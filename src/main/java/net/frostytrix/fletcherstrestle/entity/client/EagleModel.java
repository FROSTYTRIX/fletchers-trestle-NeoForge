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
import net.minecraft.util.Mth;

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
 * Every frame starts from the modelled rest pose ({@link #resetParts()}) and
 * lays the animation on top. Flight reads the eagle's interpolated flap phase,
 * pitch, bank and landing flare, so it moves at the frame rate, not in
 * twenty steps a second.
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
    // Animation
    // ---------------------------------------------------------------

    /** Share of a wingbeat spent on the downstroke: a quick, powerful down, a slower recovery. */
    private static final float DOWNSTROKE = 0.4f;
    /** How far each folded wing swings out at the shoulder: just short of level, so the tips droop. */
    private static final float WINGS_OPEN = 1.45f;
    /** Gliding, the wings lift a little past level into a shallow V, as soaring raptors hold them. */
    private static final float WINGS_GLIDE = 1.62f;
    /** Landing flare: wings raised high and swept forward to brake. */
    private static final float WINGS_FLARE = 1.95f;

    /** Ticks between the sharp head turns of a bird looking around. */
    private static final float GLANCE_TICKS = 45f;
    private static final float RUFFLE_EVERY = 260f;
    private static final float RUFFLE_TICKS = 16f;
    private static final float TAIL_FLICK_EVERY = 90f;
    private static final float TAIL_FLICK_TICKS = 6f;

    @Override
    public void setupAnim(EagleEntity entity, float walkPos, float walkSpeed,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        resetParts();
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0f, 1f);

        // Slight forward lean, ONLY in the guidebook's static render. Patchouli
        // creates a display entity and never ticks it, so tickCount stays 0;
        // in-world eagles tick past 0 immediately and stand normally.
        if (entity.tickCount == 0) {
            body.xRot = 0.12f;
        }

        float headYaw = netHeadYaw * Mth.DEG_TO_RAD;
        float headTilt = headPitch * Mth.DEG_TO_RAD;

        if (entity.isFlying()) {
            animateFlight(entity, partialTick);
            // The head stays nearer level than the body: it watches the horizon in a dive.
            headTilt -= entity.getFlightPitch(partialTick) * Mth.DEG_TO_RAD * 0.6f;
        } else if (entity.isRoosting()) {
            animateRoost(ageInTicks);
            // Asleep: the head stays tucked, whatever is going on around it.
            return;
        } else {
            float walk = Math.min(1f, walkSpeed * 2.5f);
            animateGround(entity, ageInTicks, walkPos, walk);
            int seed = entity.getId() * 7919;
            headYaw += glance(seed, ageInTicks, 1.0f) * (1f - walk);
            headTilt += glance(seed * 31 + 17, ageInTicks, 0.45f) * (1f - walk);
        }

        // On top of the head's own rest tilt from the model.
        head.yRot += headYaw;
        head.xRot += headTilt;
    }

    private void animateFlight(EagleEntity entity, float partialTick) {
        float effort = Math.min(1f, entity.getFlapAmount() / 0.25f);
        float flare = entity.getFlare(partialTick);
        float bank = entity.getBank(partialTick) * Mth.DEG_TO_RAD;

        float t = fraction(entity.getFlapPhase(partialTick) / Mth.TWO_PI);
        float stroke = stroke(t);
        float upstroke = t < DOWNSTROKE ? 0f : Mth.sin(Mth.PI * (t - DOWNSTROKE) / (1f - DOWNSTROKE));

        // The wings are modelled FOLDED along the body; the shoulder swings them
        // out (left wing on +X opens with negative zRot, right with positive), and
        // the beat oscillates around that. Stroke +1 is wings down, -1 wings up.
        float open = Mth.lerp(flare, Mth.lerp(effort, WINGS_GLIDE, WINGS_OPEN), WINGS_FLARE);
        float amplitude = 0.08f + effort * 0.42f;
        leftWingBase.zRot = -open + amplitude * stroke;
        rightWingBase.zRot = open - amplitude * stroke;

        // Swept back on the upstroke to cut drag, forward in the flare.
        float sweep = upstroke * 0.35f * effort - flare * 0.5f;
        leftWingBase.xRot = sweep;
        rightWingBase.xRot = sweep;

        // The trailing feathers lag the beat, and droop as the wing recovers.
        float lag = stroke(fraction(t - 0.1f)) - stroke;
        float flex = amplitude * 0.6f * lag + upstroke * 0.3f * effort;
        leftWingFeathers.zRot = flex;
        rightWingFeathers.zRot = -flex;

        // Each downstroke lifts the body a touch, just after the wings come down.
        commeagle.y -= stroke(fraction(t - 0.15f)) * 0.7f * effort;

        // Tail: bobs with the beat, steers into turns, fans down to brake.
        tailbase.xRot = stroke * 0.05f * effort - flare * 0.45f;
        tailbase.yRot = bank * 0.4f;
        tailfeathers.zRot = -bank * 0.3f;

        // Legs tucked back under the tail, talons curled; thrown forward to land.
        float leg = Mth.lerp(flare, 1.1f, -0.5f);
        float foot = Mth.lerp(flare, 0.6f, -0.3f);
        leftLeg.xRot = leg;
        rightLeg.xRot = leg;
        leftFoot.xRot = foot;
        rightFoot.xRot = foot;
    }

    private void animateGround(EagleEntity entity, float ageInTicks, float walkPos, float walk) {
        int seed = entity.getId() * 7919;

        // Breathing.
        commeagle.y += Mth.sin(ageInTicks * 0.10f) * 0.15f;
        neck.xRot = Mth.sin(ageInTicks * 0.05f) * 0.04f;

        if (entity.isInjured()) {
            // Hurt: one wing hangs loose off the body and it favours a leg.
            leftWingBase.zRot = -0.32f;
            leftWingFeathers.zRot = 0.25f;
            body.zRot = 0.08f;
            neck.xRot += 0.12f;
        }

        if (walk > 0.01f) {
            // A raptor's rolling walk: legs alternate, the body tips over the
            // planted foot and bobs, the tail swings to balance.
            float step = walkPos * 1.6f;
            float limp = entity.isInjured() ? 0.4f : 1.0f;
            leftLeg.xRot = Mth.sin(step) * 0.7f * walk;
            rightLeg.xRot = Mth.sin(step + Mth.PI) * 0.7f * walk * limp;
            body.zRot = Mth.sin(step) * 0.10f * walk;
            commeagle.y -= Math.abs(Mth.cos(step)) * 0.6f * walk;
            neck.xRot += Mth.sin(step * 2f) * 0.10f * walk;
            tailbase.yRot = -Mth.sin(step) * 0.12f * walk;
        }

        float still = 1f - walk;

        // Now and then a shake to settle the feathers: wings lift off the body
        // and flutter, the body and tail shiver.
        float r = (ageInTicks + Math.floorMod(seed, (int) RUFFLE_EVERY)) % RUFFLE_EVERY;
        if (r < RUFFLE_TICKS) {
            float envelope = Mth.sin(Mth.PI * r / RUFFLE_TICKS) * still;
            float flutter = Mth.sin(r * 2.8f);
            float lift = (0.30f + 0.10f * flutter) * envelope;
            leftWingBase.zRot = -lift;
            rightWingBase.zRot = lift;
            leftWingFeathers.zRot = 0.2f * flutter * envelope;
            rightWingFeathers.zRot = -0.2f * flutter * envelope;
            body.zRot += 0.05f * flutter * envelope;
            tailbase.yRot += 0.15f * flutter * envelope;
            neck.xRot -= 0.10f * envelope;
        }

        // A quick tail flick.
        float f = (ageInTicks + Math.floorMod(seed * 3, (int) TAIL_FLICK_EVERY)) % TAIL_FLICK_EVERY;
        if (f < TAIL_FLICK_TICKS) {
            tailbase.xRot += 0.30f * Mth.sin(Mth.PI * f / TAIL_FLICK_TICKS) * still;
        }
    }

    /**
     * Asleep for the night: settled low on its feet, the head turned back over
     * the shoulder and tucked down into the back feathers, breathing slowly.
     */
    private void animateRoost(float ageInTicks) {
        commeagle.y += 1.2f + Mth.sin(ageInTicks * 0.06f) * 0.12f;
        neck.xRot = 0.35f;
        head.yRot += 2.5f;
        head.xRot += 0.55f;
        head.zRot = 0.2f;
        tailbase.xRot = -0.05f;
        leftWingBase.zRot = -0.05f;
        rightWingBase.zRot = 0.05f;
    }

    /**
     * Wing position over one beat, from -1 (up) to +1 (down): the downstroke takes
     * {@link #DOWNSTROKE} of the beat, the upstroke the rest, both eased at the ends.
     */
    private static float stroke(float t) {
        if (t < DOWNSTROKE) {
            return -Mth.cos(Mth.PI * t / DOWNSTROKE);
        }
        return Mth.cos(Mth.PI * (t - DOWNSTROKE) / (1f - DOWNSTROKE));
    }

    /**
     * A bird looks around in sharp turns, then holds still: every
     * {@link #GLANCE_TICKS} it picks a new angle within ±range/2 and snaps to it
     * over three ticks.
     */
    private static float glance(int seed, float ageInTicks, float range) {
        float slot = (ageInTicks + Math.floorMod(seed, (int) GLANCE_TICKS)) / GLANCE_TICKS;
        int index = Mth.floor(slot);
        float into = (slot - index) * GLANCE_TICKS;
        float from = (random01(seed + index - 1) - 0.5f) * range;
        float to = (random01(seed + index) - 0.5f) * range;
        float k = Math.min(1f, into / 3f);
        k = k * k * (3f - 2f * k);
        return Mth.lerp(k, from, to);
    }

    /** A repeatable 0..1 value from an integer: the same input always gives the same glance. */
    private static float random01(int n) {
        n ^= n >>> 16;
        n *= 0x7feb352d;
        n ^= n >>> 15;
        n *= 0x846ca68b;
        n ^= n >>> 16;
        return (n & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float fraction(float x) {
        return x - Mth.floor(x);
    }

    /** Back to the modelled rest pose before each frame's animation is laid on top. */
    private void resetParts() {
        commeagle.resetPose();
        body.resetPose();
        neck.resetPose();
        head.resetPose();
        tailbase.resetPose();
        tailfeathers.resetPose();
        leftWingBase.resetPose();
        leftWingFeathers.resetPose();
        rightWingBase.resetPose();
        rightWingFeathers.resetPose();
        leftLeg.resetPose();
        leftFoot.resetPose();
        rightLeg.resetPose();
        rightFoot.resetPose();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        commeagle.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
