package net.backrooms;

import net.backrooms.entity.StillLifeEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public final class ModEntities {
	public static final EntityType<StillLifeEntity> STILL_LIFE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "still_life"),
			EntityType.Builder.of(StillLifeEntity::new, MobCategory.MONSTER)
					.sized(0.75F, 2.9F)
					.clientTrackingRange(12)
					.updateInterval(3)
					.build("still_life"));

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(STILL_LIFE, StillLifeEntity.createAttributes());
	}
}
