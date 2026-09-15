package net.backrooms.worldgen;

/**
 * Deterministic layout of the Poolrooms (Level !-pool).
 *
 * <p>A 24-block lattice of pale tile walls with wide archway gaps divides the
 * infinite space into cells. Each cell is either a shallow <b>pool</b> (a
 * one-block-deep basin of still water with a tiled deck around it) or a dry
 * <b>deck</b> dotted with lamp-topped pillars. The sky is open and a fast
 * day/night cycle runs overhead. Rare 3x3 portal pads lead back to Level 0.</p>
 *
 * <p>Like {@link Level0Layout}, every column is a pure function of (seed, x, z)
 * so chunk borders agree without any stored state.</p>
 */
public final class PoolroomsLayout {
	public static final int SPACING = 24;

	public static final int FLOOR_Y = 64;
	public static final int BASIN_FLOOR_Y = 63;
	public static final int WALL_TOP_Y = 71;
	public static final int PILLAR_TOP_Y = 69;

	public static final int EXIT_PAD_CHUNK_MOD = 16;
	public static final int PAD_RADIUS = 1;
	public static final long NO_POS = Long.MIN_VALUE;

	private static final float POOL_CELL_CHANCE = 0.45F;
	private static final int ARCH_WIDTH = 5;

	private final long seed;

	public PoolroomsLayout(long seed) {
		this.seed = seed;
	}

	// ---------------------------------------------------------------- hashes
	private static long mix(long z) {
		z += 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	private long hash(long a, long b, long c) {
		return mix(this.seed ^ mix(a * 31 + b * 37 + c * 131 + 0x85A308D313198A2EL));
	}

	private float rand01(long a, long b, long c) {
		return (hash(a, b, c) >>> 11) * 0x1.0p-53f;
	}

	private static int floorMod(int a, int n) {
		return Math.floorMod(a, n);
	}

	private static int floorDiv(int a, int n) {
		return Math.floorDiv(a, n);
	}

	// ------------------------------------------------------------- geometry
	public boolean isWallColumn(int x, int z) {
		int lx = floorMod(x, SPACING);
		int lz = floorMod(z, SPACING);
		boolean onX = lx == 0;
		boolean onZ = lz == 0;
		if (!onX && !onZ) {
			return false;
		}
		// Wall corners are always solid pillars.
		if (onX && onZ) {
			return true;
		}
		int cellX = floorDiv(x, SPACING);
		int cellZ = floorDiv(z, SPACING);
		if (onX) {
			// Segment runs along z inside cell (cellX, cellZ).
			if (lz <= 1 || lz >= SPACING - 2) {
				return true;
			}
			return !inArch(hash(90, cellX, cellZ), lz)
					&& !inArch(hash(91, cellX, cellZ), lz);
		}
		if (lx <= 1 || lx >= SPACING - 2) {
			return true;
		}
		return !inArch(hash(92, cellX, cellZ), lx)
				&& !inArch(hash(93, cellX, cellZ), lx);
	}

	/** Two 5-wide archways per wall segment, one per half. */
	private boolean inArch(long h, int local) {
		int c1 = 5 + (int) floorMod(h, 5);
		int c2 = 17 + (int) floorMod(h >>> 16, 4);
		return Math.abs(local - c1) <= ARCH_WIDTH / 2
				|| Math.abs(local - c2) <= ARCH_WIDTH / 2;
	}

	public boolean isPoolCell(int cellX, int cellZ) {
		return rand01(85, cellX, cellZ) < POOL_CELL_CHANCE;
	}

	/** Pool basins occupy the inside of pool cells, leaving a 3-block tiled deck. */
	public boolean isWaterColumn(int x, int z) {
		if (isWallColumn(x, z) || exitPadRoleAt(x, z) != 0) {
			return false;
		}
		int cellX = floorDiv(x, SPACING);
		int cellZ = floorDiv(z, SPACING);
		if (!isPoolCell(cellX, cellZ)) {
			return false;
		}
		int lx = floorMod(x, SPACING);
		int lz = floorMod(z, SPACING);
		return lx >= 3 && lx <= SPACING - 4 && lz >= 3 && lz <= SPACING - 4;
	}

	/** Dry deck pillars, only inside dry deck cells, lamp-capped at build time. */
	public boolean isPillarColumn(int x, int z) {
		if (isWallColumn(x, z) || exitPadRoleAt(x, z) != 0) {
			return false;
		}
		int cellX = floorDiv(x, SPACING);
		int cellZ = floorDiv(z, SPACING);
		if (isPoolCell(cellX, cellZ)) {
			return false;
		}
		int lx = floorMod(x, SPACING);
		int lz = floorMod(z, SPACING);
		int slotX = lx == 5 ? 0 : lx == 18 ? 1 : -1;
		int slotZ = lz == 5 ? 0 : lz == 18 ? 1 : -1;
		if (slotX < 0 || slotZ < 0) {
			return false;
		}
		int slot = slotX * 2 + slotZ;
		return floorMod(hash(86, cellX, cellZ) >>> (slot * 8), 3) != 0;
	}

	/** True when a wall column carries a lamp on top this height step. */
	public boolean wallLampAt(int x, int z) {
		return isWallColumn(x, z) && floorMod(x * 7 + z * 13, 8) == 0;
	}

	// ------------------------------------------------------------- exit pads
	private long exitPadCenter(int chunkX, int chunkZ) {
		if (Math.floorMod(hash(94, chunkX, chunkZ), EXIT_PAD_CHUNK_MOD) != 0) {
			return NO_POS;
		}
		int lx = 3 + (int) Math.floorMod(hash(95, chunkX, chunkZ), 10);
		int lz = 3 + (int) Math.floorMod(hash(96, chunkX, chunkZ), 10);
		int cx = chunkX * 16 + lx;
		int cz = chunkZ * 16 + lz;
		for (int dx = -PAD_RADIUS; dx <= PAD_RADIUS; dx++) {
			for (int dz = -PAD_RADIUS; dz <= PAD_RADIUS; dz++) {
				if (isWallColumn(cx + dx, cz + dz)) {
					return NO_POS;
				}
			}
		}
		return net.minecraft.core.BlockPos.asLong(cx, FLOOR_Y, cz);
	}

	/** 0 = not on a pad, 1 = lamp ring, 2 = centre (portal back to Level 0). */
	public int exitPadRoleAt(int x, int z) {
		int cx = floorDiv(x, 16);
		int cz = floorDiv(z, 16);
		for (int dcx = -1; dcx <= 1; dcx++) {
			for (int dcz = -1; dcz <= 1; dcz++) {
				long center = exitPadCenter(cx + dcx, cz + dcz);
				if (center == NO_POS) {
					continue;
				}
				net.minecraft.core.BlockPos cp = net.minecraft.core.BlockPos.of(center);
				int dx = x - cp.getX();
				int dz = z - cp.getZ();
				if (Math.abs(dx) <= PAD_RADIUS && Math.abs(dz) <= PAD_RADIUS) {
					return dx == 0 && dz == 0 ? 2 : 1;
				}
			}
		}
		return 0;
	}

	/** Dry, walkable floor column suitable for spawning/arrival (never a portal). */
	public boolean isDryFloor(int x, int z) {
		return !isWallColumn(x, z) && !isWaterColumn(x, z)
				&& !isPillarColumn(x, z) && exitPadRoleAt(x, z) == 0;
	}
}
