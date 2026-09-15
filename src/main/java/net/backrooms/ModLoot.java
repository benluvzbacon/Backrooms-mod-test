package net.backrooms;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

/** Custom loot table keys. */
public final class ModLoot {
	/** Supply chests scattered through Level 0 (primarily Almond Water). */
	public static final ResourceKey<LootTable> SUPPLY_CHEST =
			ResourceKey.create(Registries.LOOT_TABLE,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "chests/supply"));

	private ModLoot() {
	}
}
