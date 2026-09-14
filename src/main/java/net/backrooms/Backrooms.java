package net.backrooms;

import net.backrooms.mechanics.BacteriaSpawner;
import net.backrooms.mechanics.SandHelmetHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			BackroomsConfig.INSTANCE.load();
			LOGGER.info("[Backrooms] backrooms.selftest property = {}",
					System.getProperty("backrooms.selftest"));
		});
		// The headless self-test runs from the server tick loop (see below);
		// SERVER_STARTED fires before the loop, where blocking chunk loads deadlock.

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			SandHelmetHandler.tick(server);
			BacteriaSpawner.tick(server);
			// Fallback trigger for the headless self-test if SERVER_STARTED raced startup.
			if (Boolean.getBoolean("backrooms.selftest")
					&& !net.backrooms.selftest.SelfTest.DONE && server.getTickCount() > 40) {
				net.backrooms.selftest.SelfTest.run(server);
			}
		});

		UseItemCallback.EVENT.register(SandHelmetHandler::onUseItem);
		UseBlockCallback.EVENT.register(SandHelmetHandler::onUseBlock);

		LOGGER.info("[Backrooms] initialised - put sand on your head to no-clip in");
	}
}
