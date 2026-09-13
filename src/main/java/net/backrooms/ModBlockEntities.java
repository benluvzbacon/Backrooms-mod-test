package net.backrooms;

import net.backrooms.worldgen.block.LightFlickerBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<LightFlickerBlockEntity> FLUORESCENT_LIGHT =
			Registry.register(
					BuiltInRegistries.BLOCK_ENTITY_TYPE,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "fluorescent_light"),
					BlockEntityType.Builder.of(LightFlickerBlockEntity::new, ModBlocks.FLUORESCENT).build(null));

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
