package net.backrooms;

import net.backrooms.entity.BacteriaEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public final class ModEntities {
	public static final EntityType<BacteriaEntity> BACTERIA = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "bacteria"),
			EntityType.Builder.of(BacteriaEntity::new, MobCategory.MONSTER)
					.sized(0.75F, 2.45F)
					.clientTrackingRange(12)
					.updateInterval(3)
					.build("bacteria"));

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(BACTERIA, BacteriaEntity.createAttributes());
	}
}
