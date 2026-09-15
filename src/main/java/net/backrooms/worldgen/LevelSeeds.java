package net.backrooms.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * The seed actually used by world generation for a level.
 *
 * <p>A chunk generator's {@code createState} captures the random-state seed,
 * which can differ from {@link ServerLevel#getSeed()} (the biome-obfuscated
 * value). Every layout built outside the generator — spawn/arrival searches and
 * self-test checks — must use the exact seed the generator captured, so its
 * pure coordinate function agrees with the blocks that were generated.</p>
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
		return level.getChunkSource().randomState().seed();
	}
}
