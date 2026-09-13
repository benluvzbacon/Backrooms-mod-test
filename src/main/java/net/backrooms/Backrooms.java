package net.backrooms;

import net.backrooms.mechanics.BacteriaSpawner;
import net.backrooms.mechanics.SandHelmetHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Backrooms implements ModInitializer {
	public static final String MOD_ID = "backrooms";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModSounds.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		ModCreativeTabs.initialize();
		ModWorldgen.initialize();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> BackroomsConfig.INSTANCE.load());
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (Boolean.getBoolean("backrooms.selftest")) {
				net.backrooms.selftest.SelfTest.run(server);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			SandHelmetHandler.tick(server);
			BacteriaSpawner.tick(server);
		});

		UseItemCallback.EVENT.register(SandHelmetHandler::onUseItem);

		LOGGER.info("[Backrooms] initialised - put sand on your head to no-clip in");
	}
}
