package net.backrooms.client;

import net.backrooms.Backrooms;
import net.backrooms.entity.StillLifeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StillLifeRenderer extends MobRenderer<StillLifeEntity, StillLifeModel> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "textures/entity/still_life.png");

	public StillLifeRenderer(EntityRendererProvider.Context context) {
		super(context, new StillLifeModel(context.bakeLayer(StillLifeModel.LAYER)), 0.4F);
	}

	@Override
	public ResourceLocation getTextureLocation(StillLifeEntity entity) {
		return TEXTURE;
	}
}
