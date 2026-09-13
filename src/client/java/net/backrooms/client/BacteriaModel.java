package net.backrooms.client;

import net.backrooms.entity.BacteriaEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.TexturedModelData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Tall, emaciated, long-armed humanoid model for the Bacteria.
 * Texture layout (64x64): head 0,0; body 0,16; arms 28,16; legs 28,40.
 */
public class BacteriaModel extends HierarchicalModel<BacteriaEntity> {
	public static final ResourceLocation LAYER_ID =
			ResourceLocation.fromNamespaceAndPath("backrooms", "bacteria");
	public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
			new net.minecraft.client.model.geom.ModelLayerLocation(LAYER_ID, "main");

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public BacteriaModel(ModelPart root) {
		super(RenderType::entityCutoutNoCull);
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.leftArm = root.getChild("left_arm");
		this.rightArm = root.getChild("right_arm");
		this.leftLeg = root.getChild("left_leg");
		this.rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 16)
						.addBox(-3.0F, 0.0F, -2.0F, 6.0F, 18.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.ZERO);
		root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.ZERO);
		root.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(28, 16)
						.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-4.0F, 0.0F, 0.0F));
		root.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(36, 16)
						.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(4.0F, 0.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(28, 40)
						.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.5F, 18.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(36, 40)
						.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.5F, 18.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static TexturedModelData createBodyData() {
		return createBodyLayer().bakeRoot();
	}

	@Override
	public void setupAnim(BacteriaEntity entity, float limbSwing, float limbSwingAmount,
						float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
		this.head.xRot = headPitch * Mth.DEG_TO_RAD;

		float walk = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
		this.leftLeg.xRot = walk * 0.9F;
		this.rightLeg.xRot = -walk * 0.9F;

		// Long arms swing harder and hang slightly forward - spider-ish.
		float armSwing = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.9F * limbSwingAmount;
		this.leftArm.xRot = armSwing - 0.18F;
		this.rightArm.xRot = -armSwing - 0.18F;
		this.leftArm.zRot = 0.08F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
		this.rightArm.zRot = -0.08F - Mth.sin(ageInTicks * 0.05F) * 0.03F;

		// Hunched posture.
		this.body.xRot = 0.14F;
		this.head.xRot += 0.12F;

		// Lunge with both arms when attacking.
		float lunge = Mth.sin(entity.attackTime * (float) Math.PI);
		this.leftArm.xRot -= lunge * 1.6F;
		this.rightArm.xRot -= lunge * 1.6F;
	}

	@Override
	public ModelPart root() {
		return root;
	}
}
