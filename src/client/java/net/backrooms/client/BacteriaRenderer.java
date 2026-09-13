package net.backrooms.client;

import net.backrooms.Backrooms;
import net.backrooms.entity.BacteriaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BacteriaRenderer extends MobRenderer<BacteriaEntity, BacteriaModel> {
	private static final ResourceLocation TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "textures/entity/bacteria.png");

	public BacteriaRenderer(EntityRendererProvider.Context context) {
		super(context, new BacteriaModel(context.bakeLayer(BacteriaModel.LAYER)), 0.45F);
	}

	@Override
	public ResourceLocation getTextureLocation(BacteriaEntity entity) {
		return TEXTURE;
	}
}
