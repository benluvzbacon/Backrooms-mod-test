package net.backrooms.worldgen;

/**
 * Deterministic procedural description of the Level-0 floor plan.
 *
 * <p>The whole level is the intersection of two families of wall lines on a
 * jittered lattice. Every third lattice line is a straight 3-block-wide
 * <b>corridor</b> (with open intersections); the strips between corridors are
 * subdivided by the remaining wall lines, each segment of which is solid,
 * door-punched or missing entirely. The result is an endless mix of long
 * halls, small rooms, occasional merged large rooms, dead ends and junctions
 * that never repeats on a chunk boundary - every block column is a pure
 * function of (seed, x, z), so adjacent chunks always agree.</p>
 *
 * <p>This class performs no world access and allocates nothing per column, so
 * chunk generation stays cheap.</p>
 */
public final class Level0Layout {
	/** Nominal distance between lattice lines. */
	public static final int SPACING = 24;
	/** Every Nth lattice line is a corridor. */
	public static final int HALL_EVERY = 3;
	/** Corridors are 2*HALF_BAND + 1 blocks wide. */
	public static final int HALL_HALF_BAND = 1;
	/** How far non-corridor lines may wander from their nominal position. */
	public static final int JITTER = 5;

	private static final int AXIS_X = 1;
	private static final int AXIS_Z = 2;

	// Vertical plan (shared with the chunk generator).
	public static final int FLOOR_Y = 64;
	public static final int WALL_HEIGHT = 4;
	public static final int CEILING_Y = FLOOR_Y + WALL_HEIGHT + 1; // 69

	// Fixture results.
	public static final int FIXTURE_NONE = 0;
	public static final int FIXTURE_LIT = 1;
	public static final int FIXTURE_FLICKER = 2;
	public static final int FIXTURE_DEAD = 3;

	private final long seed;

	public Level0Layout(long seed) {
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
		return mix(this.seed ^ mix(a * 31 + b * 37 + c * 131 + 0x243F6A8885A308D3L));
	}

	private float rand01(long a, long b, long c) {
		return (hash(a, b, c) >>> 11) * 0x1.0p-53f;
	}

	private static int floorMod(int a, int n) {
		int r = a % n;
		return r < 0 ? r + n : r;
	}

	private static int floorDiv(int a, int n) {
		int q = a / n;
		if (a < 0 && a % n != 0) {
			q--;
		}
		return q;
	}

	// ------------------------------------------------------------- geometry
	/** World coordinate of wall/corridor line {@code i} on the X axis. */
	public int lineX(int i) {
		if (floorMod(i, HALL_EVERY) == 0) {
			return i * SPACING; // perfectly straight corridors
		}
		return i * SPACING + (int) (rand01(AXIS_X, i, 0) * (JITTER * 2 + 1)) - JITTER;
	}

	/** World coordinate of wall/corridor line {@code k} on the Z axis. */
	public int lineZ(int k) {
		if (floorMod(k, HALL_EVERY) == 0) {
			return k * SPACING;
		}
		return k * SPACING + (int) (rand01(AXIS_Z, k, 0) * (JITTER * 2 + 1)) - JITTER;
	}

	private Integer lineIndexAt(int coord, boolean xAxis) {
		int q = floorDiv(coord, SPACING);
		for (int i = q - 1; i <= q + 1; i++) {
			int pos = xAxis ? lineX(i) : lineZ(i);
			if (pos == coord) {
				return i;
			}
		}
		return null;
	}

	/** Index of the cell (interval between consecutive lines) containing coord. */
	private int cellIndexAt(int coord, boolean xAxis) {
		int q = floorDiv(coord, SPACING);
		for (int c = q - 2; c <= q + 2; c++) {
			int lo = xAxis ? lineX(c) : lineZ(c);
			int hi = xAxis ? lineX(c + 1) : lineZ(c + 1);
			if (coord > lo && coord < hi) {
				return c;
			}
		}
		return q;
	}

	private boolean inHallBand(int coord, boolean xAxis) {
		int q = floorDiv(coord, SPACING);
		int step = HALL_EVERY * SPACING;
		int nearest = Math.round((float) coord / step) * step;
		// nearest multiple of (HALL_EVERY*SPACING) is a corridor line
		if (Math.abs(coord - nearest) > HALL_HALF_BAND) {
			return false;
		}
		// verify against the (un-jittered) hall line
		int idx = nearest / SPACING;
		int pos = xAxis ? lineX(idx) : lineZ(idx);
		return Math.abs(coord - pos) <= HALL_HALF_BAND && floorMod(idx, HALL_EVERY) == 0;
	}

	// ---------------------------------------------------------- wall segments
	private static final int SEG_MISSING = 0;
	private static final int SEG_DOOR = 1;
	private static final int SEG_SOLID = 2;

	/** Raw per-segment decision before connectivity enforcement. */
	private int rawSegmentStatus(int axisSalt, int wallIndex, int cellIndex) {
		float r = rand01(axisSalt, wallIndex, cellIndex);
		if (r < 0.30F) {
			return SEG_MISSING; // large merged rooms
		} else if (r < 0.78F) {
			return SEG_DOOR;
		}
		return SEG_SOLID;
	}

	/**
	 * A "middle" cell (the one cell between corridor bands that does not
	 * directly open onto a corridor on either side) is enclosed when all four
	 * of its boundary segments rolled SOLID. Such a cell deterministically
	 * chooses one side as an escape door, guaranteeing the whole floor plan is
	 * reachable.
	 */
	private boolean middleCellEnclosed(int cx, int cz) {
		return floorMod(cx, HALL_EVERY) == 1
				&& floorMod(cz, HALL_EVERY) == 1
				&& rawSegmentStatus(AXIS_X, cx, cz) == SEG_SOLID
				&& rawSegmentStatus(AXIS_X, cx + 1, cz) == SEG_SOLID
				&& rawSegmentStatus(AXIS_Z, cz, cx) == SEG_SOLID
				&& rawSegmentStatus(AXIS_Z, cz + 1, cx) == SEG_SOLID;
	}

	/** Escape side: 0=WEST, 1=EAST, 2=SOUTH, 3=NORTH. */
	private int escapeSide(int cx, int cz) {
		return (int) (rand01(9, cx, cz) * 4);
	}

	/** Status of an X-running-direction wall (line x=wallIndex) across z-cell k. */
	private int xWallSegmentStatus(int wallIndex, int k) {
		int raw = rawSegmentStatus(AXIS_X, wallIndex, k);
		if (raw != SEG_SOLID) {
			return raw;
		}
		// adjacent cells: (wallIndex-1, k) to the west, (wallIndex, k) to the east
		for (int cx = wallIndex - 1; cx <= wallIndex; cx++) {
			if (floorMod(cx, HALL_EVERY) == 1 && floorMod(k, HALL_EVERY) == 1 && middleCellEnclosed(cx, k)) {
				int side = escapeSide(cx, k);
				int roleForThisWall = (cx == wallIndex) ? 0 : 1; // 0=WEST,1=EAST
				if (side == roleForThisWall) {
					return SEG_DOOR;
				}
			}
		}
		return SEG_SOLID;
	}

	/** Status of a Z-running-direction wall (line z=wallIndex) across x-cell i. */
	private int zWallSegmentStatus(int wallIndex, int i) {
		int raw = rawSegmentStatus(AXIS_Z, wallIndex, i);
		if (raw != SEG_SOLID) {
			return raw;
		}
		for (int cz = wallIndex - 1; cz <= wallIndex; cz++) {
			if (floorMod(i, HALL_EVERY) == 1 && floorMod(cz, HALL_EVERY) == 1 && middleCellEnclosed(i, cz)) {
				int side = escapeSide(i, cz);
				int roleForThisWall = (cz == wallIndex) ? 2 : 3; // 2=SOUTH,3=NORTH
				if (side == roleForThisWall) {
					return SEG_DOOR;
				}
			}
		}
		return SEG_SOLID;
	}

	private boolean xWallBlocksColumn(int wallIndex, int z) {
		int k = cellIndexAt(z, true);
		int z0 = lineZ(k);
		int z1 = lineZ(k + 1);
		// keep 2-block returns/pillars where perpendicular walls meet
		if (z <= z0 + 1 || z >= z1 - 1) {
			return true;
		}
		int status = xWallSegmentStatus(wallIndex, k);
		if (status == SEG_MISSING) {
			return false;
		}
		if (status == SEG_SOLID) {
			return true;
		}
		// door: two-wide opening near the segment middle
		int mid = (z0 + z1) / 2 + (int) (rand01(11, wallIndex, k) * 5) - 2;
		return z != mid - 1 && z != mid;
	}

	private boolean zWallBlocksColumn(int wallIndex, int x) {
		int i = cellIndexAt(x, false);
		int x0 = lineX(i);
		int x1 = lineX(i + 1);
		if (x <= x0 + 1 || x >= x1 - 1) {
			return true;
		}
		int status = zWallSegmentStatus(wallIndex, i);
		if (status == SEG_MISSING) {
			return false;
		}
		if (status == SEG_SOLID) {
			return true;
		}
		int mid = (x0 + x1) / 2 + (int) (rand01(12, wallIndex, i) * 5) - 2;
		return x != mid - 1 && x != mid;
	}

	/**
	 * @return true if the column at (x,z) is a solid wallpaper wall column.
	 */
	public boolean isWallColumn(int x, int z) {
		// Corridor bands suppress every wall (giving open halls + junctions).
		if (inHallBand(x, true) || inHallBand(z, false)) {
			return false;
		}
		Integer xi = lineIndexAt(x, true);
		Integer ki = lineIndexAt(z, false);
		if (xi != null) {
			// hall lines themselves never carry walls
			if (floorMod(xi, HALL_EVERY) == 0) {
				return false;
			}
			if (xWallBlocksColumn(xi, z)) {
				return true;
			}
		}
		if (ki != null) {
			if (floorMod(ki, HALL_EVERY) == 0) {
				return false;
			}
			if (zWallBlocksColumn(ki, x)) {
				return true;
			}
		}
		return false;
	}

	public boolean isDampWall(int x, int z) {
		return rand01(3, x, z) < 0.15F;
	}

	// ------------------------------------------------------------- lighting
	/**
	 * Deterministic ceiling fixture plan.
	 * Corridors: a bright regular row of fixtures along the centre line.
	 * Rooms: a 6-spaced grid; some large patches are permanently dark.
	 */
	public int fixtureAt(int x, int z) {
		boolean hallX = inHallBand(x, true);
		boolean hallZ = inHallBand(z, false);

		if (hallX && !hallZ) {
			// centre line of an X corridor, fixtures every 4 blocks along Z
			int idx = Math.round((float) x / (HALL_EVERY * SPACING)) * HALL_EVERY;
			if (x == lineX(idx) && floorMod(z + 2, 4) == 0) {
				float r = rand01(7, x, z);
				if (r < 0.20F) {
					return FIXTURE_FLICKER;
				} else if (r < 0.26F) {
					return FIXTURE_DEAD;
				}
				return FIXTURE_LIT;
			}
			return FIXTURE_NONE;
		}
		if (hallZ) {
			int idx = Math.round((float) z / (HALL_EVERY * SPACING)) * HALL_EVERY;
			if (z == lineZ(idx) && floorMod(x + 2, 4) == 0) {
				float r = rand01(8, x, z);
				if (r < 0.20F) {
					return FIXTURE_FLICKER;
				} else if (r < 0.26F) {
					return FIXTURE_DEAD;
				}
				return FIXTURE_LIT;
			}
			return FIXTURE_NONE;
		}
		// rooms
		if (floorMod(x, 6) == 2 && floorMod(z, 6) == 2) {
			int rx = floorDiv(x, HALL_EVERY * SPACING);
			int rz = floorDiv(z, HALL_EVERY * SPACING);
			if (rand01(5, rx, rz) < 0.14F) {
				return FIXTURE_DEAD; // whole dark patch
			}
			float r = rand01(6, x, z);
			if (r < 0.18F) {
				return FIXTURE_FLICKER;
			} else if (r < 0.24F) {
				return FIXTURE_DEAD;
			}
			return FIXTURE_LIT;
		}
		return FIXTURE_NONE;
	}
}
