package net.backrooms.client;

import net.backrooms.entity.StillLifeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Gaunt mannequin model for the Still Life: oversized head with wide staring
 * eyes (painted on the texture), black beard slab, tricorn hat (wide brim with
 * three upturned flaps), yellow vest, teal sleeves.
 *
 * <p>Texture layout (128x128), kept in lock-step with tools/gen_textures.py:
 * head 0,0; body 0,16; arms u24/u38 v16; legs u0/u12 v40; beard 26,40;
 * hat crown 44,40; brim 44,64; shared brim-flap UV 44,92; coat skirts
 * front 0,62, back 16,62, sides (shared) 0,72.</p>
 *
 * <p>Like vanilla bipeds, parts are authored in model space where the renderer
 * lifts the root ~24px, so the soles of the feet sit at model-Y 24 and the
 * figure stands 45px (2.8 blocks) tall atop them.</p>
 */
public class StillLifeModel extends HierarchicalModel<StillLifeEntity> {
	public static final ResourceLocation LAYER_ID =
			ResourceLocation.fromNamespaceAndPath("backrooms", "still_life");
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(LAYER_ID, "main");

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leftArm;
	private final ModelPart rightArm;
	private final ModelPart leftLeg;
	private final ModelPart rightLeg;

	public StillLifeModel(ModelPart root) {
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

		// Yellow vest / tunic. Every part sits 8px higher than its raw box so the
		// feet land on model-Y 24 (see class javadoc).
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 16)
						.addBox(-3.0F, 0.0F, -2.0F, 6.0F, 18.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -8.0F, 0.0F));

		// Flared coat skirts around the waist, tilted slightly outward.
		body.addOrReplaceChild("skirt_front",
				CubeListBuilder.create().texOffs(0, 62)
						.addBox(-3.0F, 0.0F, -1.0F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 12.0F, -2.0F, -0.15F, 0.0F, 0.0F));
		body.addOrReplaceChild("skirt_back",
				CubeListBuilder.create().texOffs(16, 62)
						.addBox(-3.0F, 0.0F, 0.0F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 12.0F, 2.0F, 0.15F, 0.0F, 0.0F));
		body.addOrReplaceChild("skirt_left",
				CubeListBuilder.create().texOffs(0, 72)
						.addBox(-1.0F, 0.0F, -2.0F, 1.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.15F));
		body.addOrReplaceChild("skirt_right",
				CubeListBuilder.create().texOffs(0, 72)
						.addBox(0.0F, 0.0F, -2.0F, 1.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.15F));

		// Slightly oversized head so the stare reads down a hallway.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -8.0F, 0.0F));

		// Teal long sleeves.
		root.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(24, 16)
						.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-3.5F, -8.0F, 0.0F));
		root.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(38, 16)
						.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(3.5F, -8.0F, 0.0F));

		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(0, 40)
						.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.5F, 10.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(12, 40)
						.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.5F, 10.0F, 0.0F));

		// Big black beard wrapping the jaw and chin; the mouth hides inside it.
		head.addOrReplaceChild("beard",
				CubeListBuilder.create().texOffs(26, 40)
						.addBox(-3.5F, -5.0F, -5.0F, 7.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.ZERO);

		// Tricorn: crown, wide flat brim and three upturned flaps.
		head.addOrReplaceChild("hat_crown",
				CubeListBuilder.create().texOffs(44, 40)
						.addBox(-3.5F, -14.0F, -3.5F, 7.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)),
				PartPose.ZERO);
		head.addOrReplaceChild("hat_brim",
				CubeListBuilder.create().texOffs(44, 64)
						.addBox(-6.5F, -9.0F, -6.5F, 13.0F, 1.0F, 13.0F, new CubeDeformation(0.0F)),
				PartPose.ZERO);
		head.addOrReplaceChild("hat_flap_front",
				CubeListBuilder.create().texOffs(44, 92)
						.addBox(-5.5F, -10.0F, -7.0F, 11.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)),
				PartPose.rotation(0.9F, 0.0F, 0.0F));
		head.addOrReplaceChild("hat_flap_left",
				CubeListBuilder.create().texOffs(44, 92)
						.addBox(0.0F, -10.0F, -5.5F, 7.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)),
				PartPose.rotation(0.0F, 0.0F, 0.9F));
		head.addOrReplaceChild("hat_flap_right",
				CubeListBuilder.create().texOffs(44, 92)
						.addBox(-7.0F, -10.0F, -5.5F, 7.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)),
				PartPose.rotation(0.0F, 0.0F, -0.9F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(StillLifeEntity entity, float limbSwing, float limbSwingAmount,
			float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
		this.head.xRot = headPitch * Mth.DEG_TO_RAD;

		if (entity.isFrozenPose()) {
			// A shop-window mannequin caught mid-step: stiff, arms held a touch
			// away from the body, weight on one leg. Only the head turns.
			this.rightArm.zRot = -0.14F;
			this.leftArm.zRot = 0.14F;
			this.rightArm.xRot = -0.06F;
			this.leftArm.xRot = 0.05F;
			this.leftLeg.xRot = 0.06F;
			this.rightLeg.xRot = -0.02F;
			return;
		}

		float walk = Mth.cos(limbSwing * 0.6662F) * 0.9F * limbSwingAmount;
		this.leftLeg.xRot = walk;
		this.rightLeg.xRot = -walk;

		float armSwing = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.8F * limbSwingAmount;
		this.leftArm.xRot = armSwing;
		this.rightArm.xRot = -armSwing;
		this.leftArm.zRot = 0.05F;
		this.rightArm.zRot = -0.05F;

		// It sprints flat-out once it can move.
		this.body.xRot = 0.10F * Mth.clamp(limbSwingAmount, 0.0F, 1.0F);

		// Two-armed lunge when it connects.
		float lunge = Mth.sin(this.attackTime * (float) Math.PI);
		this.leftArm.xRot -= lunge * 1.7F;
		this.rightArm.xRot -= lunge * 1.7F;
	}

	@Override
	public ModelPart root() {
		return root;
	}
}
