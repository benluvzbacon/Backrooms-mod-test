package net.backrooms;

import net.backrooms.worldgen.BackroomsChunkGenerator;
import net.backrooms.worldgen.PoolroomsChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class ModWorldgen {
	public static final ResourceLocation BACKROOMS_ID =
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms");

	public static final ResourceLocation POOLROOMS_ID =
			ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "poolrooms");

	public static final ResourceKey<DimensionType> BACKROOMS_TYPE_KEY =
			ResourceKey.create(Registries.DIMENSION_TYPE,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms"));

	public static final ResourceKey<DimensionType> POOLROOMS_TYPE_KEY =
			ResourceKey.create(Registries.DIMENSION_TYPE,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "poolrooms"));

	public static final ResourceKey<LevelStem> BACKROOMS_STEM_KEY =
			ResourceKey.create(Registries.LEVEL_STEM,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms"));

	public static final ResourceKey<LevelStem> POOLROOMS_STEM_KEY =
			ResourceKey.create(Registries.LEVEL_STEM,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "poolrooms"));

	public static final ResourceKey<net.minecraft.world.level.Level> BACKROOMS_LEVEL =
			ResourceKey.create(Registries.DIMENSION,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms"));

	public static final ResourceKey<net.minecraft.world.level.Level> POOLROOMS_LEVEL =
			ResourceKey.create(Registries.DIMENSION,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "poolrooms"));

	public static final ResourceKey<Biome> BACKROOMS_BIOME_KEY =
			ResourceKey.create(Registries.BIOME,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms"));

	public static final ResourceKey<Biome> POOLROOMS_BIOME_KEY =
			ResourceKey.create(Registries.BIOME,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "poolrooms"));

	public static final ResourceKey<NoiseGeneratorSettings> BACKROOMS_NOISE_KEY =
			ResourceKey.create(Registries.NOISE_SETTINGS,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "backrooms"));

	public static final ResourceKey<NoiseGeneratorSettings> POOLROOMS_NOISE_KEY =
			ResourceKey.create(Registries.NOISE_SETTINGS,
					ResourceLocation.fromNamespaceAndPath(Backrooms.MOD_ID, "poolrooms"));

	private ModWorldgen() {
	}

	/** Chunk generator codec, registered so worlds can reference it by id in datapack JSON. */
	public static void initialize() {
		net.minecraft.core.Registry.register(
				net.minecraft.core.registries.BuiltInRegistries.CHUNK_GENERATOR,
				BACKROOMS_ID, BackroomsChunkGenerator.CODEC);
		net.minecraft.core.Registry.register(
				net.minecraft.core.registries.BuiltInRegistries.CHUNK_GENERATOR,
				POOLROOMS_ID, PoolroomsChunkGenerator.CODEC);
	}

}
