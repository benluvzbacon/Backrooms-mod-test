package net.backrooms.client;

import net.backrooms.ModEntities;
import net.backrooms.ModWorldgen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BackroomsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(BacteriaModel.LAYER, BacteriaModel::createBodyData);
		EntityRendererRegistry.register(ModEntities.BACTERIA, BacteriaRenderer::new);

		DimensionRenderingRegistry.registerDimensionEffects(
				ModWorldgen.BACKROOMS_ID, new BackroomsDimensionEffects());
		// The ceiling is the only sky you will ever see here.
		DimensionRenderingRegistry.registerSkyRenderer(ModWorldgen.BACKROOMS_LEVEL, context -> { });
		DimensionRenderingRegistry.registerCloudRenderer(ModWorldgen.BACKROOMS_LEVEL, context -> { });
		DimensionRenderingRegistry.registerWeatherRenderer(ModWorldgen.BACKROOMS_LEVEL, context -> { });
	}
}
