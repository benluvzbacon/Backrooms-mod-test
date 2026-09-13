package net.backrooms;

import net.backrooms.worldgen.BackroomsChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ModWorldgen {
	public static final ResourceLocation BACKROOMS_ID =
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms");

	public static final ResourceKey<Level> BACKROOMS_LEVEL =
			ResourceKey.create(Registries.DIMENSION, BACKROOMS_ID);

	private ModWorldgen() {
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, BACKROOMS_ID, BackroomsChunkGenerator.CODEC);
	}
}
