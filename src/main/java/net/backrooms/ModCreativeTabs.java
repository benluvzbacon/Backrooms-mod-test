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
				entries.add(ModItems.BACTERIA_SPAWN_EGG));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.add(ModBlocks.WALLPAPER);
			entries.add(ModBlocks.WALLPAPER_DAMP);
			entries.add(ModBlocks.CARPET);
			entries.add(ModBlocks.CEILING_TILE);
			entries.add(ModBlocks.FOUNDATION);
			entries.add(ModBlocks.FLUORESCENT);
		});
	}
}
