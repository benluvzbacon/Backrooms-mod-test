package net.backrooms.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.backrooms.ModLoot;
import net.backrooms.ModBlocks;
import net.backrooms.worldgen.block.FluorescentBlock;
import net.backrooms.worldgen.block.LightFlickerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.RandomizableContainer;
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
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
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
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource)
			).apply(instance, BackroomsChunkGenerator::new));

	/** Captured in {@link #createState} (the one hook handed the world seed). */
	private long levelSeed;

	public BackroomsChunkGenerator(BiomeSource biomeSource) {
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

	private Level0Layout layout() {
		return new Level0Layout(this.levelSeed);
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
		// Rare supply chests holding Almond Water; everything else is handled in
		// fillFromNoise (lights, rooms) or by the BacteriaSpawner.
		Level0Layout layout = layout();
		long packed = layout.supplyChestPos(chunk.getPos().x, chunk.getPos().z);
		if (packed != Level0Layout.NO_POS) {
			BlockPos chestPos = BlockPos.of(packed);
			level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
			RandomizableContainer.setBlockEntityLootTable(
					level, level.getRandom(), chestPos, ModLoot.SUPPLY_CHEST);
		}
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
		Level0Layout layout = layout();
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
		BlockState poolTile = ModBlocks.POOL_TILE.defaultBlockState();
		BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState portal = ModBlocks.POOL_PORTAL.defaultBlockState();

		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();

		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				int x = baseX + lx;
				int z = baseZ + lz;
				boolean wall = layout.isWallColumn(x, z);
				BlockState wallState = layout.isDampWall(x, z) ? wallDamp : wallNormal;
				int padRole = layout.poolPadRoleAt(x, z);

				for (int y = 62; y <= 70; y++) {
					BlockState state = columnState(layout, y, wall, wallState, carpet, foundation,
							ceiling, lightOn, lightOff, air, x, z, padRole,
							poolTile, seaLantern, portal);
					chunk.setBlockState(pos.set(lx, y, lz), state, false);
					ocean.update(lx, y, lz, state);
					surface.update(lx, y, lz, state);
				}

				// flickering lights own a block entity
				if (!wall && layout.fixtureAt(x, z) == Level0Layout.FIXTURE_FLICKER) {
					BlockEntity blockEntity = new LightFlickerBlockEntity(
							new BlockPos(x, Level0Layout.CEILING_Y, z), lightOn);
					chunk.setBlockEntity(blockEntity);
				}
			}
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@SuppressWarnings("checkstyle:ParameterNumber")
	private BlockState columnState(Level0Layout layout, int y, boolean wall, BlockState wallState,
							BlockState carpet, BlockState foundation, BlockState ceiling,
							BlockState lightOn, BlockState lightOff, BlockState air, int x, int z,
							int padRole, BlockState poolTile, BlockState seaLantern, BlockState portal) {
		if (y == 62 || y == 63 || y == 70) {
			return foundation;
		}
		if (y == Level0Layout.FLOOR_Y) {
			if (padRole == 1) {
				return seaLantern; // lamp ring of the Poolrooms entrance
			}
			if (padRole == 2) {
				return poolTile;
			}
			return carpet;
		}
		if (y == Level0Layout.CEILING_Y) {
			if (wall) {
				return ceiling; // never embed a fixture inside a wall
			}
			if (padRole == 2) {
				return lightOn; // always brightly lit above the portal
			}
			return switch (layout.fixtureAt(x, z)) {
				case Level0Layout.FIXTURE_LIT, Level0Layout.FIXTURE_FLICKER -> lightOn;
				case Level0Layout.FIXTURE_DEAD -> lightOff;
				default -> ceiling;
			};
		}
		// Interior band 65..68
		if (!wall && padRole == 2 && y == 65) {
			return portal;
		}
		return wall ? wallState : air;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
		return Level0Layout.FLOOR_Y + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
		Level0Layout layout = layout();
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
		BlockState poolTile = ModBlocks.POOL_TILE.defaultBlockState();
		BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState portal = ModBlocks.POOL_PORTAL.defaultBlockState();
		int padRole = layout.poolPadRoleAt(x, z);

		for (int i = 0; i < height; i++) {
			int y = minY + i;
			BlockState state;
			if (y == 62 || y == 63 || y == 70) {
				state = foundation;
			} else if (y == Level0Layout.FLOOR_Y) {
				state = padRole == 1 ? seaLantern : padRole == 2 ? poolTile : carpet;
			} else if (y == Level0Layout.CEILING_Y) {
				if (wall) {
					state = ceiling;
				} else if (padRole == 2) {
					state = lightOn;
				} else {
					state = switch (layout.fixtureAt(x, z)) {
						case Level0Layout.FIXTURE_LIT, Level0Layout.FIXTURE_FLICKER -> lightOn;
						case Level0Layout.FIXTURE_DEAD -> lightOff;
						default -> ceiling;
					};
				}
			} else if (y >= 65 && y <= 68) {
				state = !wall && padRole == 2 && y == 65
						? portal
						: wall ? wallState : air;
			} else {
				state = air;
			}
			states[i] = state;
		}
		return new NoiseColumn(minY, states);
	}
}
