package net.backrooms;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

/**
 * Adds our content to the appropriate vanilla creative tabs. Kept separate so
 * future items/blocks can register themselves in one obvious place.
 */
public final class ModCreativeTabs {
	private ModCreativeTabs() {
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries ->
				entries.accept(ModItems.BACTERIA_SPAWN_EGG));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(ModBlocks.WALLPAPER);
			entries.accept(ModBlocks.WALLPAPER_DAMP);
			entries.accept(ModBlocks.CARPET);
			entries.accept(ModBlocks.CEILING_TILE);
			entries.accept(ModBlocks.FOUNDATION);
			entries.accept(ModBlocks.FLUORESCENT);
		});
	}
}
