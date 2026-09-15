package net.backrooms.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * The seed actually used by world generation for a level.
 *
 * <p>ChunkMap calls {@code ChunkGenerator.createState(..., level.getSeed())},
 * so the seed each generator captures is exactly {@link ServerLevel#getSeed()};
 * this helper returns that captured value (falling back to the level seed),
 * keeping spawn/arrival searches and self-test checks in agreement with the
 * blocks that were generated.</p>
 */
public final class LevelSeeds {
	private LevelSeeds() {
	}

	public static long of(ServerLevel level) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		if (generator instanceof BackroomsChunkGenerator backrooms) {
			return backrooms.getLayoutSeed();
		}
		if (generator instanceof PoolroomsChunkGenerator poolrooms) {
			return poolrooms.getLayoutSeed();
		}
		return level.getSeed();
	}
}
