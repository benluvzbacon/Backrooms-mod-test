package net.backrooms.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.backrooms.ModBlocks;
import net.backrooms.worldgen.block.FluorescentBlock;
import net.backrooms.worldgen.block.LightFlickerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates the Backrooms floor plan produced by {@link Level0Layout}.
 *
 * <p>The world is generated lazily per chunk, but every block column is a pure
 * function of (seed, x, z), so neighbouring chunks agree perfectly on walls,
 * doorways and lights. No structures, features or carvers run; decoration is
 * deliberately empty for fast, predictable generation.</p>
 */
public class BackroomsChunkGenerator extends ChunkGenerator {
	public static final MapCodec<BackroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource),
					Codec.LONG.lenientOptionalFieldOf("world_seed", 0L).forGetter(gen -> gen.seed)
			).apply(instance, BackroomsChunkGenerator::new));

	private final long seed;
	private final Level0Layout layout;

	public BackroomsChunkGenerator(BiomeSource biomeSource, long seed) {
		super(biomeSource);
		this.seed = seed;
		this.layout = new Level0Layout(seed);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public ChunkGenerator withSeed(long seed) {
		return new BackroomsChunkGenerator(this.biomeSource, seed);
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
	}

	@Override
	public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
							StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
		// No vanilla features - lights, rooms and Bacteria are handled ourselves.
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
		// Handled by the custom BacteriaSpawner.
	}

	@Override
	public int getGenDepth() {
		return 256;
	}

	@Override
	public int getMinY() {
		return 0;
	}

	@Override
	public int getSeaLevel() {
		return 0;
	}

	@Override
	public int getSpawnHeight(LevelHeightAccessor level) {
		return Level0Layout.FLOOR_Y + 1;
	}

	@Override
	public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
													StructureManager structures, ChunkAccess chunk) {
		Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState carpet = ModBlocks.CARPET.defaultBlockState();
		BlockState foundation = ModBlocks.FOUNDATION.defaultBlockState();
		BlockState ceiling = ModBlocks.CEILING_TILE.defaultBlockState();
		BlockState wallNormal = ModBlocks.WALLPAPER.defaultBlockState();
		BlockState wallDamp = ModBlocks.WALLPAPER_DAMP.defaultBlockState();
		BlockState lightOn = ModBlocks.FLUORESCENT.defaultBlockState();
		BlockState lightOff = lightOn.setValue(FluorescentBlock.LIT, false);

		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();

		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				int x = baseX + lx;
				int z = baseZ + lz;
				boolean wall = layout.isWallColumn(x, z);
				BlockState wallState = layout.isDampWall(x, z) ? wallDamp : wallNormal;

				for (int y = 62; y <= 70; y++) {
					BlockState state = columnState(y, wall, wallState, carpet, foundation, ceiling,
							lightOn, lightOff, air, x, z);
					chunk.setBlockState(pos.set(lx, y, lz), state, false);
					ocean.update(lx, y, lz, state);
					surface.update(lx, y, lz, state);
				}

				// flickering lights own a block entity
				if (!wall && layout.fixtureAt(x, z) == Level0Layout.FIXTURE_FLICKER) {
					BlockEntity be = new LightFlickerBlockEntity(
							new BlockPos(x, Level0Layout.CEILING_Y, z), lightOn);
					chunk.setBlockEntity(be);
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@SuppressWarnings("checkstyle:ParameterNumber")
	private BlockState columnState(int y, boolean wall, BlockState wallState, BlockState carpet,
								BlockState foundation, BlockState ceiling, BlockState lightOn,
								BlockState lightOff, BlockState air, int x, int z) {
		if (y == 62 || y == 63 || y == 70) {
			return foundation;
		}
		if (y == Level0Layout.FLOOR_Y) {
			return carpet;
		}
		if (y == Level0Layout.CEILING_Y) {
			if (wall) {
				return ceiling; // never embed a fixture inside a wall
			}
			return switch (layout.fixtureAt(x, z)) {
				case Level0Layout.FIXTURE_LIT, Level0Layout.FIXTURE_FLICKER -> lightOn;
				case Level0Layout.FIXTURE_DEAD -> lightOff;
				default -> ceiling;
			};
		}
		// Interior band 65..68
		return wall ? wallState : air;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
		return Level0Layout.FLOOR_Y + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
		int height = level.getHeight();
		int minY = level.getMinBuildHeight();
		BlockState[] states = new BlockState[height];
		BlockState air = Blocks.AIR.defaultBlockState();
		boolean wall = layout.isWallColumn(x, z);
		BlockState wallState = layout.isDampWall(x, z)
				? ModBlocks.WALLPAPER_DAMP.defaultBlockState()
				: ModBlocks.WALLPAPER.defaultBlockState();
		BlockState carpet = ModBlocks.CARPET.defaultBlockState();
		BlockState foundation = ModBlocks.FOUNDATION.defaultBlockState();
		BlockState ceiling = ModBlocks.CEILING_TILE.defaultBlockState();
		BlockState lightOn = ModBlocks.FLUORESCENT.defaultBlockState();
		BlockState lightOff = lightOn.setValue(FluorescentBlock.LIT, false);

		for (int i = 0; i < height; i++) {
			int y = minY + i;
			BlockState state;
			if (y == 62 || y == 63 || y == 70) {
				state = foundation;
			} else if (y == Level0Layout.FLOOR_Y) {
				state = carpet;
			} else if (y == Level0Layout.CEILING_Y) {
				if (wall) {
					state = ceiling;
				} else {
					state = switch (layout.fixtureAt(x, z)) {
						case Level0Layout.FIXTURE_LIT, Level0Layout.FIXTURE_FLICKER -> lightOn;
						case Level0Layout.FIXTURE_DEAD -> lightOff;
						default -> ceiling;
					};
				}
			} else if (y >= 65 && y <= 68) {
				state = wall ? wallState : air;
			} else {
				state = air;
			}
			states[i] = state;
		}
		return new NoiseColumn(minY, states);
	}
}
