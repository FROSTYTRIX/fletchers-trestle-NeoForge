package net.frostytrix.fletcherstrestle.entity.client;

import net.frostytrix.fletcherstrestle.FletcherTrestle;
import net.frostytrix.fletcherstrestle.entity.custom.HeavyDummyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class HeavyDummyModel<T extends HeavyDummyEntity> extends HumanoidModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(FletcherTrestle.MOD_ID, "heavy_dummy"), "main");

	public HeavyDummyModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition partdefinition = meshdefinition.getRoot();

		// 1. HEAD (Remains at the neck pivot)
		partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16)
						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		// 2. BODY (Torso: 8 wide, 12 tall)
		partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(4, 0)
						.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		// 3. ARMS (The Shoulders)
		// Vanilla arms pivot 2 pixels DOWN and 5 pixels OUT from the neck.
		// We adjust the box to go UP 2 pixels to meet the neck line.
		partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0)
						.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				PartPose.offset(-5.0F, 2.0F, 0.0F));

		partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0)
						.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				PartPose.offset(5.0F, 2.0F, 0.0F));

		// 4. LEGS (The Base)
		// Pivot is 12 pixels down from the neck.
		partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 32)
						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				PartPose.offset(-1.9F, 12.0F, 0.0F));

		partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 32)
						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
				PartPose.offset(1.9F, 12.0F, 0.0F));

		partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// 1. DO NOT call super.setupAnim here.
		// That is what triggers the default walking/swinging logic.

		// 2. Head Tracking (Optional)
		// If you want the dummy's "head" block to still follow the player:
		this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);

		// 3. Force-reset all limb rotations to zero
		// This keeps your arms and legs perfectly aligned with the body block
		this.body.yRot = 0.0F;
		this.body.xRot = 0.0F;
		this.body.zRot = 0.0F;

		this.rightArm.xRot = 0.0F;
		this.rightArm.yRot = 0.0F;
		this.rightArm.zRot = 0.0F;

		this.leftArm.xRot = 0.0F;
		this.leftArm.yRot = 0.0F;
		this.leftArm.zRot = 0.0F;

		this.rightLeg.xRot = 0.0F;
		this.rightLeg.yRot = 0.0F;
		this.rightLeg.zRot = 0.0F;

		this.leftLeg.xRot = 0.0F;
		this.leftLeg.yRot = 0.0F;
		this.leftLeg.zRot = 0.0F;
	}

}