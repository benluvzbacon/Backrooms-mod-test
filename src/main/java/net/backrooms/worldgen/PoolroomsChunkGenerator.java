package net.backrooms.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.backrooms.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chunk generator for the Poolrooms. Roofed maze rooms with shallow pools and
 * rare tall, windowed great halls under a glass roof; see {@link PoolroomsLayout}.
 */
public class PoolroomsChunkGenerator extends ChunkGenerator {
	public static final MapCodec<PoolroomsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
			).apply(instance, PoolroomsChunkGenerator::new));

	private static final int BOTTOM_Y = 58;
	private static final int TOP_Y = 87;

	private long levelSeed;

	public PoolroomsChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public ChunkGeneratorStructureState createState(
			net.minecraft.core.HolderLookup<net.minecraft.world.level.levelgen.structure.StructureSet> structureSets,
			RandomState random, long seed) {
		this.levelSeed = seed;
		return super.createState(structureSets, random, seed);
	}

	private PoolroomsLayout layout() {
		return new PoolroomsLayout(this.levelSeed);
	}

	/** The worldgen seed captured in {@link #createState} (self-test/debug). */
	public long getLayoutSeed() {
		return this.levelSeed;
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
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
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
		return PoolroomsLayout.FLOOR_Y + 1;
	}

	@Override
	public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
			StructureManager structures, ChunkAccess chunk) {
		PoolroomsLayout layout = layout();
		Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();

		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				int x = baseX + lx;
				int z = baseZ + lz;
				boolean wall = layout.isWallColumn(x, z);
				// Tall status is read off the border itself: arch openings report
				// isWallColumn()==false, but the wall plane (and its window band
				// and cap) still has to be built there.
				boolean tallWall = layout.isTallWallColumn(x, z);
				boolean water = layout.isWaterColumn(x, z);
				boolean pillar = layout.isPillarColumn(x, z);
				boolean hall = layout.isInGreatHall(x, z);
				int padRole = layout.exitPadRoleAt(x, z);

				for (int y = BOTTOM_Y; y <= TOP_Y; y++) {
					BlockState state = columnState(layout, y, wall, tallWall, water, pillar, hall,
							padRole, x, z);
					chunk.setBlockState(pos.set(lx, y, lz), state, false);
					ocean.update(lx, y, lz, state);
					surface.update(lx, y, lz, state);
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@SuppressWarnings("checkstyle:ParameterNumber")
	private BlockState columnState(PoolroomsLayout layout, int y, boolean wall, boolean tallWall,
			boolean water, boolean pillar, boolean hall, int padRole, int x, int z) {
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState tile = ModBlocks.POOL_TILE.defaultBlockState();
		BlockState foundation = ModBlocks.FOUNDATION.defaultBlockState();
		BlockState lamp = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState glass = Blocks.GLASS.defaultBlockState();
		BlockState portal = ModBlocks.POOL_PORTAL.defaultBlockState();

		if (y < 62) {
			return foundation;
		}
		if (y == 62) {
			// Sunken basin floor: a glowing ring around the drain.
			if (padRole == 1) {
				return lamp;
			}
			if (padRole == 2) {
				return portal;
			}
			return foundation;
		}
		if (y == PoolroomsLayout.BASIN_FLOOR_Y) {
			if (padRole != 0) {
				return Blocks.WATER.defaultBlockState();
			}
			if (wall || water) {
				return tile;
			}
			return foundation;
		}
		if (y == PoolroomsLayout.FLOOR_Y) {
			if (padRole != 0) {
				// Open water with its surface flush with the surrounding floor.
				return Blocks.WATER.defaultBlockState();
			}
			if (water) {
				return Blocks.WATER.defaultBlockState();
			}
			return tile; // walls, pillars, dry deck
		}

		boolean perimeterOpening = layout.isPerimeterOpening(x, z);

		// Great hall perimeter: tall walls with a clerestory window band.
		if (tallWall) {
			if (y <= PoolroomsLayout.HALL_ARCH_TOP_Y) {
				return perimeterOpening ? air : tile;
			}
			if (y == PoolroomsLayout.HALL_LINTEL_Y) {
				return tile;
			}
			if (y >= PoolroomsLayout.HALL_WINDOW_MIN_Y && y <= PoolroomsLayout.HALL_WINDOW_MAX_Y) {
				return glass;
			}
			if (y <= PoolroomsLayout.HALL_WALL_TOP_Y) {
				return tile;
			}
			return tile; // wall cap under the roof border
		}

		if (hall) {
			// Open great-hall interior, apart from tall pillars and their lamp caps.
			if (y == PoolroomsLayout.HALL_GLASS_ROOF_Y) {
				// Solid cap where a pillar meets the roof; glass everywhere else.
				return pillar ? tile : glass;
			}
			if (pillar && y < PoolroomsLayout.HALL_PILLAR_LAMP_Y) {
				return tile;
			}
			if (pillar && y == PoolroomsLayout.HALL_PILLAR_LAMP_Y) {
				return lamp;
			}
			if (layout.hallLanternAt(x, z) && y == PoolroomsLayout.HALL_PILLAR_LAMP_Y) {
				return lamp;
			}
			return air;
		}

		// Roofed maze rooms.
		if (wall || (pillar && y <= 69)) {
			return tile;
		}
		if (y <= 69) {
			return air;
		}
		if (y == PoolroomsLayout.CEILING_Y) {
			if (padRole == 2) {
				return lamp;
			}
			if (layout.ceilingLampAt(x, z)) {
				return lamp;
			}
			return tile;
		}
		if (y == PoolroomsLayout.ROOF_Y) {
			return foundation;
		}
		return air;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
		return PoolroomsLayout.FLOOR_Y + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
		PoolroomsLayout layout = layout();
		int height = level.getHeight();
		int minY = level.getMinBuildHeight();
		BlockState[] states = new BlockState[height];
		boolean wall = layout.isWallColumn(x, z);
		boolean tallWall = layout.isTallWallColumn(x, z);
		boolean water = layout.isWaterColumn(x, z);
		boolean pillar = layout.isPillarColumn(x, z);
		boolean hall = layout.isInGreatHall(x, z);
		int padRole = layout.exitPadRoleAt(x, z);
		for (int i = 0; i < height; i++) {
			int y = minY + i;
			BlockState state;
			if (y < BOTTOM_Y || y > TOP_Y) {
				state = Blocks.AIR.defaultBlockState();
			} else {
				state = columnState(layout, y, wall, tallWall, water, pillar, hall, padRole, x, z);
			}
			states[i] = state;
		}
		return new NoiseColumn(minY, states);
	}
}
