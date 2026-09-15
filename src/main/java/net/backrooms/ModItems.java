package net.backrooms;

import net.backrooms.item.AlmondWaterItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	public static final Item STILL_LIFE_SPAWN_EGG = register(
			"still_life_spawn_egg",
			new SpawnEggItem(ModEntities.STILL_LIFE, 0x3E2C1C, 0x87B0A6, new Item.Properties()));

	// Bottles do not stack, like vanilla potions (one glass bottle returned per drink).
	public static final Item ALMOND_WATER = register(
			"almond_water",
			new AlmondWaterItem(new Item.Properties().stacksTo(1)));

	private ModItems() {
	}

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM,
				ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, name), item);
	}

	public static void initialize() {
	}
}
